package com.helpmanual.service;

import com.helpmanual.entity.Article;
import com.helpmanual.entity.ArticleVersion;
import com.helpmanual.entity.enums.ArticleStatus;
import com.helpmanual.entity.enums.EditorMode;
import com.helpmanual.exception.BadRequestException;
import com.helpmanual.exception.ResourceNotFoundException;
import com.helpmanual.repository.ArticleRepository;
import com.helpmanual.repository.ArticleVersionRepository;
import org.jsoup.Jsoup;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository articleVersionRepository;

    public ArticleService(ArticleRepository articleRepository,
                          ArticleVersionRepository articleVersionRepository) {
        this.articleRepository = articleRepository;
        this.articleVersionRepository = articleVersionRepository;
    }

    @Transactional
    public Article create(String title, String slug, Long categoryId, String draftContent,
                          String draftContentHtml, String summary, EditorMode editorMode,
                          Integer sortOrder, Long authorId) {
        if (!StringUtils.hasText(slug)) {
            slug = generateSlug(title);
        }
        if (articleRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Article article = new Article();
        article.setTitle(title);
        article.setSlug(slug);
        article.setCategoryId(categoryId);
        article.setDraftContent(draftContent);
        article.setDraftContentHtml(draftContentHtml);
        article.setSummary(summary != null ? summary : generateSummary(draftContentHtml));
        article.setEditorMode(editorMode != null ? editorMode : EditorMode.RICH);
        article.setSortOrder(sortOrder != null ? sortOrder : 0);
        article.setAuthorId(authorId);
        article.setLastEditorId(authorId);
        article.setStatus(ArticleStatus.DRAFT);

        return articleRepository.save(article);
    }

    @Transactional
    public Article update(Long id, String title, String slug, Long categoryId,
                          String draftContent, String draftContentHtml, String summary,
                          EditorMode editorMode, Integer sortOrder, Long editorId) {
        Article article = findById(id);

        if (StringUtils.hasText(title)) {
            article.setTitle(title);
        }
        if (StringUtils.hasText(slug) && !slug.equals(article.getSlug())) {
            if (articleRepository.existsBySlug(slug)) {
                throw new BadRequestException("Article slug already exists");
            }
            article.setSlug(slug);
        }
        if (categoryId != null) {
            article.setCategoryId(categoryId);
        }
        if (draftContent != null) {
            article.setDraftContent(draftContent);
        }
        if (draftContentHtml != null) {
            article.setDraftContentHtml(draftContentHtml);
        }
        if (summary != null) {
            article.setSummary(summary);
        }
        if (editorMode != null) {
            article.setEditorMode(editorMode);
        }
        if (sortOrder != null) {
            article.setSortOrder(sortOrder);
        }
        article.setLastEditorId(editorId);

        return articleRepository.save(article);
    }

    @Transactional
    @CacheEvict(value = "publicArticles", allEntries = true)
    public Article publish(Long id, String changeNotes, Long publisherId) {
        Article article = findById(id);

        // Create version
        long versionCount = articleVersionRepository.countByArticleId(id);
        ArticleVersion version = new ArticleVersion();
        version.setArticleId(id);
        version.setVersionLabel("v" + (versionCount + 1));
        version.setChangeNotes(changeNotes);
        version.setTitle(article.getTitle());
        version.setContent(article.getDraftContent());
        version.setContentHtml(article.getDraftContentHtml());
        version.setPublishedBy(publisherId);
        ArticleVersion savedVersion = articleVersionRepository.save(version);

        // Update article
        article.setPublishedTitle(article.getTitle());
        article.setPublishedContentHtml(article.getDraftContentHtml());
        article.setPublishedSummary(article.getSummary());
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setCurrentVersion(savedVersion.getId());
        article.setPublishedAt(LocalDateTime.now());
        article.setLastEditorId(publisherId);

        return articleRepository.save(article);
    }

    @Transactional
    @CacheEvict(value = "publicArticles", allEntries = true)
    public Article unpublish(Long id) {
        Article article = findById(id);
        article.setStatus(ArticleStatus.DRAFT);
        article.setPublishedTitle(null);
        article.setPublishedContentHtml(null);
        article.setPublishedSummary(null);
        article.setPublishedAt(null);
        return articleRepository.save(article);
    }

    @Transactional
    public Article softDelete(Long id) {
        Article article = findById(id);
        article.setDeleted(true);
        return articleRepository.save(article);
    }

    @Transactional
    public Article restore(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        article.setDeleted(false);
        return articleRepository.save(article);
    }

    @Transactional
    @CacheEvict(value = "publicArticles", allEntries = true)
    public void permanentDelete(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        articleVersionRepository.deleteAll(articleVersionRepository.findByArticleIdOrderByCreatedAtDesc(id));
        articleRepository.delete(article);
    }

    @Transactional
    public Article revertToVersion(Long articleId, Long versionId, Long editorId) {
        Article article = findById(articleId);
        ArticleVersion version = articleVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));

        if (!version.getArticleId().equals(articleId)) {
            throw new BadRequestException("Version does not belong to this article");
        }

        article.setTitle(version.getTitle());
        article.setDraftContent(version.getContent());
        article.setDraftContentHtml(version.getContentHtml());
        article.setLastEditorId(editorId);

        return articleRepository.save(article);
    }

    @Transactional
    @CacheEvict(value = "publicArticles", allEntries = true)
    public Article toggleVisibility(Long id, boolean visible) {
        Article article = findById(id);
        article.setVisible(visible);
        return articleRepository.save(article);
    }

    public Article findById(Long id) {
        return articleRepository.findById(id)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
    }

    public Article findBySlug(String slug) {
        return articleRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
    }

    public Page<Article> findAll(Pageable pageable) {
        return articleRepository.findByDeletedFalse(pageable);
    }

    public Page<Article> findTrashed(Pageable pageable) {
        return articleRepository.findByDeletedTrue(pageable);
    }

    public List<Article> findByCategoryId(Long categoryId) {
        return articleRepository.findByCategoryIdAndDeletedFalseOrderBySortOrder(categoryId);
    }

    public List<Article> findPublishedByCategoryId(Long categoryId) {
        return articleRepository.findPublishedByCategoryId(categoryId, ArticleStatus.PUBLISHED);
    }

    public List<Article> findPublished() {
        return articleRepository.findByStatusAndVisibleTrueAndDeletedFalseOrderBySortOrder(ArticleStatus.PUBLISHED);
    }

    public List<ArticleVersion> findVersions(Long articleId) {
        return articleVersionRepository.findByArticleIdOrderByCreatedAtDesc(articleId);
    }

    public Page<Article> search(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable);
    }

    @Transactional
    public void reorder(List<com.helpmanual.dto.request.ReorderRequest.ReorderItem> items) {
        for (var item : items) {
            Article article = findById(item.id());
            article.setSortOrder(item.sortOrder());
            articleRepository.save(article);
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String generateSummary(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = Jsoup.parse(html).text();
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}
