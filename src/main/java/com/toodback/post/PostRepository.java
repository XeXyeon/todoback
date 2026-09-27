package com.toodback.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = "author")
    Optional<Post> findWithAuthorById(Long id);

    @Query(value = """
            select new com.toodback.post.PostSummaryResponse(
                p.id, p.title, a.nickname, count(c.id), p.createdAt, p.updatedAt)
            from Post p join p.author a left join Comment c on c.post = p
            group by p.id, p.title, a.nickname, p.createdAt, p.updatedAt
            """, countQuery = "select count(p) from Post p")
    Page<PostSummaryResponse> findSummaries(Pageable pageable);
}
