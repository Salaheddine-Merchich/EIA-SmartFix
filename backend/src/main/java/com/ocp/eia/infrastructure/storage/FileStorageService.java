package com.ocp.eia.infrastructure.storage;



import com.ocp.eia.config.AppProperties;

import com.ocp.eia.shared.exception.BadRequestException;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;

import org.springframework.core.io.UrlResource;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;

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

public class FileStorageService {



    private static final Set<String> ALLOWED_TYPES = Set.of(

            "application/pdf",

            "image/png",

            "image/jpeg",

            "image/jpg",

            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "docx");

    private static final Set<String> FALLBACK_MIME_TYPES = Set.of(

            "application/octet-stream",

            "application/zip"

    );

    private static final long MAX_SIZE = 10 * 1024 * 1024;



    private final AppProperties appProperties;



    public StoredFile store(UUID interventionId, MultipartFile file) {

        validate(file);

        try {

            Path dir = Paths.get(appProperties.getStorage().getPath(), interventionId.toString());

            Files.createDirectories(dir);

            String storedName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());

            Path target = dir.resolve(storedName);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFile(storedName, target.toString(), file.getContentType(), file.getSize());

        } catch (IOException e) {

            throw new BadRequestException("Erreur lors du stockage du fichier: " + e.getMessage());

        }

    }



    public Resource loadAsResource(String storagePath) {

        try {

            Path path = Paths.get(storagePath);

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

            Files.deleteIfExists(Paths.get(storagePath));

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

        if (!isAllowedFile(file)) {

            throw new BadRequestException("Type de fichier non autorisé. Formats acceptés: PDF, PNG, JPG, DOCX");

        }

    }



    private boolean isAllowedFile(MultipartFile file) {

        String contentType = file.getContentType();

        if (contentType != null && ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {

            return true;

        }



        String extension = extractExtension(file.getOriginalFilename());

        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {

            return false;

        }



        if (contentType == null) {

            return true;

        }



        return FALLBACK_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT));

    }



    private String extractExtension(String filename) {

        if (filename == null || !filename.contains(".")) {

            return null;

        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

    }



    private String sanitizeFilename(String filename) {

        if (filename == null) return "document";

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");

    }



    public record StoredFile(String storedName, String storagePath, String contentType, long size) {}

}


