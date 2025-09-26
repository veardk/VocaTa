package com.vocata.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.vocata.common.result.ApiResponse;
import com.vocata.common.result.PageResult;
import com.vocata.common.utils.UserContext;
import com.vocata.file.dto.FileUploadResponse;
import com.vocata.file.service.FileService;
import com.vocata.user.dto.UpdateUserProfileRequest;
import com.vocata.user.dto.UserProfileResponse;
import com.vocata.user.dto.request.BatchCheckFavoriteRequest;
import com.vocata.user.dto.request.FavoriteRequest;
import com.vocata.user.dto.response.FavoriteResponse;
import com.vocata.user.service.UserService;
import com.vocata.user.service.UserFavoriteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户个人信息控制器
 *
 * @author vocata
 * @since 2025-09-24
 */
@RestController
@RequestMapping("/api/client/user")
@SaCheckLogin
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final FileService fileService;

    @Autowired
    private UserFavoriteService userFavoriteService;

    public UserController(UserService userService, FileService fileService) {
        this.userService = userService;
        this.fileService = fileService;
    }

    /**
     * 获取当前用户个人信息
     *
     * @return 用户个人信息
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getCurrentUserProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ApiResponse.success(profile);
    }

    /**
     * 更新当前用户个人信息
     *
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateCurrentUserProfile(
            @RequestBody @Validated UpdateUserProfileRequest request) {

        log.info("用户更新个人信息: {}", request);

        UserProfileResponse profile = userService.updateCurrentUserProfile(request);
        return ApiResponse.success(profile);
    }

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 更新后的用户信息
     */
    @PostMapping("/avatar")
    public ApiResponse<UserProfileResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file) {

        log.info("用户上传头像: {}", file.getOriginalFilename());

        // 上传文件到七牛云
        FileUploadResponse uploadResponse = fileService.uploadFile(file, "avatar");

        // 更新用户头像
        UserProfileResponse profile = userService.updateUserAvatar(uploadResponse.getFileUrl());

        log.info("用户头像上传成功: {}", uploadResponse.getFileUrl());

        return ApiResponse.success(profile);
    }

    // ========== 收藏功能相关接口 ==========

    /**
     * 切换收藏状态（收藏/取消收藏）
     * 如果已收藏则取消收藏，如果未收藏则添加收藏
     */
    @PostMapping("/favorite/toggle")
    public ApiResponse<Map<String, Object>> toggleFavorite(@RequestBody @Valid FavoriteRequest request) {
        Long userId = UserContext.getUserId();
        Map<String, Object> result = userFavoriteService.toggleFavorite(userId, request.getCharacterId());
        return ApiResponse.success(result);
    }

    /**
     * 快速切换收藏状态（通过路径参数）
     * GET /api/client/user/favorite/toggle/{characterId}
     */
    @PostMapping("/favorite/toggle/{characterId}")
    public ApiResponse<Map<String, Object>> toggleFavoriteByPath(@PathVariable Long characterId) {
        Long userId = UserContext.getUserId();
        Map<String, Object> result = userFavoriteService.toggleFavorite(userId, characterId);
        return ApiResponse.success(result);
    }

    /**
     * 获取用户收藏的角色列表
     */
    @GetMapping("/favorites")
    public ApiResponse<PageResult<FavoriteResponse>> getUserFavorites(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getUserId();
        PageResult<FavoriteResponse> result = userFavoriteService.getUserFavorites(userId, pageNum, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 获取用户收藏角色数量
     */
    @GetMapping("/favorite/count")
    public ApiResponse<Integer> getUserFavoriteCount() {
        Long userId = UserContext.getUserId();
        Integer count = userFavoriteService.getUserFavoriteCount(userId);
        return ApiResponse.success(count);
    }

    /**
     * 批量检查收藏状态
     */
    @PostMapping("/favorite/batch-check")
    public ApiResponse<Map<String, Boolean>> batchCheckFavoriteStatus(@RequestBody @Valid BatchCheckFavoriteRequest request) {
        Long userId = UserContext.getUserId();
        Map<String, Boolean> result = userFavoriteService.batchCheckFavoriteStatus(userId, request.getCharacterIds());
        return ApiResponse.success(result);
    }

    /**
     * 检查单个角色的收藏状态
     */
    @GetMapping("/favorite/check/{characterId}")
    public ApiResponse<Boolean> checkFavoriteStatus(@PathVariable Long characterId) {
        Long userId = UserContext.getUserId();
        boolean result = userFavoriteService.isUserFavorite(userId, characterId);
        return ApiResponse.success(result);
    }
}