package io.cahlee.domain.post;

import io.cahlee.auth.CustomUserDetails;
import io.cahlee.auth.oauth2.CustomOAuth2UserDetailsAdapter;
import io.cahlee.domain.category.CategoryService;
import io.cahlee.domain.comment.CommentService;
import io.cahlee.domain.comment.dto.CommentCreateRequest;
import io.cahlee.domain.comment.dto.CommentResponse;
import io.cahlee.domain.post.dto.PostCreateRequest;
import io.cahlee.domain.post.dto.PostResponse;
import io.cahlee.domain.post.dto.PostUpdateRequest;
import io.cahlee.domain.tag.TagService;
import io.cahlee.domain.user.User;
import io.cahlee.domain.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CommentService commentService;
    private final UserService userService;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal Object principal) {
        User user = principal != null ? resolveUser(principal) : null;
        if (user != null) {
            return "redirect:/" + user.getUsername();
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/{username:[a-zA-Z0-9_]+}/{id:[0-9]+}")
    public String viewPost(@PathVariable String username,
                           @PathVariable Long id,
                           @AuthenticationPrincipal Object principal,
                           Model model) {
        PostResponse post;
        try {
            post = postService.getPostDetail(id);
        } catch (IllegalArgumentException e) {
            return "redirect:/?error=notfound";
        }

        User currentUser = principal != null ? resolveUser(principal) : null;

        if (!post.isPublic()) {
            if (currentUser == null) {
                return "redirect:/auth/login";
            }
            if (!post.getAuthorId().equals(currentUser.getId())) {
                throw new AccessDeniedException("This post is private.");
            }
        }

        List<CommentResponse> comments = commentService.findByPostId(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentForm", new CommentCreateRequest());

        if (currentUser != null) {
            model.addAttribute("currentUserId", currentUser.getId());
        }

        return "post/detail";
    }

    @GetMapping("/{username:[a-zA-Z0-9_]+}")
    public String userBlog(@PathVariable String username,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @AuthenticationPrincipal Object principal,
                           Model model) {
        User blogOwner;
        try {
            blogOwner = userService.findByUsername(username);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts;

        User currentUser = principal != null ? resolveUser(principal) : null;
        boolean isOwner = currentUser != null && currentUser.getId().equals(blogOwner.getId());
        if (isOwner) {
            posts = postService.findByUser(blogOwner.getId(), pageable);
        } else {
            posts = postService.findPublicByUser(blogOwner.getId(), pageable);
        }

        model.addAttribute("posts", posts);
        model.addAttribute("blogOwner", username);
        model.addAttribute("tags", null);
        model.addAttribute("categories", categoryService.findByUserId(blogOwner.getId()));
        return "index";
    }

    @GetMapping("/posts/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.search(q, pageable);
        model.addAttribute("posts", posts);
        model.addAttribute("query", q);
        model.addAttribute("tags", tagService.findAll());
        return "index";
    }

    @GetMapping("/posts/new")
    public String showCreateForm(@AuthenticationPrincipal Object principal, Model model) {
        User user = resolveUser(principal);
        model.addAttribute("postForm", new PostCreateRequest());
        model.addAttribute("categories", categoryService.findByUserId(user.getId()));
        model.addAttribute("post", null);
        return "post/form";
    }

    @PostMapping("/posts/new")
    public String createPost(@Valid @ModelAttribute("postForm") PostCreateRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal Object principal,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User user = resolveUser(principal);
            model.addAttribute("categories", categoryService.findByUserId(user.getId()));
            model.addAttribute("post", null);
            return "post/form";
        }

        User user = resolveUser(principal);
        Post created = postService.create(request, user);
        redirectAttributes.addFlashAttribute("success", "Post created successfully.");
        return "redirect:/" + user.getUsername() + "/" + created.getId();
    }

    @GetMapping("/{username:[a-zA-Z0-9_]+}/{id:[0-9]+}/edit")
    public String showEditForm(@PathVariable String username,
                               @PathVariable Long id,
                               @AuthenticationPrincipal Object principal,
                               Model model) {
        User user = resolveUser(principal);
        Post post = postService.findById(id);

        if (!post.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not authorized to edit this post.");
        }

        PostUpdateRequest form = new PostUpdateRequest();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        form.setPublicPost(post.isPublic());
        if (post.getCategory() != null) {
            form.setCategoryId(post.getCategory().getId());
        }

        model.addAttribute("postForm", form);
        model.addAttribute("post", post);
        model.addAttribute("categories", categoryService.findByUserId(user.getId()));
        return "post/form";
    }

    @PostMapping("/{username:[a-zA-Z0-9_]+}/{id:[0-9]+}/edit")
    public String updatePost(@PathVariable String username,
                             @PathVariable Long id,
                             @Valid @ModelAttribute("postForm") PostUpdateRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal Object principal,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User user = resolveUser(principal);
            model.addAttribute("categories", categoryService.findByUserId(user.getId()));
            model.addAttribute("post", postService.findById(id));
            return "post/form";
        }

        User user = resolveUser(principal);
        try {
            postService.update(id, request, user.getId());
            redirectAttributes.addFlashAttribute("success", "Post updated successfully.");
        } catch (IllegalStateException e) {
            throw new AccessDeniedException(e.getMessage());
        }
        return "redirect:/" + username + "/" + id;
    }

    @PostMapping("/{username:[a-zA-Z0-9_]+}/{id:[0-9]+}/delete")
    public String deletePost(@PathVariable String username,
                             @PathVariable Long id,
                             @AuthenticationPrincipal Object principal,
                             RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        try {
            postService.delete(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Post deleted.");
        } catch (IllegalStateException e) {
            throw new AccessDeniedException(e.getMessage());
        }
        return "redirect:/";
    }

    private User resolveUser(Object principal) {
        if (principal instanceof CustomUserDetails details) return details.getUser();
        if (principal instanceof CustomOAuth2UserDetailsAdapter adapter) return adapter.getUser();
        return null;
    }
}
