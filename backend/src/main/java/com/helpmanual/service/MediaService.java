package com.helpmanual.service;

import com.helpmanual.entity.Media;
import com.helpmanual.exception.BadRequestException;
import com.helpmanual.exception.ResourceNotFoundException;
import com.helpmanual.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final String uploadDir;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            "application/x-rar-compressed",
            "text/plain",
            "text/csv",
            "video/mp4",
            "video/webm",
            "audio/mpeg",
            "audio/wav"
    );

    public MediaService(MediaRepository mediaRepository,
                        @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.mediaRepository = mediaRepository;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public Media upload(MultipartFile file, Long uploadedBy) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BadRequestException("File type not allowed: " + contentType);
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Media media = new Media();
        media.setOriginalName(originalName);
        media.setStoredName(storedName);
        media.setFilePath(filePath.toString());
        media.setFileSize(file.getSize());
        media.setContentType(contentType);
        media.setUploadedBy(uploadedBy);

        return mediaRepository.save(media);
    }

    public Page<Media> list(Pageable pageable) {
        return mediaRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public void delete(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));

        try {
            Path filePath = Paths.get(media.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail
        }

        mediaRepository.delete(media);
    }

    public Media findById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));
    }
}
