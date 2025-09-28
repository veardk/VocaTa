package com.vocata.file.service;

import com.vocata.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * @author vocata
 * @since 2025-09-24
 */
public interface FileService {

    /**
     * 上传文件到七牛云
     *
     * @param file 上传的文件
     * @param fileType 文件类型（用于目录分类）
     * @return 上传结果
     */
    FileUploadResponse uploadFile(MultipartFile file, String fileType);

    /**
     * 上传字节数组到七牛云 - 用于音频文件上传
     *
     * @param audioData 音频字节数据
     * @param fileName 原始文件名（包含扩展名）
     * @param fileType 文件类型（用于目录分类）
     * @param contentType 文件MIME类型
     * @return 上传结果
     */
    FileUploadResponse uploadAudioFile(byte[] audioData, String fileName, String fileType, String contentType);

    /**
     * 删除七牛云文件
     *
     * @param fileName 文件名
     * @return 是否成功
     */
    boolean deleteFile(String fileName);
}