package io.cahlee.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private boolean publicPost = true;

    private Long categoryId;

    private List<String> tags;

    public boolean isPublic() {
        return publicPost;
    }
}
