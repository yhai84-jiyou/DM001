package com.helpmanual.controller;

import com.helpmanual.dto.response.CategoryTreeNode;
import com.helpmanual.dto.response.PageResponse;
import com.helpmanual.dto.response.PublicArticleResponse;
import com.helpmanual.entity.Article;
import com.helpmanual.entity.enums.ArticleStatus;
import com.helpmanual.exception.ResourceNotFoundException;
import com.helpmanual.service.ArticleService;
import com.helpmanual.service.CategoryService;
import com.helpmanual.service.SearchService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final CategoryService categoryService;
    private final ArticleService articleService;
    private final SearchService searchService;

    public PublicController(CategoryService categoryService,
                            ArticleService articleService,
                            SearchService searchService) {
        this.categoryService = categoryService;
        this.articleService = articleService;
        this.searchService = searchService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryTreeNode>> categories() {
        return ResponseEntity.ok(categoryService.buildTree(true));
    }

    @GetMapping("/articles/{slug}")
    public ResponseEntity<PublicArticleResponse> articleBySlug(@PathVariable String slug) {
        Article article = articleService.findBySlug(slug);
        if (article.getStatus() != ArticleStatus.PUBLISHED || !article.isVisible()) {
            throw new ResourceNotFoundException("Article not found");
        }
        return ResponseEntity.ok(PublicArticleResponse.fromEntity(article));
    }

    @GetMapping("/categories/{categoryId}/articles")
    public ResponseEntity<List<PublicArticleResponse>> articlesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(
                articleService.findPublishedByCategoryId(categoryId).stream()
                        .map(PublicArticleResponse::fromEntity)
                        .toList()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<PublicArticleResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var results = searchService.searchPublished(q, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.from(results, PublicArticleResponse::fromEntity));
    }
}
