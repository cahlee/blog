package io.cahlee.domain.comment;

import io.cahlee.domain.comment.dto.CommentCreateRequest;
import io.cahlee.domain.comment.dto.CommentResponse;
import io.cahlee.domain.post.Post;
import io.cahlee.domain.post.PostRepository;
import io.cahlee.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Comment createComment(Long postId, CommentCreateRequest request, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        Comment.CommentBuilder builder = Comment.builder()
                .content(request.getContent())
                .post(post);

        if (currentUser != null) {
            builder.user(currentUser);
        } else {
            if (request.getGuestName() == null || request.getGuestName().isBlank()) {
                throw new IllegalArgumentException("Guest name is required for guest comments.");
            }
            if (request.getGuestPassword() == null || request.getGuestPassword().isBlank()) {
                throw new IllegalArgumentException("Guest password is required for guest comments.");
            }
            builder.guestName(request.getGuestName())
                   .guestPassword(passwordEncoder.encode(request.getGuestPassword()));
        }

        return commentRepository.save(builder.build());
    }

    @Transactional
    public void deleteComment(Long commentId, User currentUser, String guestPassword) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        if (comment.getUser() != null) {
            if (currentUser == null || !comment.getUser().getId().equals(currentUser.getId())) {
                throw new IllegalStateException("Not authorized to delete this comment.");
            }
        } else {
            if (guestPassword == null || guestPassword.isBlank()) {
                throw new IllegalArgumentException("Password is required to delete guest comment.");
            }
            if (!passwordEncoder.matches(guestPassword, comment.getGuestPassword())) {
                throw new IllegalArgumentException("Incorrect password.");
            }
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findByPostId(Long postId) {
        return commentRepository.findByPostIdWithUser(postId)
                .stream()
                .map(CommentResponse::new)
                .collect(Collectors.toList());
    }
}
