package com.toodback.comment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "author")
    List<Comment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId);

    @EntityGraph(attributePaths = "author")
    Optional<Comment> findWithAuthorById(Long id);

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteByPostId(Long postId);
}
