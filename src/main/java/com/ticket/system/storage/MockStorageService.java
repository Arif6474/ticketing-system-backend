package com.ticket.system.storage;

import com.ticket.system.exception.AppException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("test")
public class MockStorageService implements StorageService {

    private final Map<String, byte[]> storageMap = new ConcurrentHashMap<>();
    private boolean failUpload = false;
    private boolean failDelete = false;

    public void setFailUpload(boolean failUpload) {
        this.failUpload = failUpload;
    }

    public void setFailDelete(boolean failDelete) {
        this.failDelete = failDelete;
    }

    public boolean hasObject(String objectKey) {
        return storageMap.containsKey(objectKey);
    }

    public void clear() {
        storageMap.clear();
        failUpload = false;
        failDelete = false;
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        if (failUpload || objectKey.contains("simulated_upload_fail")) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated R2 upload failure");
        }
        try {
            byte[] bytes = inputStream.readAllBytes();
            storageMap.put(objectKey, bytes);
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read input stream: " + e.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        if (failDelete || objectKey.contains("simulated_delete_fail")) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated R2 delete failure");
        }
        storageMap.remove(objectKey);
    }

    @Override
    public String generateDownloadUrl(String objectKey, String originalFilename, Duration expiration) {
        if (!storageMap.containsKey(objectKey) && !objectKey.contains("issues/")) {
            throw new AppException(HttpStatus.NOT_FOUND, "Storage object not found");
        }
        return "https://mock-r2.storage.com/" + objectKey + "?expiration=" + expiration.getSeconds() + "&filename=" + originalFilename;
    }
}
