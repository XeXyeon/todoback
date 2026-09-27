package com.toodback.post;

import com.toodback.comment.CommentRepository;
import com.toodback.common.ForbiddenException;
import com.toodback.common.NotFoundException;
import com.toodback.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostResponse create(PostRequest request, Member member) {
        return PostResponse.from(postRepository.save(new Post(request.title(), request.content(), member)));
    }

    @Transactional(readOnly = true)
    public PostPageResponse list(int page, int size) {
        Page<PostSummaryResponse> result = postRepository.findSummaries(PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new PostPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) {
        return PostResponse.from(findPost(id));
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request, Member member) {
        Post post = findPost(id);
        requireAuthor(post, member);
        post.update(request.title(), request.content());
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long id, Member member) {
        Post post = findPost(id);
        requireAuthor(post, member);
        commentRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    private Post findPost(Long id) {
        return postRepository.findWithAuthorById(id)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    private void requireAuthor(Post post, Member member) {
        if (!post.getAuthor().getId().equals(member.getId())) {
            throw new ForbiddenException("작성자만 변경할 수 있습니다.");
        }
    }
}
