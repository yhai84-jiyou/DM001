package com.helpmanual.entity;

import com.helpmanual.entity.enums.ArticleStatus;
import com.helpmanual.entity.enums.EditorMode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Lob
    @Column(name = "draft_content", columnDefinition = "TEXT")
    private String draftContent;

    @Lob
    @Column(name = "draft_content_html", columnDefinition = "TEXT")
    private String draftContentHtml;

    @Column(name = "published_title", length = 255)
    private String publishedTitle;

    @Lob
    @Column(name = "published_content_html", columnDefinition = "TEXT")
    private String publishedContentHtml;

    @Column(name = "published_summary", length = 500)
    private String publishedSummary;

    @Column(length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status = ArticleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "editor_mode", nullable = false, length = 20)
    private EditorMode editorMode = EditorMode.RICH;

    @Column(name = "current_version")
    private Long currentVersion;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "last_editor_id")
    private Long lastEditorId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Article() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDraftContent() { return draftContent; }
    public void setDraftContent(String draftContent) { this.draftContent = draftContent; }

    public String getDraftContentHtml() { return draftContentHtml; }
    public void setDraftContentHtml(String draftContentHtml) { this.draftContentHtml = draftContentHtml; }

    public String getPublishedTitle() { return publishedTitle; }
    public void setPublishedTitle(String publishedTitle) { this.publishedTitle = publishedTitle; }

    public String getPublishedContentHtml() { return publishedContentHtml; }
    public void setPublishedContentHtml(String publishedContentHtml) { this.publishedContentHtml = publishedContentHtml; }

    public String getPublishedSummary() { return publishedSummary; }
    public void setPublishedSummary(String publishedSummary) { this.publishedSummary = publishedSummary; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public ArticleStatus getStatus() { return status; }
    public void setStatus(ArticleStatus status) { this.status = status; }

    public EditorMode getEditorMode() { return editorMode; }
    public void setEditorMode(EditorMode editorMode) { this.editorMode = editorMode; }

    public Long getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Long currentVersion) { this.currentVersion = currentVersion; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public Long getLastEditorId() { return lastEditorId; }
    public void setLastEditorId(Long lastEditorId) { this.lastEditorId = lastEditorId; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
