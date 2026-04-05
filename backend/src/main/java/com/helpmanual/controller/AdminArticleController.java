package com.helpmanual.controller;

import com.helpmanual.dto.request.ArticleRequest;
import com.helpmanual.dto.request.PublishRequest;
import com.helpmanual.dto.request.ReorderRequest;
import com.helpmanual.dto.response.ArticleResponse;
import com.helpmanual.dto.response.ArticleVersionResponse;
import com.helpmanual.dto.response.PageResponse;
import com.helpmanual.entity.Article;
import com.helpmanual.entity.enums.EditorMode;
import com.helpmanual.service.ArticleService;
import com.helpmanual.service.OperationLogService;
import com.helpmanual.util.HtmlSanitizer;
import com.helpmanual.util.MarkdownRenderer;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;
    private final OperationLogService operationLogService;

    public AdminArticleController(ArticleService articleService,
                                  OperationLogService operationLogService) {
        this.articleService = articleService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ArticleResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Article> articles = articleService.findAll(
                PageRequest.of(page, size, Sort.by("sortOrder").ascending()));
        return ResponseEntity.ok(PageResponse.from(articles, ArticleResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(ArticleResponse.fromEntity(articleService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        EditorMode mode = request.editorMode() != null ?
                EditorMode.valueOf(request.editorMode()) : EditorMode.RICH;

        Article article = articleService.create(
                request.title(), request.slug(), request.categoryId(),
                request.draftContent(), request.draftContentHtml(),
                request.summary(), mode, request.sortOrder(), userId
        );

        operationLogService.log(userId, "CREATE", "ARTICLE", article.getId(),
                "Created article: " + article.getTitle());

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ArticleRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        EditorMode mode = request.editorMode() != null ?
                EditorMode.valueOf(request.editorMode()) : null;

        Article article = articleService.update(id, request.title(), request.slug(),
                request.categoryId(), request.draftContent(), request.draftContentHtml(),
                request.summary(), mode, request.sortOrder(), userId);

        operationLogService.log(userId, "UPDATE", "ARTICLE", article.getId(),
                "Updated article: " + article.getTitle());

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ArticleResponse> publish(@PathVariable Long id,
                                                    @RequestBody(required = false) PublishRequest request,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String versionLabel = request != null ? request.versionLabel() : null;
        String changeNotes = request != null ? request.changeNotes() : null;

        Article article = articleService.publish(id, versionLabel, changeNotes, userId);

        operationLogService.log(userId, "PUBLISH", "ARTICLE", article.getId(),
                "Published article: " + article.getTitle());

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ArticleResponse> unpublish(@PathVariable Long id,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Article article = articleService.unpublish(id);

        operationLogService.log(userId, "UNPUBLISH", "ARTICLE", article.getId(),
                "Unpublished article: " + article.getTitle());

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ArticleVersionResponse>> versions(@PathVariable Long id) {
        return ResponseEntity.ok(
                articleService.findVersions(id).stream()
                        .map(ArticleVersionResponse::fromEntity)
                        .toList()
        );
    }

    @PostMapping("/{id}/revert/{versionId}")
    public ResponseEntity<ArticleResponse> revert(@PathVariable Long id,
                                                   @PathVariable Long versionId,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Article article = articleService.revertToVersion(id, versionId, userId);

        operationLogService.log(userId, "REVERT", "ARTICLE", article.getId(),
                "Reverted article to version " + versionId);

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @PostMapping("/{id}/trash")
    public ResponseEntity<ArticleResponse> trash(@PathVariable Long id,
                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Article article = articleService.softDelete(id);

        operationLogService.log(userId, "TRASH", "ARTICLE", article.getId(),
                "Moved article to trash: " + article.getTitle());

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ArticleResponse> restore(@PathVariable Long id,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Article article = articleService.restore(id);

        operationLogService.log(userId, "RESTORE", "ARTICLE", article.getId(),
                "Restored article from trash: " + article.getTitle());

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable Long id,
                                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleService.permanentDelete(id);

        operationLogService.log(userId, "PERMANENT_DELETE", "ARTICLE", id,
                "Permanently deleted article");

        return ResponseEntity.ok(Map.of("message", "Article permanently deleted"));
    }

    @GetMapping("/trash")
    public ResponseEntity<PageResponse<ArticleResponse>> trashList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Article> articles = articleService.findTrashed(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.from(articles, ArticleResponse::fromEntity));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ArticleResponse>> byCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(
                articleService.findByCategoryId(categoryId).stream()
                        .map(ArticleResponse::fromEntity)
                        .toList()
        );
    }

    @PostMapping("/reorder")
    public ResponseEntity<Map<String, String>> reorder(@Valid @RequestBody ReorderRequest request,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        articleService.reorder(request.items());

        operationLogService.log(userId, "REORDER", "ARTICLE", null, "Reordered articles");

        return ResponseEntity.ok(Map.of("message", "Articles reordered"));
    }

    @PostMapping("/import")
    public ResponseEntity<?> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("editorMode") String editorMode,
            Authentication authentication) throws IOException {
        Long userId = (Long) authentication.getPrincipal();

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "File name is required"));
        }

        Set<String> allowedExtensions = Set.of(".md", ".docx");
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase()
                : "";
        if (!allowedExtensions.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only .md and .docx files are supported"));
        }

        String title = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        EditorMode mode = EditorMode.valueOf(editorMode.toUpperCase());

        String draftContent;
        String draftContentHtml;

        if (".md".equals(ext)) {
            draftContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            draftContentHtml = HtmlSanitizer.sanitize(MarkdownRenderer.render(draftContent));
        } else {
            // .docx: frontend converts via mammoth.js and sends HTML as file content
            draftContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            draftContentHtml = HtmlSanitizer.sanitize(draftContent);
        }

        Article article = articleService.create(
                title, null, categoryId,
                draftContent, draftContentHtml,
                null, mode, null, userId
        );

        operationLogService.log(userId, "IMPORT", "ARTICLE", article.getId(),
                "Imported article from file: " + originalFilename);

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }
}
