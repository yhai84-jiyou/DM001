package com.helpmanual.repository;

import com.helpmanual.entity.ArticleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Long> {

    List<ArticleVersion> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    long countByArticleId(Long articleId);
}
