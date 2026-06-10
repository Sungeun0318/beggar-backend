package com.beggar.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final software.amazon.awssdk.services.s3.S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3-image-bucket}")
    private String imageBucket;

    public byte[] getFileBytes(String key) {
        try {
            return s3Client.getObject(builder -> builder.bucket(bucket).key(key).build())
                    .readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("S3 파일 읽기 실패: " + key, e);
        }
    }

    public String generatePresignedUrl(String fileName) {
        return generatePresignedUrl(bucket, fileName);
    }

    public String generateProfilePresignedUrl(String fileName) {
        return generatePresignedUrl(imageBucket, fileName);
    }

    private String generatePresignedUrl(String targetBucket, String fileName) {
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(targetBucket)
                .key(uniqueFileName)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public String generatePresignedGetUrl(String rawUrl) {
        return generatePresignedGetUrl(bucket, rawUrl);
    }

    public String generateProfilePresignedGetUrl(String rawUrl) {
        return generatePresignedGetUrl(imageBucket, rawUrl);
    }

    private String generatePresignedGetUrl(String targetBucket, String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return rawUrl;
        
        try {
            String encodedKey = rawUrl.substring(rawUrl.lastIndexOf("/") + 1);
            String key = java.net.URLDecoder.decode(encodedKey, java.nio.charset.StandardCharsets.UTF_8);

            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = 
                software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(targetBucket)
                    .key(key)
                    .build();

            software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest = 
                software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30)) // 30분 유효
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            return rawUrl;
        }
    }
}
