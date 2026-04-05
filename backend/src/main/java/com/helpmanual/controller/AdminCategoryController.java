package com.helpmanual.controller;

import com.helpmanual.dto.request.CategoryRequest;
import com.helpmanual.dto.request.ReorderRequest;
import com.helpmanual.dto.response.CategoryResponse;
import com.helpmanual.dto.response.CategoryTreeNode;
import com.helpmanual.entity.Category;
import com.helpmanual.service.CategoryService;
import com.helpmanual.service.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final OperationLogService operationLogService;

    public AdminCategoryController(CategoryService categoryService,
                                   OperationLogService operationLogService) {
        this.categoryService = categoryService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryTreeNode>> tree() {
        return ResponseEntity.ok(categoryService.buildTree(false));
    }

    @GetMapping("/flat")
    public ResponseEntity<List<CategoryResponse>> flat() {
        return ResponseEntity.ok(
                categoryService.findAll().stream()
                        .map(CategoryResponse::fromEntity)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.fromEntity(categoryService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Category category = categoryService.create(
                request.name(), request.slug(), request.parentId(),
                request.icon(), request.sortOrder()
        );

        operationLogService.log(userId, "CREATE", "CATEGORY", category.getId(),
                "Created category: " + category.getName());

        return ResponseEntity.ok(CategoryResponse.fromEntity(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody CategoryRequest request,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Category category = categoryService.update(id, request.name(), request.slug(),
                request.parentId(), request.icon(), request.sortOrder());

        operationLogService.log(userId, "UPDATE", "CATEGORY", category.getId(),
                "Updated category: " + category.getName());

        return ResponseEntity.ok(CategoryResponse.fromEntity(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        categoryService.delete(id);

        operationLogService.log(userId, "DELETE", "CATEGORY", id, "Deleted category");

        return ResponseEntity.ok(Map.of("message", "Category deleted"));
    }

    @PostMapping("/reorder")
    public ResponseEntity<Map<String, String>> reorder(@Valid @RequestBody ReorderRequest request,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        categoryService.reorder(request.items());

        operationLogService.log(userId, "REORDER", "CATEGORY", null, "Reordered categories");

        return ResponseEntity.ok(Map.of("message", "Categories reordered"));
    }
}
