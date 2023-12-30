package io.cahlee.blog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.cahlee.blog.service.PostService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TemplateContoller {
	private final PostService postService;
	
	@GetMapping("/")
	public String mainPage(Model model) {
		model.addAttribute("posts", postService.findPosts("cahlee"));
		return "main";
	}
	
	@GetMapping("/about")
	public String aboutPage() {
		return "about";
	}
	
	@GetMapping("/post/list")
	public String postListPage(Model model) {
		model.addAttribute("posts", postService.findPosts("cahlee"));
		return "post/list";
	}
	
	@GetMapping("/post/{postId}")
	public String postPage(@PathVariable Long postId, Model model) {
		model.addAttribute("post", postService.getPost(postId));
		return "post/detail";
	}
	
	@GetMapping("/post/new")
	public String postNewPage() {
		return "post/new";
	}
	
	@GetMapping("/post/update")
	public String posUpdatePage(@RequestParam Long postId, Model model) {
		model.addAttribute("post", postService.getPost(postId));
		return "post/update";
	}
	
	@GetMapping("/project/list")
	public String projectListPage() {
		return "project/list";
	}
	
	@GetMapping("/user/new")
	public String userNewPage() {
		return "user/new";
	}
}
