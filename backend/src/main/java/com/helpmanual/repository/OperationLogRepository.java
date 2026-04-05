package com.helpmanual.repository;

import com.helpmanual.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<OperationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM OperationLog o WHERE " +
           "(:userId IS NULL OR o.userId = :userId) AND " +
           "(:action IS NULL OR o.action = :action) AND " +
           "(:targetType IS NULL OR o.targetType = :targetType) " +
           "ORDER BY o.createdAt DESC")
    Page<OperationLog> findWithFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("targetType") String targetType,
            Pageable pageable);
}
