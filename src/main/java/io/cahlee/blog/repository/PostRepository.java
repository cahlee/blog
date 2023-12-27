package io.cahlee.blog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cahlee.blog.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	List<Post> findByAuthorId(String userId);
	
}
