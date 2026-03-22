package io.cahlee.domain.post.dto;

import io.cahlee.domain.post.Post;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class PostResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final String renderedContent;
    private final boolean isPublic;
    private final int viewCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String authorName;
    private final Long authorId;
    private final String categoryName;
    private final Long categoryId;
    private final Set<String> tags;

    public PostResponse(Post post, String renderedContent) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.renderedContent = renderedContent;
        this.isPublic = post.isPublic();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.authorName = post.getUser() != null ? post.getUser().getUsername() : "Unknown";
        this.authorId = post.getUser() != null ? post.getUser().getId() : null;
        this.categoryName = post.getCategory() != null ? post.getCategory().getName() : null;
        this.categoryId = post.getCategory() != null ? post.getCategory().getId() : null;
        this.tags = post.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toSet());
    }
}
