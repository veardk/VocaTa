package com.vocata.file.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.vocata.common.result.ApiResponse;
import com.vocata.file.dto.FileUploadResponse;
import com.vocata.file.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 *
 * @author vocata
 * @since 2025-09-24
 */
@RestController
@RequestMapping("/client/file")
@SaCheckLogin
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param type 文件类型分类（可选，用于目录分类，如: avatar, image等）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false, defaultValue = "common") String type) {

        log.info("开始上传文件: {}, 类型: {}", file.getOriginalFilename(), type);

        FileUploadResponse response = fileService.uploadFile(file, type);

        log.info("文件上传成功: {}", response.getFileUrl());

        return ApiResponse.success(response);
    }
}