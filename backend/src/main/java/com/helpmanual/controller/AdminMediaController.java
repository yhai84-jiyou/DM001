package com.helpmanual.controller;

import com.helpmanual.dto.response.MediaResponse;
import com.helpmanual.dto.response.PageResponse;
import com.helpmanual.service.MediaService;
import com.helpmanual.service.OperationLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;
    private final OperationLogService operationLogService;

    public AdminMediaController(MediaService mediaService,
                                OperationLogService operationLogService) {
        this.mediaService = mediaService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaResponse> upload(@RequestParam("file") MultipartFile file,
                                                 Authentication authentication) throws IOException {
        Long userId = (Long) authentication.getPrincipal();
        var media = mediaService.upload(file, userId);

        operationLogService.log(userId, "UPLOAD", "MEDIA", media.getId(),
                "Uploaded file: " + media.getOriginalName());

        return ResponseEntity.ok(MediaResponse.fromEntity(media));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MediaResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                PageResponse.from(mediaService.list(PageRequest.of(page, size)),
                        MediaResponse::fromEntity)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        mediaService.delete(id);

        operationLogService.log(userId, "DELETE", "MEDIA", id, "Deleted media file");

        return ResponseEntity.ok(Map.of("message", "Media deleted"));
    }
}
