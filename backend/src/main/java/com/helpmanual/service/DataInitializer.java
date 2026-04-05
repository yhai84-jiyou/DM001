package com.helpmanual.service;

import com.helpmanual.entity.Article;
import com.helpmanual.entity.Category;
import com.helpmanual.entity.enums.ArticleStatus;
import com.helpmanual.entity.enums.EditorMode;
import com.helpmanual.repository.ArticleRepository;
import com.helpmanual.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    public DataInitializer(CategoryRepository categoryRepository,
                           ArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional
    public void seedSampleData(Long adminId) {
        if (categoryRepository.count() > 0) {
            return;
        }

        // Create sample categories
        Category gettingStarted = createCategory("Getting Started", "getting-started", null, "book", 0);
        Category userGuide = createCategory("User Guide", "user-guide", null, "document", 1);
        Category faq = createCategory("FAQ", "faq", null, "question", 2);
        Category advanced = createCategory("Advanced", "advanced", gettingStarted.getId(), "settings", 0);

        // Create sample articles
        createArticle(gettingStarted.getId(), "Welcome", "welcome",
                "<h1>Welcome to Help Manual</h1><p>This is your help manual website. Start by creating categories and articles.</p>",
                "Welcome to the Help Manual website.", adminId, 0, true);

        createArticle(gettingStarted.getId(), "Quick Start Guide", "quick-start",
                "<h1>Quick Start Guide</h1><p>Follow these steps to get started:</p><ol><li>Create categories</li><li>Write articles</li><li>Publish content</li></ol>",
                "A quick start guide to help you get up and running.", adminId, 1, true);

        createArticle(userGuide.getId(), "Managing Articles", "managing-articles",
                "<h1>Managing Articles</h1><p>Learn how to create, edit, and publish articles in the help manual.</p>",
                "Guide to managing articles.", adminId, 0, true);

        createArticle(faq.getId(), "Frequently Asked Questions", "frequently-asked-questions",
                "<h1>FAQ</h1><p><strong>Q: How do I create a new article?</strong></p><p>A: Navigate to the admin panel and click 'New Article'.</p>",
                "Common questions and answers.", adminId, 0, true);
    }

    private Category createCategory(String name, String slug, Long parentId, String icon, int sortOrder) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setParentId(parentId);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        category.setVisible(true);
        return categoryRepository.save(category);
    }

    private Article createArticle(Long categoryId, String title, String slug,
                                  String content, String summary, Long authorId,
                                  int sortOrder, boolean publish) {
        Article article = new Article();
        article.setCategoryId(categoryId);
        article.setTitle(title);
        article.setSlug(slug);
        article.setDraftContent(content);
        article.setDraftContentHtml(content);
        article.setSummary(summary);
        article.setEditorMode(EditorMode.RICH);
        article.setSortOrder(sortOrder);
        article.setAuthorId(authorId);
        article.setLastEditorId(authorId);

        if (publish) {
            article.setStatus(ArticleStatus.PUBLISHED);
            article.setPublishedTitle(title);
            article.setPublishedContentHtml(content);
            article.setPublishedSummary(summary);
            article.setPublishedAt(LocalDateTime.now());
        } else {
            article.setStatus(ArticleStatus.DRAFT);
        }

        return articleRepository.save(article);
    }
}
