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

import jakarta.annotation.PostConstruct;
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

    @Value("${supabase.s3.endpoint}")
    private String endpoint;

    @Value("${supabase.s3.region}")
    private String region;

    /**
     * Test connection to Supabase bucket on startup
     */
    @PostConstruct
    public void testConnection() {
        log.info("========================================");
        log.info("🔌 Testing Supabase S3 Connection");
        log.info("========================================");
        log.info("Endpoint: {}", endpoint);
        log.info("Region: {}", region);
        log.info("Bucket: {}", bucketName);
        log.info("Supabase URL: {}", supabaseUrl);
        log.info("----------------------------------------");
        
        try {
            // Test 1: Check if bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            HeadBucketResponse response = s3Client.headBucket(headBucketRequest);
            log.info("✅ Bucket '{}' exists and is accessible", bucketName);
            log.info("   Response: {}", response);
            
            // Test 2: List objects in bucket (limit to 1 to verify access)
            ListObjectsRequest listRequest = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .maxKeys(1)
                    .build();
            
            ListObjectsResponse listResponse = s3Client.listObjects(listRequest);
            log.info("✅ Can list objects in bucket '{}'", bucketName);
            log.info("   Object count: {}", listResponse.contents().size());
            
            log.info("========================================");
            log.info("✅ Supabase Storage connection successful!");
            log.info("========================================");
            
        } catch (NoSuchBucketException e) {
            log.error("❌ Bucket '{}' does not exist or is not accessible", bucketName, e);
            log.error("   Please create the bucket in Supabase Storage");
            log.error("   Error: {}", e.getMessage());
        } catch (S3Exception e) {
            log.error("❌ Failed to connect to Supabase S3: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("   Error Message: {}", e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error connecting to Supabase: {}", e.getMessage(), e);
        }
    }


    public String uploadAndConvertFile(MultipartFile file, String podNumber, FileConversionService conversionService) {
    log.info("Processing and uploading file for POD: {}", podNumber);
    
    try {
        // Check if conversion is needed
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFilename);
        
        byte[] fileData;
        String finalFilename;
        String finalContentType;
        
        // If not PDF, convert to PDF
        if (!"pdf".equalsIgnoreCase(extension) && !"application/pdf".equalsIgnoreCase(contentType)) {
            log.info("Converting {} to PDF", originalFilename);
            fileData = conversionService.convertToPdf(file);
            finalFilename = podNumber + ".pdf";
            finalContentType = "application/pdf";
        } else {
            fileData = file.getBytes();
            finalFilename = podNumber + "." + extension;
            finalContentType = contentType != null ? contentType : "application/pdf";
        }
        
        // Upload the file
        String filePath = String.format("%s/%s", podNumber, finalFilename);
        
        log.info("Uploading file to Supabase: {}/{}", bucketName, filePath);
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .contentType(finalContentType)
                .contentLength((long) fileData.length)
                .build();
        
        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(fileData)
        );
        
        String fileUrl = String.format("%s/storage/v1/object/public/%s/%s", 
                supabaseUrl, bucketName, filePath);
        
        log.info("File uploaded successfully: {}", fileUrl);
        return fileUrl;
        
    } catch (Exception e) {
        log.error("Failed to upload file: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
    }
}
    
    /**
     * Upload a file to Supabase Storage with detailed logging
     */
    public String uploadFile(MultipartFile file, String podNumber) {
        log.info("========================================");
        log.info("📤 Starting file upload");
        log.info("========================================");
        log.info("Pod Number: {}", podNumber);
        log.info("File Name: {}", file.getOriginalFilename());
        log.info("File Size: {} bytes", file.getSize());
        log.info("File Content Type: {}", file.getContentType());
        
        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString();
            String filePath = String.format("%s/%s.%s", podNumber, fileName, extension);
            
            log.info("Generated file path: {}", filePath);
            log.info("Target bucket: {}", bucketName);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            log.info("Uploading to Supabase...");
            long startTime = System.currentTimeMillis();
            
            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            
            long endTime = System.currentTimeMillis();
            log.info("✅ File uploaded successfully in {} ms", (endTime - startTime));
            log.info("   ETag: {}", response.eTag());
            log.info("   Version ID: {}", response.versionId());
            
            String fileUrl = String.format("%s/storage/v1/object/public/%s/%s", 
                    supabaseUrl, bucketName, filePath);
            
            log.info("   File URL: {}", fileUrl);
            log.info("========================================");
            return fileUrl;
            
        } catch (IOException e) {
            log.error("❌ IO Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("❌ Supabase S3 Error: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("   Error Message: {}", e.awsErrorDetails().errorMessage());
            log.error("   Request ID: {}", e.requestId());
            throw new RuntimeException("Supabase upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a file exists in Supabase Storage with logging
     */
    public boolean fileExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.debug("File URL is null or empty");
            return false;
        }
        
        try {
            String filePath = extractFilePath(fileUrl);
            log.debug("Checking file existence: {}", filePath);
            
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            s3Client.headObject(headRequest);
            log.debug("✅ File exists: {}", filePath);
            return true;
            
        } catch (NoSuchKeyException e) {
            log.debug("❌ File does not exist: {}", fileUrl);
            return false;
        } catch (S3Exception e) {
            log.error("❌ Failed to check file existence: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error checking file existence: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate a signed URL for private files with logging
     */
    public String generateSignedUrl(String fileUrl) {
        try {
            String filePath = extractFilePath(fileUrl);
            log.debug("Generating signed URL for: {}", filePath);
            
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
            
            log.debug("✅ Signed URL generated: {}", signedUrl);
            return signedUrl;
            
        } catch (S3Exception e) {
            log.error("❌ Failed to generate signed URL: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            throw new RuntimeException("Failed to generate signed URL: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Supabase Storage with logging
     */
    public void deleteFile(String fileUrl) {
        try {
            String filePath = extractFilePath(fileUrl);
            log.info("Deleting file: {}", filePath);
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            DeleteObjectResponse response = s3Client.deleteObject(deleteRequest);
            log.info("✅ File deleted: {}", filePath);
            log.info("   Delete marker: {}", response.deleteMarker());
            
        } catch (S3Exception e) {
            log.error("❌ Failed to delete file: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
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
     */
    private String extractFilePath(String fileUrl) {
        String prefix = "/object/public/" + bucketName + "/";
        int startIndex = fileUrl.indexOf(prefix);
        if (startIndex != -1) {
            return fileUrl.substring(startIndex + prefix.length());
        }
        return fileUrl;
    }
}
