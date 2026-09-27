package com.toodback.comment;

import com.toodback.member.Member;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable Long postId, @Valid @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal Member member) {
        return commentService.create(postId, request, member);
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long postId) {
        return commentService.list(postId);
    }

    @PutMapping("/{id}")
    public CommentResponse update(@PathVariable Long postId, @PathVariable Long id,
                                  @Valid @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal Member member) {
        return commentService.update(postId, id, request, member);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId, @PathVariable Long id,
                       @AuthenticationPrincipal Member member) {
        commentService.delete(postId, id, member);
    }
}
