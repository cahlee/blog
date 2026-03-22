package com.blog.domain.category;

import com.blog.auth.CustomUserDetails;
import com.blog.auth.oauth2.CustomOAuth2UserDetailsAdapter;
import com.blog.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(@AuthenticationPrincipal Object principal, Model model) {
        User user = resolveUser(principal);
        List<Category> categories = categoryService.findByUserId(user.getId());
        model.addAttribute("categories", categories);
        return "category/list";
    }

    @PostMapping
    public String createCategory(@RequestParam String name,
                                 @AuthenticationPrincipal Object principal,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        try {
            categoryService.create(name, user);
            redirectAttributes.addFlashAttribute("success", "Category created.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 @AuthenticationPrincipal Object principal,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        try {
            categoryService.delete(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Category deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam String name,
                                 @AuthenticationPrincipal Object principal,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        try {
            categoryService.update(id, name, user.getId());
            redirectAttributes.addFlashAttribute("success", "Category updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    private User resolveUser(Object principal) {
        if (principal instanceof CustomUserDetails details) {
            return details.getUser();
        } else if (principal instanceof CustomOAuth2UserDetailsAdapter adapter) {
            return adapter.getUser();
        }
        throw new IllegalStateException("Unknown principal type");
    }
}
