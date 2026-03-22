package io.cahlee.domain.comment;

import io.cahlee.auth.CustomUserDetails;
import io.cahlee.auth.oauth2.CustomOAuth2UserDetailsAdapter;
import io.cahlee.domain.comment.dto.CommentCreateRequest;
import io.cahlee.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{username:[a-zA-Z0-9_]+}/{postId:[0-9]+}/comments")
    public String addComment(@PathVariable String username,
                             @PathVariable Long postId,
                             @Valid @ModelAttribute CommentCreateRequest request,
                             @AuthenticationPrincipal Object principal,
                             RedirectAttributes redirectAttributes) {
        User user = (principal != null) ? resolveUser(principal) : null;
        try {
            commentService.createComment(postId, request, user);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("commentError", e.getMessage());
        }
        return "redirect:/" + username + "/" + postId;
    }

    @PostMapping("/{username:[a-zA-Z0-9_]+}/{postId:[0-9]+}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable String username,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                @RequestParam(required = false) String guestPassword,
                                @AuthenticationPrincipal Object principal,
                                RedirectAttributes redirectAttributes) {
        User user = (principal != null) ? resolveUser(principal) : null;
        try {
            commentService.deleteComment(commentId, user, guestPassword);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("commentError", e.getMessage());
        }
        return "redirect:/" + username + "/" + postId;
    }

    private User resolveUser(Object principal) {
        if (principal instanceof CustomUserDetails details) return details.getUser();
        if (principal instanceof CustomOAuth2UserDetailsAdapter adapter) return adapter.getUser();
        return null;
    }
}
