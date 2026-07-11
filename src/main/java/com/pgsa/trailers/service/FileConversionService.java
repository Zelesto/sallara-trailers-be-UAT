// src/main/java/com/pgsa/trailers/service/FileConversionService.java
package com.pgsa.trailers.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileConversionService {

    private static final Set<String> IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    ));

    private static final Set<String> PDF_TYPES = new HashSet<>(Arrays.asList(
            "application/pdf"
    ));

    private static final Set<String> DOC_TYPES = new HashSet<>(Arrays.asList(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ));

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    /**
     * Convert any supported file to PDF bytes
     */
    public byte[] convertToPdf(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        log.info("Converting file: {}, type: {}, extension: {}", originalFilename, contentType, extension);

        // If already PDF, return as is
        if (isPdf(contentType, extension)) {
            log.info("File is already PDF, returning as is");
            return file.getBytes();
        }

        // Convert images to PDF
        if (isImage(contentType, extension)) {
            log.info("Converting image to PDF");
            return convertImageToPdf(file.getBytes(), extension);
        }

        // Handle DOC/DOCX
        if (isDoc(contentType, extension)) {
            log.info("Converting Word document to PDF");
            return convertDocToPdf(file);
        }

        // Unsupported format - convert to PDF with metadata
        log.warn("Unsupported file format: {}, converting with metadata", contentType);
        return convertToPdfWithMetadata(file);
    }

    /**
     * Convert image bytes to PDF
     */
    private byte[] convertImageToPdf(byte[] imageBytes, String extension) throws IOException {
        // Save image to temp file
        Path tempFile = null;
        try {
            // Create temp file with proper extension
            String ext = extension != null ? extension : "jpg";
            tempFile = Files.createTempFile("image_", "." + ext);
            Files.write(tempFile, imageBytes);
            
            try (PDDocument document = new PDDocument()) {
                // Load image from file
                PDImageXObject pdImage = PDImageXObject.createFromFile(tempFile.toString(), document);
                
                // Create page with image dimensions
                float width = pdImage.getWidth();
                float height = pdImage.getHeight();
                PDRectangle pageSize = new PDRectangle(width, height);
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, width, height);
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                document.save(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("Error converting image to PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to convert image to PDF", e);
        } finally {
            // Clean up temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * Convert DOC/DOCX to PDF (simplified version)
     */
    private byte[] convertDocToPdf(MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        String filename = file.getOriginalFilename();
        String fileSize = formatFileSize(content.length);
        
        // Create a simple PDF with file info
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Document: " + (filename != null ? filename : "Unknown"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("File Size: " + fileSize);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("This document was converted from: " + 
                        (file.getContentType() != null ? file.getContentType() : "Unknown"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Please note: The original document format is not natively supported.");
                contentStream.endText();
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error converting document to PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to convert document to PDF", e);
        }
    }

    /**
     * Convert any file to PDF with metadata
     */
    private byte[] convertToPdfWithMetadata(MultipartFile file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("File: " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "Unknown"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Type: " + (file.getContentType() != null ? file.getContentType() : "Unknown"));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Size: " + formatFileSize(file.getSize()));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("This is a converted PDF preview.");
                contentStream.endText();
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Check if file is an image
     */
    private boolean isImage(String contentType, String extension) {
        if (contentType != null && IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return true;
        }
        if (extension != null) {
            String ext = extension.toLowerCase();
            return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || 
                   ext.equals("gif") || ext.equals("bmp") || ext.equals("webp");
        }
        return false;
    }

    /**
     * Check if file is PDF
     */
    private boolean isPdf(String contentType, String extension) {
        if (contentType != null && contentType.toLowerCase().contains("pdf")) {
            return true;
        }
        if (extension != null) {
            return extension.toLowerCase().equals("pdf");
        }
        return false;
    }

    /**
     * Check if file is Word document
     */
    private boolean isDoc(String contentType, String extension) {
        if (contentType != null && DOC_TYPES.contains(contentType.toLowerCase())) {
            return true;
        }
        if (extension != null) {
            String ext = extension.toLowerCase();
            return ext.equals("doc") || ext.equals("docx");
        }
        return false;
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) {
            return null;
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * Format file size
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Get content type from file extension
     */
    public String getContentTypeFromExtension(String filename) {
        String extension = getFileExtension(filename);
        if (extension == null) {
            return "application/pdf";
        }
        
        switch (extension.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default:
                return "application/octet-stream";
        }
    }
}
