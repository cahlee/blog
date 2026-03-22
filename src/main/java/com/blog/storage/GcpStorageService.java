package com.blog.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class GcpStorageService implements StorageService {

    private final Storage storage;

    @Value("${gcp.storage.bucket-name:my-blog-bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = "images/" + UUID.randomUUID() + extension;

        BlobId blobId = BlobId.of(bucketName, storedFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        String url = "https://storage.googleapis.com/" + bucketName + "/" + storedFileName;
        log.info("File uploaded to GCP Storage: {}", url);
        return url;
    }

    public void deleteFile(String storedUrl) {
        try {
            String prefix = "https://storage.googleapis.com/" + bucketName + "/";
            if (storedUrl.startsWith(prefix)) {
                String objectName = storedUrl.substring(prefix.length());
                BlobId blobId = BlobId.of(bucketName, objectName);
                storage.delete(blobId);
                log.info("File deleted from GCP Storage: {}", storedUrl);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from GCP Storage: {}", storedUrl, e);
        }
    }
}
