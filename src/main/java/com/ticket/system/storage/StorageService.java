package com.ticket.system.storage;

import java.io.InputStream;
import java.time.Duration;

public interface StorageService {

    void upload(String objectKey, InputStream inputStream, long contentLength, String contentType);

    void delete(String objectKey);

    String generateDownloadUrl(String objectKey, String originalFilename, Duration expiration);
}
