package com.helpmanual.repository;

import com.helpmanual.entity.Article;
import com.helpmanual.entity.enums.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByCategoryIdAndDeletedFalseOrderBySortOrder(Long categoryId);

    Optional<Article> findBySlugAndDeletedFalse(String slug);

    List<Article> findByStatusAndVisibleTrueAndDeletedFalseOrderBySortOrder(ArticleStatus status);

    Page<Article> findByDeletedFalse(Pageable pageable);

    Page<Article> findByDeletedTrue(Pageable pageable);

    List<Article> findByDeletedFalseOrderBySortOrder();

    @Query("SELECT a FROM Article a WHERE a.deleted = false AND a.status = :status AND a.visible = true AND a.categoryId = :categoryId ORDER BY a.sortOrder")
    List<Article> findPublishedByCategoryId(@Param("categoryId") Long categoryId, @Param("status") ArticleStatus status);

    @Query("SELECT a FROM Article a WHERE a.deleted = false AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    boolean existsBySlug(String slug);

    @Query("SELECT a FROM Article a WHERE a.deleted = false AND a.categoryId = :categoryId ORDER BY a.sortOrder")
    List<Article> findByCategoryIdOrderBySortOrder(@Param("categoryId") Long categoryId);
}
