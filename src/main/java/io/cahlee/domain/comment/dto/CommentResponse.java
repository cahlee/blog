package io.cahlee.domain.comment.dto;

import io.cahlee.domain.comment.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentResponse {

    private final Long id;
    private final String content;
    private final LocalDateTime createdAt;
    private final boolean deleted;
    private final String authorName;
    private final boolean isGuest;
    private final Long userId;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.deleted = comment.isDeleted();
        this.createdAt = comment.getCreatedAt();

        if (comment.isDeleted()) {
            this.content = "This comment has been deleted.";
            this.authorName = "";
            this.isGuest = false;
            this.userId = null;
        } else {
            this.content = comment.getContent();
            if (comment.getUser() != null) {
                this.authorName = comment.getUser().getUsername();
                this.isGuest = false;
                this.userId = comment.getUser().getId();
            } else {
                this.authorName = comment.getGuestName() != null ? comment.getGuestName() : "Guest";
                this.isGuest = true;
                this.userId = null;
            }
        }
    }
}
