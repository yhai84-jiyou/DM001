package com.helpmanual.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "article_versions")
public class ArticleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "version_label", length = 50)
    private String versionLabel;

    @Column(name = "change_notes", length = 500)
    private String changeNotes;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ArticleVersion() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public String getChangeNotes() { return changeNotes; }
    public void setChangeNotes(String changeNotes) { this.changeNotes = changeNotes; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public Long getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
