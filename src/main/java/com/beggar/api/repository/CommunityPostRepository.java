package com.beggar.api.repository;

import com.beggar.api.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findAllByOrderByCreatedAtDesc();

    List<CommunityPost> findAllByCategoryOrderByCreatedAtDesc(String category);
}
