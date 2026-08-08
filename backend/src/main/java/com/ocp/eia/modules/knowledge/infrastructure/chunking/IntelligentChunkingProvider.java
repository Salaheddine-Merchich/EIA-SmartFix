package com.ocp.eia.modules.knowledge.infrastructure.chunking;

import com.ocp.eia.modules.knowledge.domain.port.ChunkingProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@Slf4j
public class IntelligentChunkingProvider implements ChunkingProviderPort {
    
    private static final int MAX_CHUNK_SIZE = 500;  // Taille max recommandée pour embeddings
    private static final int MIN_CHUNK_SIZE = 50;   // Taille min pour éviter les fragments insignifiants
    private static final int OVERLAP_SIZE = 50;     // Chevauchement entre chunks pour préserver le contexte
    
    // Patterns pour identifier les sections d'intervention
    private static final Pattern SYMPTOMES_PATTERN = Pattern.compile(
        "(?i)\\b(symptômes?|symptome?s?|problème?s?|panne?s?|défaut?s?|observation?s?)\\s*:?", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern CAUSE_PATTERN = Pattern.compile(
        "(?i)\\b(causes?|origine?s?|racine?s?|diagnostic?s?|analyse?s?)\\s*:?", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "(?i)\\b(actions?|solution?s?|réparation?s?|correction?s?|intervention?s?)\\s*:?", 
        Pattern.CASE_INSENSITIVE);
    
    // Patterns pour documents de connaissance
    private static final Pattern SECTION_PATTERN = Pattern.compile(
        "(?m)^\\d+\\.?\\s+[A-Z][^\\n]{10,80}$|^[A-Z][^\\n]{5,80}\\s*:?\\s*$", 
        Pattern.MULTILINE);
    private static final Pattern PROCEDURE_PATTERN = Pattern.compile(
        "(?i)\\b(procédure?s?|étapes?|instructions?|méthode?s?)\\s*:?", 
        Pattern.CASE_INSENSITIVE);

    @Override
    public List<TextChunk> chunkContent(String content, ContentType contentType) {
        if (content == null || content.trim().length() < MIN_CHUNK_SIZE) {
            log.debug("Contenu trop court pour chunking: {} caractères", 
                     content != null ? content.length() : 0);
            return content != null ? List.of(TextChunk.simple(content.trim(), 0)) : List.of();
        }
        
        return switch (contentType) {
            case INTERVENTION -> chunkIntervention(content);
            case KNOWLEDGE_DOCUMENT -> chunkKnowledgeDocument(content);
            case GENERIC -> chunkGeneric(content);
        };
    }
    
    /**
     * Chunking spécialisé pour les interventions techniques
     */
    private List<TextChunk> chunkIntervention(String content) {
        List<TextChunk> chunks = new ArrayList<>();
        
        // Tentative de découpage sémantique par sections
        List<SemanticSection> sections = extractInterventionSections(content);
        
        if (sections.size() > 1) {
            // Plusieurs sections identifiées : chunk par section
            for (int i = 0; i < sections.size(); i++) {
                SemanticSection section = sections.get(i);
                if (section.content.length() > MAX_CHUNK_SIZE) {
                    // Section trop grande : subdiviser
                    List<TextChunk> subChunks = chunkBySentences(section.content, section.type);
                    for (int j = 0; j < subChunks.size(); j++) {
                        TextChunk subChunk = subChunks.get(j);
                        chunks.add(new TextChunk(
                            subChunk.content(),
                            chunks.size(),
                            section.startOffset + subChunk.startOffset(),
                            section.startOffset + subChunk.endOffset(),
                            section.type
                        ));
                    }
                } else {
                    chunks.add(new TextChunk(
                        section.content, i, section.startOffset, section.endOffset, section.type
                    ));
                }
            }
        } else {
            // Pas de sections identifiées : chunking générique
            chunks.addAll(chunkGeneric(content));
        }
        
        log.debug("Chunking intervention: {} caractères → {} chunks", content.length(), chunks.size());
        return chunks;
    }
    
    /**
     * Chunking spécialisé pour les documents de connaissance
     */
    private List<TextChunk> chunkKnowledgeDocument(String content) {
        List<TextChunk> chunks = new ArrayList<>();
        
        // Tentative de découpage par sections numérotées ou titres
        String[] lines = content.split("\\n");
        StringBuilder currentChunk = new StringBuilder();
        String currentType = "content";
        int chunkIndex = 0;
        int currentStartOffset = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) {
                currentChunk.append("\n");
                continue;
            }
            
            // Détecter les titres de section
            if (SECTION_PATTERN.matcher(line).matches() || 
                PROCEDURE_PATTERN.matcher(line).find()) {
                
                // Finaliser le chunk précédent
                if (currentChunk.length() > MIN_CHUNK_SIZE) {
                    chunks.add(new TextChunk(
                        currentChunk.toString().trim(),
                        chunkIndex++,
                        currentStartOffset,
                        currentStartOffset + currentChunk.length(),
                        currentType
                    ));
                    currentStartOffset += currentChunk.length();
                }
                
                // Commencer un nouveau chunk
                currentChunk = new StringBuilder(line + "\n");
                currentType = identifyDocumentSectionType(line);
            } else {
                currentChunk.append(line).append("\n");
                
                // Si le chunk devient trop grand, le finaliser
                if (currentChunk.length() > MAX_CHUNK_SIZE) {
                    chunks.add(new TextChunk(
                        currentChunk.toString().trim(),
                        chunkIndex++,
                        currentStartOffset,
                        currentStartOffset + currentChunk.length(),
                        currentType
                    ));
                    currentStartOffset += currentChunk.length();
                    currentChunk = new StringBuilder();
                }
            }
        }
        
