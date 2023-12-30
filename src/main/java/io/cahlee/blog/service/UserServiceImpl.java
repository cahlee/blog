package io.cahlee.blog.service;

import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.cahlee.blog.domain.User;
import io.cahlee.blog.domain.UserRole;
import io.cahlee.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;

	@Override
	public User getUser(String id) {
		return userRepository.findById(id).orElseThrow(() -> new NoSuchElementException());
	}

	@Override
	public void save(User user) {
		userRepository.save(user);
	}

	@Override
	public void register(User user) {
		if(userRepository.existsById(user.getId())) {
			throw new IllegalStateException();
		}
		
		User newUser = new User();
		newUser.setId(user.getId());
		newUser.setName(user.getName());
		newUser.setPassword(encoder.encode(user.getPassword()));
		newUser.setUserRole(UserRole.USER);
		
		userRepository.save(newUser);
	}

}
