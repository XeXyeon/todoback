package com.toodback.comment;

import com.toodback.common.ForbiddenException;
import com.toodback.common.NotFoundException;
import com.toodback.member.Member;
import com.toodback.post.Post;
import com.toodback.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public CommentResponse create(Long postId, CommentRequest request, Member member) {
        Post post = findPost(postId);
        return CommentResponse.from(commentRepository.save(new Comment(request.content(), member, post)));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long postId) {
        findPost(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(postId)
                .stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse update(Long postId, Long id, CommentRequest request, Member member) {
        Comment comment = findComment(postId, id);
        requireAuthor(comment, member);
        comment.update(request.content());
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long postId, Long id, Member member) {
        Comment comment = findComment(postId, id);
        requireAuthor(comment, member);
        commentRepository.delete(comment);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    private Comment findComment(Long postId, Long id) {
        Comment comment = commentRepository.findWithAuthorById(id)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
        if (!comment.getPost().getId().equals(postId)) {
            throw new NotFoundException("댓글을 찾을 수 없습니다.");
        }
        return comment;
    }

    private void requireAuthor(Comment comment, Member member) {
        if (!comment.getAuthor().getId().equals(member.getId())) {
            throw new ForbiddenException("작성자만 변경할 수 있습니다.");
        }
    }
}
