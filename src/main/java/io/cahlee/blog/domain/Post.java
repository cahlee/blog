package io.cahlee.blog.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Post {
	@Id
	@GeneratedValue
	private Long id;
	
	private String title;
	
	@ManyToOne
	private User author;
	
	@ManyToOne
	private Category category;
	
	@CreationTimestamp
	private Date createdDate;
	
	@UpdateTimestamp
	private Date updatedDate;
	
	@Column(length = 4000)
	private String contents;
	
	public static Post map(PostDto postDto) {
		Post post = new Post();
		
		post.setId(postDto.getId());
		
		User user = new User();
		user.setId(postDto.getAuthor());
		post.setAuthor(user);
		
		post.setTitle(postDto.getTitle());
		post.setContents(postDto.getContents());
		
		return post;
	}

	public static PostDto map(Post post) {
		PostDto postDto = new PostDto();
		
		postDto.setId(post.getId());
		postDto.setAuthor(post.getAuthor().getId());
		postDto.setTitle(post.getTitle());
		postDto.setContents(post.getContents());
		postDto.setCreatedDate(post.getCreatedDate());
		postDto.setUpdatedDate(post.getUpdatedDate());
		
		return postDto;
	}

	public static List<PostDto> map(List<Post> posts) {
		List<PostDto> postDtoList = new ArrayList<>();
		
		for(Post post : posts) {
			PostDto postDto = new PostDto();
			
			postDto.setId(post.getId());
			postDto.setAuthor(post.getAuthor().getId());
			postDto.setTitle(post.getTitle());
			postDto.setContents(post.getContents());
			postDto.setCreatedDate(post.getCreatedDate());
			postDto.setUpdatedDate(post.getUpdatedDate());
			
			postDtoList.add(postDto);
		}
		
		return postDtoList;
	}
}
