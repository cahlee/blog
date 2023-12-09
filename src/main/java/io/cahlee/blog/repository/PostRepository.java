package io.cahlee.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cahlee.blog.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
	
}
