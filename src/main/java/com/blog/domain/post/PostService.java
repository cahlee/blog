package com.blog.domain.post;

import com.blog.domain.category.Category;
import com.blog.domain.category.CategoryRepository;
import com.blog.domain.post.dto.PostCreateRequest;
import com.blog.domain.post.dto.PostResponse;
import com.blog.domain.post.dto.PostUpdateRequest;
import com.blog.domain.tag.Tag;
import com.blog.domain.tag.TagService;
import com.blog.domain.user.User;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagService tagService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public PostService(PostRepository postRepository,
                       CategoryRepository categoryRepository,
                       TagService tagService) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.tagService = tagService;

        MutableDataSet options = new MutableDataSet();
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    public String renderMarkdown(String markdown) {
        if (markdown == null) return "";
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }

    @Transactional
    public Post create(PostCreateRequest request, User author) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        List<Tag> tags = tagService.findOrCreateTags(request.getTags());

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isPublic(request.isPublic())
                .user(author)
                .category(category)
                .tags(new HashSet<>(tags))
                .build();

        return postRepository.save(post);
    }

    @Transactional
    public Post update(Long postId, PostUpdateRequest request, Long userId) {
        Post post = getPostForEdit(postId, userId);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        List<Tag> tags = tagService.findOrCreateTags(request.getTags());

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setPublic(request.isPublic());
        post.setCategory(category);
        post.setTags(new HashSet<>(tags));

        return postRepository.save(post);
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = getPostForEdit(postId, userId);
        postRepository.delete(post);
    }

    @Transactional
    public PostResponse getPostDetail(Long postId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        postRepository.incrementViewCount(postId);
        String rendered = renderMarkdown(post.getContent());
        return new PostResponse(post, rendered);
    }

    @Transactional(readOnly = true)
    public Post findById(Long id) {
        return postRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> findPublicPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);
        return posts.map(p -> new PostResponse(p, ""));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> findByCategory(Long categoryId, Pageable pageable) {
        Page<Post> posts = postRepository.findByCategoryIdAndIsPublicTrueOrderByCreatedAtDesc(categoryId, pageable);
        return posts.map(p -> new PostResponse(p, ""));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return findPublicPosts(pageable);
        }

        // MySQL FULLTEXT 우선, 실패 시 LIKE 검색으로 폴백 (H2, FULLTEXT 인덱스 미설치 환경 대응)
        try {
            return postRepository.searchByFullText("+" + keyword + "*", pageable)
                    .map(p -> new PostResponse(p, ""));
        } catch (Exception e) {
            log.debug("Fulltext search unavailable, falling back to LIKE search: {}", e.getMessage());
            return postRepository.searchByKeyword(keyword, pageable)
                    .map(p -> new PostResponse(p, ""));
        }
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> findByUser(Long userId, Pageable pageable) {
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return posts.map(p -> new PostResponse(p, ""));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> findPublicByUser(Long userId, Pageable pageable) {
        Page<Post> posts = postRepository.findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(userId, pageable);
        return posts.map(p -> new PostResponse(p, ""));
    }

    @Transactional(readOnly = true)
    public PostResponse toResponse(Long postId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        return new PostResponse(post, renderMarkdown(post.getContent()));
    }

    private Post getPostForEdit(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Not authorized to edit this post.");
        }
        return post;
    }
}
