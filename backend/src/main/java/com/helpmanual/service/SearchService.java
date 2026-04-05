package com.helpmanual.service;

import com.helpmanual.entity.Article;
import com.helpmanual.entity.enums.ArticleStatus;
import com.helpmanual.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final ArticleRepository articleRepository;
    private final FtsService ftsService;

    public SearchService(ArticleRepository articleRepository, FtsService ftsService) {
        this.articleRepository = articleRepository;
        this.ftsService = ftsService;
    }

    /**
     * Search published articles by keyword using FTS5.
     * Falls back to LIKE query if FTS returns no results.
     */
    public Page<Article> searchPublished(String keyword, Pageable pageable) {
        try {
            List<Long> articleIds = ftsService.search(keyword);
            if (!articleIds.isEmpty()) {
                List<Article> articles = articleRepository.findAllById(articleIds).stream()
                        .filter(a -> !a.isDeleted() && a.getStatus() == ArticleStatus.PUBLISHED && a.isVisible())
                        .toList();
                int start = (int) Math.min(pageable.getOffset(), articles.size());
                int end = Math.min(start + pageable.getPageSize(), articles.size());
                return new PageImpl<>(articles.subList(start, end), pageable, articles.size());
            }
        } catch (Exception e) {
            // FTS5 table might not exist yet or query failed; fall back to LIKE
        }
        return articleRepository.searchByKeyword(keyword, pageable);
    }
}
