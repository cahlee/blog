package com.blog.domain.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> findByUserId(Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    @Transactional
    public Category create(String name, com.blog.domain.user.User user) {
        if (categoryRepository.existsByNameAndUserId(name, user.getId())) {
            throw new IllegalArgumentException("Category already exists: " + name);
        }
        Category category = Category.builder()
                .name(name)
                .user(user)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Category category = findById(id);
        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Not authorized to delete this category.");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public Category update(Long id, String newName, Long userId) {
        Category category = findById(id);
        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Not authorized to update this category.");
        }
        category.setName(newName);
        return categoryRepository.save(category);
    }
}
