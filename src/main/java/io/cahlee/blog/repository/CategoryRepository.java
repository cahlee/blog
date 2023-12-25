package io.cahlee.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cahlee.blog.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
