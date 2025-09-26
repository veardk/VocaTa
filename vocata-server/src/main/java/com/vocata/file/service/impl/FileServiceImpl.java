package com.vocata.file.service.impl;

import cn.hutool.core.util.IdUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.file.config.QiniuConfig;
import com.vocata.file.dto.FileUploadResponse;
import com.vocata.file.service.FileService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 文件服务实现
 *
 * @author vocata
 * @since 2025-09-24
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final Auth auth;
    private final UploadManager uploadManager;
    private final BucketManager bucketManager;
    private final QiniuConfig qiniuConfig;

    public FileServiceImpl(Auth auth, UploadManager uploadManager,
                          BucketManager bucketManager, QiniuConfig qiniuConfig) {
        this.auth = auth;
        this.uploadManager = uploadManager;
        this.bucketManager = bucketManager;
        this.qiniuConfig = qiniuConfig;
    }

    /**
     * 允许的图片类型
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 文件大小限制（5MB）
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String fileType) {
        // 验证文件
        validateFile(file);

        try {
            // 生成文件名
            String fileName = generateFileName(file, fileType);

            // 获取上传token
            String uploadToken = auth.uploadToken(qiniuConfig.getBucket());

            // 上传文件
            Response response = uploadManager.put(file.getBytes(), fileName, uploadToken);

            if (!response.isOK()) {
                log.error("文件上传失败，响应: {}", response.toString());
                throw new BizException(ApiCode.FILE_UPLOAD_FAILED);
            }

            // 构建文件访问URL
            String fileUrl = qiniuConfig.getDomain() + "/" + fileName;

            return FileUploadResponse.builder()
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .fileUrl(fileUrl)
                    .uploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();

        } catch (Exception e) {
            log.error("文件上传异常", e);
            throw new BizException(ApiCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public FileUploadResponse uploadAudioFile(byte[] audioData, String fileName, String fileType, String contentType) {
        // 验证音频数据
        validateAudioFile(audioData, fileName, contentType);

        try {
            // 生成唯一文件名
            String uniqueFileName = generateAudioFileName(fileName, fileType);

            // 获取上传token
            String uploadToken = auth.uploadToken(qiniuConfig.getBucket());

            // 上传音频数据
            Response response = uploadManager.put(audioData, uniqueFileName, uploadToken);

            if (!response.isOK()) {
                log.error("音频文件上传失败，响应: {}", response.toString());
                throw new BizException(ApiCode.FILE_UPLOAD_FAILED);
            }

            // 构建文件访问URL
            String fileUrl = qiniuConfig.getDomain() + "/" + uniqueFileName;

            log.info("音频文件上传成功: {}, 访问 URL: {}", uniqueFileName, fileUrl);

            return FileUploadResponse.builder()
                    .fileName(uniqueFileName)
                    .originalFileName(fileName)
                    .fileSize((long) audioData.length)
                    .contentType(contentType)
                    .fileUrl(fileUrl)
                    .uploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();

        } catch (Exception e) {
            log.error("音频文件上传异常: {}", fileName, e);
            throw new BizException(ApiCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            Response response = bucketManager.delete(qiniuConfig.getBucket(), fileName);
            return response.isOK();
        } catch (QiniuException e) {
            log.error("文件删除失败: {}", fileName, e);
            return false;
        }
    }

    /**
     * 用于模块化要求，将文件验证和名称生成逻辑抽离
     */

    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BizException(ApiCode.FILE_EMPTY);
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ApiCode.FILE_SIZE_EXCEEDED);
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType) || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BizException(ApiCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    /**
     * 验证音频文件
     */
    private void validateAudioFile(byte[] audioData, String fileName, String contentType) {
        // 检查数据是否为空
        if (audioData == null || audioData.length == 0) {
            throw new BizException(ApiCode.FILE_EMPTY);
        }

        // 检查文件大小 (10MB 限制，音频文件通常较大)
        if (audioData.length > 10 * 1024 * 1024) {
            throw new BizException(ApiCode.FILE_SIZE_EXCEEDED);
        }

        // 检查文件名
        if (StringUtils.isBlank(fileName)) {
            throw new BizException(ApiCode.FILE_EMPTY);
        }

        // 检查内容类型 (支持常见音频格式)
        if (StringUtils.isBlank(contentType)) {
            // 如果没有提供contentType，根据文件扩展名推断
            String extension = getFileExtension(fileName).toLowerCase();
            if (!isAllowedAudioExtension(extension)) {
                throw new BizException(ApiCode.FILE_TYPE_NOT_ALLOWED);
            }
        }
    }

    /**
     * 检查是否为允许的音频扩展名
     */
    private boolean isAllowedAudioExtension(String extension) {
        List<String> allowedExtensions = Arrays.asList(".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac");
        return allowedExtensions.contains(extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }
        return "";
    }

    /**
     * 生成文件名
     */
    private String generateFileName(MultipartFile file, String fileType) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            originalFilename = "unknown";
        }

        // 获取文件扩展名
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex);
        }

        // 生成唯一文件名: fileType/yyyyMMdd/uuid.ext
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = IdUtil.simpleUUID();

        return String.format("%s/%s/%s%s",
                StringUtils.isNotBlank(fileType) ? fileType : "common",
                dateStr,
                uuid,
                extension);
    }

    /**
     * 生成音频文件名
     */
    private String generateAudioFileName(String originalFileName, String fileType) {
        // 获取文件扩展名
        String extension = getFileExtension(originalFileName);
        if (StringUtils.isBlank(extension)) {
            extension = ".mp3"; // 默认扩展名
        }

        // 生成唯一文件名: fileType/yyyyMMdd/uuid.ext
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = IdUtil.simpleUUID();

        return String.format("%s/%s/%s%s",
                StringUtils.isNotBlank(fileType) ? fileType : "audio",
                dateStr,
                uuid,
                extension);
    }
}