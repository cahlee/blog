package io.cahlee.domain.image;

import io.cahlee.auth.CustomUserDetails;
import io.cahlee.auth.oauth2.CustomOAuth2UserDetailsAdapter;
import io.cahlee.domain.user.User;
import io.cahlee.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final StorageService storageService;
    private final PostImageRepository postImageRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                         @AuthenticationPrincipal Object principal) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        try {
            String url = storageService.uploadFile(file);

            PostImage postImage = PostImage.builder()
                    .originalName(file.getOriginalFilename())
                    .storedUrl(url)
                    .build();
            postImageRepository.save(postImage);

            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            log.error("Failed to upload image", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    private User resolveUser(Object principal) {
        if (principal instanceof CustomUserDetails details) {
            return details.getUser();
        } else if (principal instanceof CustomOAuth2UserDetailsAdapter adapter) {
            return adapter.getUser();
        }
        return null;
    }
}
