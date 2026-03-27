package io.cahlee.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@Profile("!prod")
public class LocalStorageService implements StorageService {

    @Value("${storage.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir, "images");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(storedFileName);
        Files.write(filePath, file.getBytes());

        String url = baseUrl + "/images/" + storedFileName;
        log.info("File saved locally: {}", filePath.toAbsolutePath());
        return url;
    }

    @Override
    public void deleteFile(String storedUrl) {
        try {
            String prefix = baseUrl + "/images/";
            if (storedUrl.startsWith(prefix)) {
                String fileName = storedUrl.substring(prefix.length());
                Path uploadBase = Paths.get(uploadDir, "images").toAbsolutePath().normalize();
                Path filePath = uploadBase.resolve(fileName).normalize();
                // 경로 탐색 공격 방지: 파일 경로가 업로드 디렉토리 내에 있는지 확인
                if (!filePath.startsWith(uploadBase)) {
                    log.warn("Attempted path traversal in deleteFile: {}", storedUrl);
                    return;
                }
                Files.deleteIfExists(filePath);
                log.info("Local file deleted: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete local file: {}", storedUrl, e);
        }
    }
}
