package io.cahlee.blog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.cahlee.blog.domain.Post;

@SpringBootTest
class PostServiceTest {

	@Autowired
	PostService postService;

	@Test
	@DisplayName("Post 신규 생성 테스트")
	void savePostTest() throws Exception {
		Post post = createPost();
		
		Post result = postService.save(post);

		assertEquals(post.getTitle(), result.getTitle());
		assertEquals(post.getAuthor(), result.getAuthor());
		assertEquals(post.getContents(), result.getContents());
		assertNotNull(result.getCreatedDate());
		assertNotNull(result.getUpdatedDate());
	}
	
	@Test
	@DisplayName("Post ID 조회 테스트")
	void getPostTest() throws Exception {
		Post result = postService.save(createPost());
		
		Post post = postService.getPost(result.getId());
		
		assertEquals(result.getId(), post.getId());
		assertEquals(result.getTitle(), post.getTitle());
		assertEquals(result.getContents(), post.getContents());
		assertEquals(result.getCreatedDate(), post.getCreatedDate());
		assertEquals(result.getUpdatedDate(), post.getUpdatedDate());
	}
	
	@Test
	@DisplayName("Post ID 조회 시 Exception 테스트")
	void getPostNullTest() throws Exception {
		Assertions.assertThrows(NoSuchElementException.class, () -> postService.getPost(-1L));
	}

	private Post createPost() {
		Post post = new Post();
		post.setTitle("테스트1 제목");
		post.setAuthor("테스트 작성자");
		post.setContents("테스트 내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용");
		
		return post;
	}
}
