package com.ocp.eia.infrastructure.storage;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** Minimal ZIP/DOCX magic bytes (PK\\x03\\x04…). */
    private static byte[] docxMagicBytes() {
        return new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x08};
    }

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
    void store_docxWithZipMagic_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport.docx",
                DOCX_MIME,
                docxMagicBytes()
        );

        FileStorageService.StoredFile stored = fileStorageService.store(UUID.randomUUID(), file);

        assertNotNull(stored);
        assertEquals(DOCX_MIME, stored.contentType());
    }

    @Test
    void store_docxWithDeclaredMimeAndZipMagic_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport.docx",
                "application/octet-stream",
                docxMagicBytes()
        );

        FileStorageService.StoredFile stored = fileStorageService.store(UUID.randomUUID(), file);

        assertNotNull(stored);
        assertEquals(DOCX_MIME, stored.contentType());
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
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}
        );

        FileStorageService.StoredFile stored = fileStorageService.store(UUID.randomUUID(), file);

        assertNotNull(stored);
        assertEquals("application/pdf", stored.contentType());
    }
}
