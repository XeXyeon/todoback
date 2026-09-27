package com.toodback.comment;

import java.time.LocalDateTime;

public record CommentResponse(Long id, Long postId, String content, String authorNickname,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPost().getId(), comment.getContent(),
                comment.getAuthor().getNickname(), comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
