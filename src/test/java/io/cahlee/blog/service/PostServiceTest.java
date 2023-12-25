package io.cahlee.blog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.NoSuchElementException;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import io.cahlee.blog.domain.Category;
import io.cahlee.blog.domain.Post;
import io.cahlee.blog.domain.User;
import io.cahlee.blog.domain.UserRole;

@SpringBootTest
//@Transactional
@TestPropertySource(locations="classpath:application-test.properties")
class PostServiceTest {

	@Autowired
	PostService postService;

	@Autowired
	UserService userService;

	@Autowired
	CategoryService categoryService;

	@Test
	@DisplayName("Post 신규 생성 테스트")
	void savePostTest() throws Exception {
		User user = createUser();
		Category category = createCategory(user);
		userService.save(user);
		categoryService.save(category);
		
		Post post = createPost(user, category);
		
		Post result = postService.save(post);

		assertEquals(post.getTitle(), result.getTitle());
		assertEquals(post.getContents(), result.getContents());
		assertEquals(result.getTitle(), post.getTitle());
		assertEquals(result.getCategory().getUser(), user);
//		assertNotNull(result.getCreatedDate());
//		assertNotNull(result.getUpdatedDate());
	}
	
	@Test
	@DisplayName("Post ID 조회 테스트")
	void getPostTest() throws Exception {
		User user = createUser();
		Category category = createCategory(user);
		userService.save(user);
		categoryService.save(category);
		
		Post result = postService.save(createPost(user, category));
		
		Post post = postService.getPost(result.getId());
		
		assertEquals(result.getId(), post.getId());
		assertEquals(result.getCategory(), category);
		assertEquals(result.getTitle(), post.getTitle());
		assertEquals(result.getContents(), post.getContents());
		assertEquals(result.getCreatedDate(), post.getCreatedDate());
		assertEquals(result.getUpdatedDate(), post.getUpdatedDate());
		
		assertEquals(result.getCategory().getUser(), user);
	}
	
	@Test
	@DisplayName("Post List 조회 테스트")
	void getPostListTest() throws Exception {
		User user = createUser();
		Category category = createCategory(user);
		userService.save(user);
		categoryService.save(category);
		
		List<Post> sampleList = Lists.newArrayList();
		for(int i=0; i<3; i++) {
			sampleList.add(postService.save(createPost(user, category)));
		}
		
		User resultUser = userService.getUser(user.getId());
		Category resultCategory  = resultUser.getCategories().stream().filter(i -> i.getId() == category.getId()).findFirst().get();
		List<Post> resultList = resultCategory.getPosts();

		assertEquals(3, resultList.size());
	}
	
	@Test
	@DisplayName("Post ID 조회 시 Exception 테스트")
	void getPostNullTest() throws Exception {
		Assertions.assertThrows(NoSuchElementException.class, () -> postService.getPost(-1L));
	}

	private Post createPost(User user, Category category) {
		Post post = new Post();
		post.setTitle("테스트1 제목");
		post.setContents("테스트 내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용내용 내용");
		
		post.setCategory(category);
		category.getPosts().add(post);
		
		return post;
	}
	
	private Category createCategory(User user) {
		Category category = new Category();
		category.setName("스터디");
		category.setUser(user);
		user.getCategories().add(category);
		
		return category;
	}

	private User createUser() {
		User user = new User();
		user.setId("cahlee");
		user.setName("이창희");
		user.setUserRole(UserRole.USER);
		
		return user;
	}
}
