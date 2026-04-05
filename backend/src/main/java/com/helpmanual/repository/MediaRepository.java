package com.helpmanual.repository;

import com.helpmanual.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    Page<Media> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
