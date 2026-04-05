package com.helpmanual.service;

import com.helpmanual.entity.Article;
import com.helpmanual.entity.enums.ArticleStatus;
import com.helpmanual.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final ArticleRepository articleRepository;

    public SearchService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /**
     * Search published articles by keyword.
     * This is a placeholder implementation using LIKE queries.
     * Can be upgraded to SQLite FTS5 with native queries for better performance.
     */
    public Page<Article> searchPublished(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable);
    }
}
