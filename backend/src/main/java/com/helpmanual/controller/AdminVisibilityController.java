package com.helpmanual.controller;

import com.helpmanual.dto.request.BatchVisibilityRequest;
import com.helpmanual.dto.request.VisibilityRequest;
import com.helpmanual.dto.response.ArticleResponse;
import com.helpmanual.dto.response.CategoryResponse;
import com.helpmanual.dto.response.CategoryTreeNode;
import com.helpmanual.service.ArticleService;
import com.helpmanual.service.CategoryService;
import com.helpmanual.service.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/visibility")
public class AdminVisibilityController {

    private final CategoryService categoryService;
    private final ArticleService articleService;
    private final OperationLogService operationLogService;

    public AdminVisibilityController(CategoryService categoryService,
                                     ArticleService articleService,
                                     OperationLogService operationLogService) {
        this.categoryService = categoryService;
        this.articleService = articleService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeNode>> tree() {
        return ResponseEntity.ok(categoryService.buildTree(false));
    }

    @PutMapping("/category/{id}")
    public ResponseEntity<CategoryResponse> toggleCategory(@PathVariable Long id,
                                                            @Valid @RequestBody VisibilityRequest request,
                                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        var category = categoryService.toggleVisibility(id, request.visible());

        operationLogService.log(userId, "TOGGLE_VISIBILITY", "CATEGORY", id,
                "Set category visibility to " + request.visible());

        return ResponseEntity.ok(CategoryResponse.fromEntity(category));
    }

    @PutMapping("/article/{id}")
    public ResponseEntity<ArticleResponse> toggleArticle(@PathVariable Long id,
                                                          @Valid @RequestBody VisibilityRequest request,
                                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        var article = articleService.toggleVisibility(id, request.visible());

        operationLogService.log(userId, "TOGGLE_VISIBILITY", "ARTICLE", id,
                "Set article visibility to " + request.visible());

        return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, String>> batchUpdate(@Valid @RequestBody BatchVisibilityRequest request,
                                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        if (request.categories() != null) {
            for (var item : request.categories()) {
                categoryService.toggleVisibility(item.id(), item.visible());
            }
        }
        if (request.articles() != null) {
            for (var item : request.articles()) {
                articleService.toggleVisibility(item.id(), item.visible());
            }
        }

        operationLogService.log(userId, "BATCH_VISIBILITY", "MIXED", null,
                "Batch updated visibility settings");

        return ResponseEntity.ok(Map.of("message", "Visibility updated"));
    }
}
