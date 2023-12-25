package io.cahlee.blog.service;

import io.cahlee.blog.domain.User;

public interface UserService {

	User getUser(String id);

	void save(User user);

}
