// src/main/java/com/pgsa/trailers/service/SupabaseStorageService.java
package com.pgsa.trailers.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${supabase.s3.bucket}")
    private String bucketName;

    @Value("${supabase.url}")
    private String supabaseUrl;

    /**
     * Upload a file to Supabase Storage
     * @param file The file to upload
     * @param podNumber The POD number for folder organization
     * @return The public URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String podNumber) {
        try {
            // Generate a unique file path
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString();
            String filePath = String.format("%s/%s.%s", 
                    podNumber, 
                    fileName,
                    extension);
            
            log.info("Uploading file to Supabase: {}/{}", bucketName, filePath);
            
            // Build the upload request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            // Upload the file
            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            
            log.info("File uploaded successfully. ETag: {}", response.eTag());
            
            // Return the public URL
            String fileUrl = String.format("%s/storage/v1/object/public/%s/%s", 
                    supabaseUrl, bucketName, filePath);
            
            log.info("File URL: {}", fileUrl);
            return fileUrl;
            
        } catch (IOException e) {
            log.error("Failed to upload file to Supabase", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("Supabase S3 error: {}", e.getMessage(), e);
            throw new RuntimeException("Supabase upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a signed URL for private files (expires in 1 hour)
     */
    public String generateSignedUrl(String fileUrl) {
        try {
            // Extract file path from URL
            String filePath = extractFilePath(fileUrl);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String signedUrl = presignedRequest.url().toString();
            
            log.info("Generated signed URL for: {}", filePath);
            return signedUrl;
            
        } catch (S3Exception e) {
            log.error("Failed to generate signed URL", e);
            throw new RuntimeException("Failed to generate signed URL: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Supabase Storage
     */
    public void deleteFile(String fileUrl) {
        try {
            String filePath = extractFilePath(fileUrl);
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            DeleteObjectResponse response = s3Client.deleteObject(deleteRequest);
            log.info("File deleted: {}, Delete marker: {}", filePath, response.deleteMarker());
            
        } catch (S3Exception e) {
            log.error("Failed to delete file", e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    /**
 * Check if a file exists in Supabase Storage
 */
public boolean fileExists(String fileUrl) {
    try {
        String filePath = extractFilePath(fileUrl);
        
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .build();
        
        s3Client.headObject(headRequest);
        return true;
        
    } catch (NoSuchKeyException e) {
        return false;
    } catch (S3Exception e) {
        log.error("Failed to check if file exists: {}", e.getMessage(), e);
        return false;
    }
}
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "pdf";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Extract file path from URL
     * URL format: https://project-id.supabase.co/storage/v1/object/public/bucket/path/to/file
     */
    private String extractFilePath(String fileUrl) {
        String prefix = "/object/public/" + bucketName + "/";
        int startIndex = fileUrl.indexOf(prefix);
        if (startIndex != -1) {
            return fileUrl.substring(startIndex + prefix.length());
        }
        
        // If URL is already just the path
        if (!fileUrl.contains("/")) {
            return fileUrl;
        }
        
        // Fallback: try to extract from URL
        try {
            java.net.URL url = new java.net.URL(fileUrl);
            String path = url.getPath();
            int bucketIndex = path.indexOf(bucketName);
            if (bucketIndex != -1) {
                String fullPath = path.substring(bucketIndex + bucketName.length() + 1);
                return fullPath;
            }
        } catch (Exception e) {
            log.warn("Failed to extract file path from URL: {}", fileUrl);
        }
        
        return fileUrl;
    }
}
