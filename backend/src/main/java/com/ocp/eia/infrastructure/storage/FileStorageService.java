package com.ocp.eia.infrastructure.storage;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "docx");
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private final AppProperties appProperties;

    public StoredFile store(UUID interventionId, MultipartFile file) {
        validate(file);
        try {
            Path root = storageRoot();
            Path dir = root.resolve(interventionId.toString()).normalize();
            ensureUnderRoot(dir);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            Path target = dir.resolve(storedName).normalize();
            ensureUnderRoot(target);
            String detectedType = detectContentType(file);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(storedName, target.toString(), detectedType, file.getSize());
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            log.error("File storage failed for intervention {}: {}", interventionId, e.toString());
            throw new BadRequestException("Erreur lors du stockage du fichier");
        }
    }

    public Resource loadAsResource(String storagePath) {
        try {
            Path path = resolveSafePath(storagePath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("Fichier introuvable");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BadRequestException("Chemin de fichier invalide");
        }
    }

    public void delete(String storagePath) {
        try {
            Path path = resolveSafePath(storagePath);
            Files.deleteIfExists(path);
        } catch (BadRequestException e) {
            log.warn("Refused delete outside storage root: {}", e.getMessage());
        } catch (IOException ignored) {
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("Le fichier dépasse la taille maximale de 10 Mo");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Type de fichier non autorisé. Formats acceptés: PDF, PNG, JPG, DOCX");
        }
        String detected = detectContentType(file);
        if (!ALLOWED_TYPES.contains(detected.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Type de fichier non autorisé. Formats acceptés: PDF, PNG, JPG, DOCX");
        }
        if (!extensionMatchesType(extension, detected)) {
            throw new BadRequestException("Type de fichier non autorisé. Formats acceptés: PDF, PNG, JPG, DOCX");
        }
    }

    private String detectContentType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(8);
            String magic = sniffMagic(header);
            if (magic != null) {
                return magic;
            }
        } catch (IOException e) {
            log.warn("Unable to sniff file content type");
        }
        String declared = file.getContentType();
        if (declared != null && ALLOWED_TYPES.contains(declared.toLowerCase(Locale.ROOT))) {
            return declared.toLowerCase(Locale.ROOT);
        }
        throw new BadRequestException("Type de fichier non autorisé. Formats acceptés: PDF, PNG, JPG, DOCX");
    }

    private static String sniffMagic(byte[] header) {
        if (header.length >= 4
                && header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
            return "application/pdf";
        }
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return "image/png";
        }
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // ZIP container (DOCX)
        if (header.length >= 4
                && header[0] == 0x50 && header[1] == 0x4B && (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07)
                && (header[3] == 0x04 || header[3] == 0x06 || header[3] == 0x08)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return null;
    }

    private static boolean extensionMatchesType(String extension, String contentType) {
        return switch (extension) {
            case "pdf" -> contentType.equals("application/pdf");
            case "png" -> contentType.equals("image/png");
            case "jpg", "jpeg" -> contentType.equals("image/jpeg") || contentType.equals("image/jpg");
            case "docx" -> contentType.contains("wordprocessingml") || contentType.equals("application/zip");
            default -> false;
        };
    }

    private Path storageRoot() throws IOException {
        Path root = Paths.get(appProperties.getStorage().getPath()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    private Path resolveSafePath(String storagePath) {
        try {
            Path root = storageRoot();
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            ensureUnderRoot(path);
            return path;
        } catch (IOException e) {
            throw new BadRequestException("Chemin de fichier invalide");
        }
    }

    private void ensureUnderRoot(Path path) throws IOException {
        Path root = storageRoot();
        if (!path.startsWith(root)) {
            throw new BadRequestException("Chemin de fichier invalide");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        String base = filename.replace("\\", "/");
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        String cleaned = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "document" : cleaned;
    }

    public static String contentDispositionFilename(String filename) {
        String safe = sanitizeFilename(filename).replace("\"", "");
        return safe;
    }

    public record StoredFile(String storedName, String storagePath, String contentType, long size) {}
}
