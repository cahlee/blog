package io.cahlee.blog.service;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import io.cahlee.blog.domain.User;
import io.cahlee.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	
	private final UserRepository userRepository;

	@Override
	public User getUser(String id) {
		return userRepository.findById(id).orElseThrow(() -> new NoSuchElementException());
	}

	@Override
	public void save(User user) {
		userRepository.save(user);
	}

}
