package com.helpmanual.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FtsService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void syncArticle(Long articleId, String title, String htmlContent) {
        String plainText = htmlContent != null ? Jsoup.parse(htmlContent).text() : "";

        // Delete existing entry
        entityManager.createNativeQuery("DELETE FROM article_fts WHERE article_id = :articleId")
                .setParameter("articleId", articleId)
                .executeUpdate();

        // Insert new entry
        entityManager.createNativeQuery(
                "INSERT INTO article_fts(title, content, article_id) VALUES (:title, :content, :articleId)")
                .setParameter("title", title != null ? title : "")
                .setParameter("content", plainText)
                .setParameter("articleId", articleId)
                .executeUpdate();
    }

    @Transactional
    public void removeArticle(Long articleId) {
        entityManager.createNativeQuery("DELETE FROM article_fts WHERE article_id = :articleId")
                .setParameter("articleId", articleId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<Long> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        // Escape special FTS5 characters and wrap in quotes for safe matching
        String safeKeyword = "\"" + keyword.replace("\"", "\"\"") + "\"";
        return entityManager.createNativeQuery(
                "SELECT article_id FROM article_fts WHERE article_fts MATCH :keyword ORDER BY rank")
                .setParameter("keyword", safeKeyword)
                .getResultList();
    }
}
