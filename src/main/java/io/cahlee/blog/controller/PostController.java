package io.cahlee.blog.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cahlee.blog.domain.Post;
import io.cahlee.blog.domain.PostDto;
import io.cahlee.blog.service.PostService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {
	private final PostService postService;
	
	@GetMapping("/posts")
	ResponseEntity<List<PostDto>> listPost() {
		List<PostDto> posts = Post.map(postService.findAllPosts());
		if(posts.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(posts, HttpStatus.OK);
	}
	
	@GetMapping("/posts/{id}")
	ResponseEntity<PostDto> getPost(@PathVariable Long id) {
		Post post = null;
		
		try {
			post = postService.getPost(id);
		} catch(NoSuchElementException e) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		PostDto postDto = Post.map(post);
		
		return new ResponseEntity<>(postDto, HttpStatus.OK);
	}
	
	@PostMapping("/posts")
	ResponseEntity<PostDto> addPost(@RequestBody PostDto postDto) {
		Post post = Post.map(postDto);
		
		PostDto result = Post.map(postService.save(post));
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(result, responseHeaders, HttpStatus.CREATED);
	}
	
	@PutMapping("/posts")
	ResponseEntity<PostDto> updatePost(@RequestBody PostDto postDto) {
		Post post = Post.map(postDto);
		PostDto result = Post.map(postService.save(post));
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
	}
}
