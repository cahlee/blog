package io.cahlee.domain.user;

import io.cahlee.auth.CustomUserDetails;
import io.cahlee.auth.oauth2.CustomOAuth2UserDetailsAdapter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/auth/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match.");
            return "auth/register";
        }

        try {
            userService.register(form.getEmail(), form.getUsername(), form.getPassword());
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/auth/login")
    public String showLoginForm(@RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "auth/login";
    }

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal Object principal, Model model) {
        User user = resolveUser(principal);
        model.addAttribute("user", user);
        return "auth/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal Object principal,
                                @RequestParam String username,
                                RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        try {
            userService.updateProfile(user.getId(), username);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal Object principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/profile";
        }
        try {
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    private User resolveUser(Object principal) {
        if (principal instanceof CustomUserDetails details) {
            return details.getUser();
        } else if (principal instanceof CustomOAuth2UserDetailsAdapter adapter) {
            return adapter.getUser();
        }
        throw new IllegalStateException("Unknown principal type");
    }

    @Getter
    @Setter
    public static class RegisterForm {
        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Username is required")
        @Size(min = 2, max = 50, message = "Username must be 2-50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }
}
