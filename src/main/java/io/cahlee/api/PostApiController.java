package io.cahlee.api;

import io.cahlee.auth.CustomUserDetails;
import io.cahlee.auth.oauth2.CustomOAuth2UserDetailsAdapter;
import io.cahlee.domain.post.Post;
import io.cahlee.domain.post.PostService;
import io.cahlee.domain.post.dto.PostCreateRequest;
import io.cahlee.domain.post.dto.PostResponse;
import io.cahlee.domain.post.dto.PostUpdateRequest;
import io.cahlee.domain.user.User;
import io.cahlee.domain.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;
    private final UserService userService;

    @GetMapping
    public Page<PostResponse> listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String q) {

        Pageable pageable = PageRequest.of(page, size);

        if (username != null) {
            try {
                User user = userService.findByUsername(username);
                return postService.findPublicByUser(user.getId(), pageable);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username);
            }
        }
        if (q != null && !q.isBlank()) {
            return postService.search(q, pageable);
        }
        return postService.findPublicPosts(pageable);
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id,
                                @AuthenticationPrincipal Object principal) {
        PostResponse post = postService.getPostDetail(id);
        if (!post.isPublic()) {
            if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            User user = resolveUser(principal);
            if (!post.getAuthorId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        return post;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody PostCreateRequest request,
                                   @AuthenticationPrincipal Object principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User user = resolveUser(principal);
        Post post = postService.create(request, user);
        return postService.toResponse(post.getId());
    }

    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id,
                                   @Valid @RequestBody PostUpdateRequest request,
                                   @AuthenticationPrincipal Object principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User user = resolveUser(principal);
        try {
            postService.update(id, request, user.getId());
            return postService.toResponse(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id,
                           @AuthenticationPrincipal Object principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User user = resolveUser(principal);
        try {
            postService.delete(id, user.getId());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    private User resolveUser(Object principal) {
        if (principal instanceof CustomUserDetails details) return details.getUser();
        if (principal instanceof CustomOAuth2UserDetailsAdapter adapter) return adapter.getUser();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