        // Finaliser le dernier chunk
        if (currentChunk.length() > MIN_CHUNK_SIZE) {
            chunks.add(new TextChunk(
                currentChunk.toString().trim(),
                chunkIndex,
                currentStartOffset,
                currentStartOffset + currentChunk.length(),
                currentType
            ));
        }
        
        // Si aucun chunk valide n'a été créé, faire un chunking générique
        if (chunks.isEmpty()) {
            chunks.addAll(chunkGeneric(content));
        }
        
        log.debug("Chunking document: {} caractères → {} chunks", content.length(), chunks.size());
        return chunks;
    }
    
    /**
     * Chunking générique par phrases avec chevauchement
     */
    private List<TextChunk> chunkGeneric(String content) {
        return chunkBySentences(content, "generic");
    }
    
    private List<TextChunk> chunkBySentences(String content, String type) {
        List<TextChunk> chunks = new ArrayList<>();
        
        // Découpage par phrases
        String[] sentences = content.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int currentOffset = 0;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > MAX_CHUNK_SIZE && currentChunk.length() > MIN_CHUNK_SIZE) {
                // Finaliser le chunk actuel
                String chunkContent = currentChunk.toString().trim();
                chunks.add(new TextChunk(
                    chunkContent,
                    chunkIndex++,
                    currentOffset,
                    currentOffset + chunkContent.length(),
                    type
                ));
                
                // Commencer le nouveau chunk avec chevauchement
                String overlap = getLastSentences(chunkContent, OVERLAP_SIZE);
                currentChunk = new StringBuilder(overlap.isEmpty() ? sentence : overlap + " " + sentence);
                currentOffset += chunkContent.length() - overlap.length();
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            }
        }
        
        // Finaliser le dernier chunk
        if (currentChunk.length() > MIN_CHUNK_SIZE) {
            String chunkContent = currentChunk.toString().trim();
            chunks.add(new TextChunk(
                chunkContent,
                chunkIndex,
                currentOffset,
                currentOffset + chunkContent.length(),
                type
            ));
        }
        
        return chunks;
    }
    
    private List<SemanticSection> extractInterventionSections(String content) {
        List<SemanticSection> sections = new ArrayList<>();
        
        String[] lines = content.split("\\n");
        StringBuilder currentSection = new StringBuilder();
        String currentType = "general";
        int sectionStartOffset = 0;
        int currentOffset = 0;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Identifier le type de section
            String detectedType = null;
            if (SYMPTOMES_PATTERN.matcher(trimmedLine).find()) {
                detectedType = "symptomes";
            } else if (CAUSE_PATTERN.matcher(trimmedLine).find()) {
                detectedType = "cause";
            } else if (ACTION_PATTERN.matcher(trimmedLine).find()) {
                detectedType = "action";
            }
            
            if (detectedType != null && !detectedType.equals(currentType)) {
                // Finaliser la section précédente
                if (currentSection.length() > 0) {
                    sections.add(new SemanticSection(
                        currentSection.toString().trim(), currentType, sectionStartOffset, currentOffset
                    ));
                }
                
                // Commencer nouvelle section
                currentSection = new StringBuilder(line + "\n");
                currentType = detectedType;
                sectionStartOffset = currentOffset;
            } else {
                currentSection.append(line).append("\n");
            }
            
            currentOffset += line.length() + 1;
        }
        
        // Finaliser la dernière section
        if (currentSection.length() > 0) {
            sections.add(new SemanticSection(
                currentSection.toString().trim(), currentType, sectionStartOffset, currentOffset
            ));
        }
        
        return sections;
    }
    
    private String identifyDocumentSectionType(String line) {
        if (PROCEDURE_PATTERN.matcher(line).find()) {
            return "procedure";
        } else if (line.matches("(?i).*diagnostic.*|.*dépannage.*|.*troubleshooting.*")) {
            return "diagnostic";
        } else if (line.matches("(?i).*sécurité.*|.*warning.*|.*attention.*")) {
            return "securite";
        } else if (line.matches("\\d+\\..*")) {
            return "etape";
        } else {
            return "section";
        }
    }
    
    private String getLastSentences(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder overlap = new StringBuilder();
        
        for (int i = sentences.length - 1; i >= 0; i--) {
            if (overlap.length() + sentences[i].length() <= maxLength) {
                if (overlap.length() > 0) {
                    overlap.insert(0, " ");
                }
                overlap.insert(0, sentences[i]);
            } else {
                break;
            }
        }
        
        return overlap.toString();
    }
    
    private record SemanticSection(String content, String type, int startOffset, int endOffset) {}
}