package io.cahlee.blog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.cahlee.blog.service.PostService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TemplateContoller {
	private final PostService postService;
	
	@GetMapping("/")
	public String mainPage(Model model) {
		model.addAttribute("posts", postService.findAllPosts());
		return "main";
	}
	
	@GetMapping("/post/new")
	public String postNewPage() {
		return "postForm";
	}
}
