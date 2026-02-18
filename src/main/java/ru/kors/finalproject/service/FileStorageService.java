package ru.kors.finalproject.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path storageRoot;
    private final long maxFileSizeBytes;
    private final Set<String> allowedContentTypes;

    public FileStorageService(
            @Value("${app.storage.root:./storage}") String storageRoot,
            @Value("${app.storage.max-file-size-mb:20}") long maxFileSizeMb,
            @Value("${app.storage.allowed-content-types:application/pdf,image/png,image/jpeg,application/zip,text/plain}") String allowedContentTypes) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.maxFileSizeBytes = Math.max(maxFileSizeMb, 1) * 1024 * 1024;
        this.allowedContentTypes = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    @PostConstruct
    void initStorageRoot() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize storage directory", ex);
        }
    }

    public StoredFile store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File exceeds max size");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Unsupported file content type: " + contentType);
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String relativePath = generateRelativePath(folder, originalName);
        Path target = resolveStoragePath(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file", ex);
        }
        return new StoredFile(originalName, relativePath.replace('\\', '/'), contentType, file.getSize());
    }

    public StoredFile storeBytes(byte[] content, String originalName, String contentType, String folder) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content is required");
        }
        if (content.length > maxFileSizeBytes) {
            throw new IllegalArgumentException("File exceeds max size");
        }
        String normalizedContentType = normalizeContentType(contentType);
        if (!isAllowedContentType(normalizedContentType)) {
            throw new IllegalArgumentException("Unsupported file content type: " + normalizedContentType);
        }
        String safeOriginalName = sanitizeFilename(originalName);
        String relativePath = generateRelativePath(folder, safeOriginalName);
        Path target = resolveStoragePath(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store generated file", ex);
        }
        return new StoredFile(safeOriginalName, relativePath.replace('\\', '/'), normalizedContentType, content.length);
    }

    public ResponseEntity<Resource> buildDownloadResponse(String relativePath, String originalName, String contentType) {
        Path filePath = resolveStoragePath(relativePath);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("File not found");
        }
        Resource resource = new PathResource(filePath);
        String safeName = sanitizeFilename(originalName);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(safeName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(normalizeContentType(contentType)))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    public void deleteSilently(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path path = resolveStoragePath(relativePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private boolean isAllowedContentType(String contentType) {
        if (allowedContentTypes.contains(contentType)) {
            return true;
        }
        int slashIndex = contentType.indexOf('/');
        if (slashIndex < 1) {
            return false;
        }
        String wildcard = contentType.substring(0, slashIndex) + "/*";
        return allowedContentTypes.contains(wildcard);
    }

    private Path resolveStoragePath(String relativePath) {
        Path resolved = storageRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        return resolved;
    }

    private static String generateRelativePath(String folder, String originalName) {
        String ext = extractExtension(originalName);
        LocalDate now = LocalDate.now();
        return String.format(
                "%s/%d/%02d/%02d/%s%s",
                sanitizeFolder(folder),
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                UUID.randomUUID(),
                ext
        );
    }

    private static String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "misc";
        }
        return folder.replace("\\", "/")
                .replaceAll("[^a-zA-Z0-9/_-]", "_")
                .replaceAll("/+", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private static String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "file.bin";
        }
        String cleaned = original.replace("\\", "/");
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.isBlank()) {
            return "file.bin";
        }
        return cleaned;
    }

    private static String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot);
        if (ext.length() > 10) {
            return "";
        }
        return ext;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType.trim().toLowerCase();
    }

    public record StoredFile(String originalName, String storagePath, String contentType, long sizeBytes) {
    }
}
