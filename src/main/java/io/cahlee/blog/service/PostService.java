package io.cahlee.blog.service;

import java.util.List;

import io.cahlee.blog.domain.Post;

public interface PostService {

	Post save(Post post);

	Post getPost(Long id);

	List<Post> findAllPosts();

	List<Post> findPosts(String userId);

	void deletePost(Post post);

}
