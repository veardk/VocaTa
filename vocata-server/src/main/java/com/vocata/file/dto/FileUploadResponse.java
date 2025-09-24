package com.vocata.file.dto;

/**
 * 文件上传响应
 *
 * @author vocata
 * @since 2025-09-24
 */
public class FileUploadResponse {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 上传时间
     */
    private String uploadTime;

    public FileUploadResponse() {
    }

    public FileUploadResponse(String fileName, String originalFileName, Long fileSize,
                            String contentType, String fileUrl, String uploadTime) {
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.fileUrl = fileUrl;
        this.uploadTime = uploadTime;
    }

    public static FileUploadResponseBuilder builder() {
        return new FileUploadResponseBuilder();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public static class FileUploadResponseBuilder {
        private String fileName;
        private String originalFileName;
        private Long fileSize;
        private String contentType;
        private String fileUrl;
        private String uploadTime;

        public FileUploadResponseBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public FileUploadResponseBuilder originalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
            return this;
        }

        public FileUploadResponseBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public FileUploadResponseBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public FileUploadResponseBuilder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }

        public FileUploadResponseBuilder uploadTime(String uploadTime) {
            this.uploadTime = uploadTime;
            return this;
        }

        public FileUploadResponse build() {
            return new FileUploadResponse(fileName, originalFileName, fileSize,
                                        contentType, fileUrl, uploadTime);
        }
    }
}