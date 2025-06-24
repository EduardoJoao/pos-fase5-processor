package com.core.domain.video.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

class ZipServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void zipDirectory_shouldCreateZip() throws Exception {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "abc");
        ZipService service = new ZipService();
        Path zip = service.zipDirectory(tempDir, "testzip");
        assertThat(zip).exists();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            assertThat(zipFile.getEntry("file.txt")).isNotNull();
        }
        Files.deleteIfExists(zip);
    }
}