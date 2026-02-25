package ru.kors.finalproject.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void buildDownloadResponse_supportsLegacyLeadingSlashPaths() throws IOException {
        FileStorageService service = new FileStorageService(
                tempDir.toString(),
                20,
                "application/pdf,application/zip,text/plain"
        );
        service.initStorageRoot();

        ResponseEntity<Resource> response = service.buildDownloadResponse(
                "/files/student_id_scan.pdf",
                "student_id_scan.pdf",
                "application/pdf"
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Files.exists(tempDir.resolve("files/student_id_scan.pdf"))).isTrue();
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("student_id_scan.pdf");
    }

    @Test
    void buildDownloadResponse_rejectsPathTraversal() {
        FileStorageService service = new FileStorageService(
                tempDir.toString(),
                20,
                "application/pdf,application/zip,text/plain"
        );
        service.initStorageRoot();

        assertThatThrownBy(() ->
                service.buildDownloadResponse("../secret.txt", "secret.txt", "text/plain")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid storage path");
    }
}
