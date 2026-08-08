package com.ocp.eia.infrastructure.storage;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.presentation.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getStorage().setPath(tempDir.toString());
        fileStorageService = new FileStorageService(appProperties);
    }

    @Test
    void store_docxWithZipMimeType_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport.docx",
                "application/zip",
                "docx-content".getBytes()
        );

        FileStorageService.StoredFile stored = fileStorageService.store(UUID.randomUUID(), file);

        assertNotNull(stored);
        assertEquals("application/zip", stored.contentType());
    }

    @Test
    void store_docxWithOctetStreamMimeType_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport.docx",
                "application/octet-stream",
                "docx-content".getBytes()
        );

        FileStorageService.StoredFile stored = fileStorageService.store(UUID.randomUUID(), file);

        assertNotNull(stored);
        assertEquals("application/octet-stream", stored.contentType());
    }

    @Test
    void store_exeWithOctetStreamMimeType_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "exe-content".getBytes()
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> fileStorageService.store(UUID.randomUUID(), file)
        );
        assertTrue(ex.getMessage().contains("Type de fichier non autorisé"));
    }

    @Test
    void store_pdfWithStandardMimeType_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        FileStorageService.StoredFile stored = fileStorageService.store(UUID.randomUUID(), file);

        assertNotNull(stored);
        assertEquals("application/pdf", stored.contentType());
    }
}
