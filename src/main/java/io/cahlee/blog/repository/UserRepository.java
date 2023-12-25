package io.cahlee.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cahlee.blog.domain.User;

public interface UserRepository extends JpaRepository<User, String> {

}
