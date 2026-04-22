package com.cpt202.HerLink.util;

import com.cpt202.HerLink.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

// file store
@Component
public class FileStorageManager {

    private final Path uploadRootPath;

    public FileStorageManager(@Value("${HerLink.upload-dir:uploads}") String uploadDir) {
        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile multipartFile, String folderName) {
        StoredFile storedFile = storeFile(multipartFile, folderName);
        return storedFile == null ? null : storedFile.getFilePath();
    }

    public StoredFile storeFile(MultipartFile multipartFile, String folderName) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (!FileTypeValidator.isSupported(originalFilename)) {
            throw new AppException(400, "Unsupported file type.");
        }

        try {
            String safeFolderName = (folderName == null || folderName.isBlank()) ? "" : folderName.trim();
            Path targetFolderPath = safeFolderName.isEmpty()
                    ? uploadRootPath
                    : uploadRootPath.resolve(safeFolderName).normalize();

            Files.createDirectories(targetFolderPath);

            String fileExtension = "";
            if (originalFilename != null) {
                int lastDotIndex = originalFilename.lastIndexOf(".");
                if (lastDotIndex >= 0) {
                    fileExtension = originalFilename.substring(lastDotIndex);
                }
            }

            String storedFilename = UUID.randomUUID().toString().replace("-", "") + fileExtension;
            Path targetFilePath = targetFolderPath.resolve(storedFilename);

            multipartFile.transferTo(targetFilePath.toFile());

            String relativePath;
            if (safeFolderName.isEmpty()) {
                relativePath = storedFilename;
            } else {
                relativePath = safeFolderName + "/" + storedFilename;
            }

            StoredFile storedFile = new StoredFile();
            storedFile.setOriginalFilename(originalFilename);
            storedFile.setStoredFilename(storedFilename);
            storedFile.setFilePath(relativePath);
            storedFile.setFileSize(multipartFile.getSize());
            storedFile.setFileType(resolveFileType(originalFilename));
            return storedFile;
        } catch (IOException exception) {
            throw new AppException(
                    500,
                    "Failed to store uploaded file.",
                    java.util.List.of(exception.getMessage())
            );
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }

        try {
            Path targetPath = uploadRootPath.resolve(relativePath).normalize();

            if (!targetPath.startsWith(uploadRootPath)) {
                throw new AppException(400, "Invalid file path.");
            }

            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            throw new AppException(
                    500,
                    "Failed to delete stored file.",
                    java.util.List.of(exception.getMessage())
            );
        }
    }

    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }

        try {
            Path targetPath = uploadRootPath.resolve(relativePath).normalize();
            if (!targetPath.startsWith(uploadRootPath)) {
                return;
            }

            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
            // cleanup only, intentionally ignored
        }
    }

    private String resolveFileType(String filename) {
        String extension = FileTypeValidator.getNormalizedExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    public static class StoredFile {

        private String originalFilename;
        private String storedFilename;
        private String filePath;
        private String fileType;
        private long fileSize;

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getStoredFilename() {
            return storedFilename;
        }

        public void setStoredFilename(String storedFilename) {
            this.storedFilename = storedFilename;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
    }
}
