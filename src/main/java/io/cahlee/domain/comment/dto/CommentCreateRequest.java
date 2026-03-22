package io.cahlee.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String content;

    private String guestName;

    private String guestPassword;
}
