package io.cahlee.blog.service;

import org.springframework.stereotype.Service;

import io.cahlee.blog.domain.Category;
import io.cahlee.blog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;
	
	@Override
	public void save(Category category) {
		categoryRepository.save(category);
	}

}
