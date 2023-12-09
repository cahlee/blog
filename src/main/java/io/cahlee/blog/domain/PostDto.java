package io.cahlee.blog.domain;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDto {
	private Long id;
	private String title;
	private String author;
	private Date createdDate;
	private Date updatedDate;
	private String contents;
}
