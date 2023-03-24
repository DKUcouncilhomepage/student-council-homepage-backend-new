package com.dku.council.domain.comment;

import com.dku.council.domain.comment.model.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByPostId(Long postId, Pageable pageable);

    List<Comment> findAllByPostIdAndUserId(Long postId, Long userId);

    Page<Comment> findAllByUserId(Long userId, Pageable pageable);
}
