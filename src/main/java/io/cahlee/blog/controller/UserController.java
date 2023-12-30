package io.cahlee.blog.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.cahlee.blog.domain.User;
import io.cahlee.blog.domain.UserDto;
import io.cahlee.blog.service.UserService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
	
	private final UserService userService;

	@PostMapping("/users")
	public ResponseEntity<UserDto> addUser(@RequestBody UserDto userDto) {
		User user = User.map(userDto);
		
		userService.register(user);
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
	}
}
