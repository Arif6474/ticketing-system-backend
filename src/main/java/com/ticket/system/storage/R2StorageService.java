package com.ticket.system.storage;

import com.ticket.system.config.R2Properties;
import com.ticket.system.exception.AppException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
@Profile("!test")
public class R2StorageService implements StorageService {

    private final R2Properties r2Properties;

    public R2StorageService(R2Properties r2Properties) {
        this.r2Properties = r2Properties;
    }

    private S3Client buildS3Client() {
        if (r2Properties.getEndpoint() == null || r2Properties.getEndpoint().isBlank()) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "R2 storage endpoint is not configured");
        }
        return S3Client.builder()
                .endpointOverride(URI.create(r2Properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                r2Properties.getAccessKeyId() != null ? r2Properties.getAccessKeyId() : "dummy",
                                r2Properties.getSecretAccessKey() != null ? r2Properties.getSecretAccessKey() : "dummy"
                        )))
                .region(Region.of(r2Properties.getRegion() != null ? r2Properties.getRegion() : "auto"))
                .build();
    }

    private S3Presigner buildS3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(r2Properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                r2Properties.getAccessKeyId() != null ? r2Properties.getAccessKeyId() : "dummy",
                                r2Properties.getSecretAccessKey() != null ? r2Properties.getSecretAccessKey() : "dummy"
                        )))
                .region(Region.of(r2Properties.getRegion() != null ? r2Properties.getRegion() : "auto"))
                .build();
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try (S3Client s3 = buildS3Client()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to storage");
        }
    }

    @Override
    public void delete(String objectKey) {
        try (S3Client s3 = buildS3Client()) {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(objectKey)
                    .build();

            s3.deleteObject(deleteRequest);
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file from storage");
        }
    }

    @Override
    public String generateDownloadUrl(String objectKey, String originalFilename, Duration expiration) {
        try (S3Presigner presigner = buildS3Presigner()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(objectKey)
                    .responseContentDisposition("attachment; filename=\"" + originalFilename + "\"")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate presigned download URL");
        }
    }
}
