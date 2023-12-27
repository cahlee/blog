package io.cahlee.blog.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import io.cahlee.blog.domain.Post;
import io.cahlee.blog.repository.PostRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

	private final PostRepository postRepository;

	@Override
	public Post save(Post post) {
		return postRepository.save(post);
	}

	@Override
	public Post getPost(Long id) {
		return postRepository.findById(id).orElseThrow(() -> new NoSuchElementException());
	}

	@Override
	public List<Post> findAllPosts() {
		return postRepository.findAll();
	}

	@Override
	public List<Post> findPosts(String userId) {
		return postRepository.findByAuthorId(userId);
	}

	@Override
	public void deletePost(Post post) {
		postRepository.delete(post);
	}

}
