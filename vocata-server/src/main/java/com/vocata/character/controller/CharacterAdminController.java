package com.vocata.character.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.character.dto.request.CharacterSearchRequest;
import com.vocata.character.dto.request.CharacterUpdateRequest;
import com.vocata.character.dto.response.CharacterDetailResponse;
import com.vocata.character.dto.response.CharacterResponse;
import com.vocata.character.entity.Character;
import com.vocata.character.service.CharacterService;
import com.vocata.common.constant.CharacterStatus;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.result.ApiResponse;
import com.vocata.common.result.PageResult;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理接口 - 管理端API
 * 路径前缀: /api/admin/character
 * 需要管理员权限
 */
@RestController
@RequestMapping("/admin/character")
@SaCheckRole("admin")
public class CharacterAdminController {

    @Autowired
    private CharacterService characterService;

    /**
     * 获取所有角色列表（包括私有角色）
     * GET /api/admin/character
     */
    @GetMapping
    public ApiResponse<PageResult<CharacterResponse>> getAllCharacters(CharacterSearchRequest request) {
        Page<Character> page = new Page<>(request.getPageNum(), request.getPageSize());

        IPage<Character> result = characterService.getPublicCharacters(
                page,
                request.getStatus(),
                request.getIsFeatured(),
                request.getTags()
        );

        PageResult<CharacterResponse> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));

        return ApiResponse.success(pageResult);
    }

    /**
     * 根据ID获取角色详情
     * GET /api/admin/character/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<CharacterDetailResponse> getCharacterById(@PathVariable Long id) {
        Character character = characterService.getById(id);
        if (character == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND, "角色不存在");
        }

        CharacterDetailResponse response = convertToDetailResponse(character);
        return ApiResponse.success(response);
    }

    /**
     * 更新角色状态
     * PUT /api/admin/character/{id}/status
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateCharacterStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        if (!CharacterStatus.isValidStatus(status)) {
            throw new BizException(ApiCode.PARAM_ERROR, "无效的状态值");
        }

        characterService.updateStatus(id, status);
        String statusName = CharacterStatus.getStatusName(status);
        return ApiResponse.success("角色状态已更新为：" + statusName);
    }

    /**
     * 批量更新角色状态
     * PUT /api/admin/character/batch/status
     */
    @PutMapping("/batch/status")
    public ApiResponse<Void> batchUpdateStatus(
            @RequestBody List<Long> ids,
            @RequestParam Integer status) {

        if (!CharacterStatus.isValidStatus(status)) {
            throw new BizException(ApiCode.PARAM_ERROR, "无效的状态值");
        }

        int successCount = 0;
        for (Long id : ids) {
            if (characterService.updateStatus(id, status)) {
                successCount++;
            }
        }

        return ApiResponse.success(String.format("成功更新 %d/%d 个角色状态", successCount, ids.size()));
    }

    /**
     * 设置角色为精选
     * PUT /api/admin/character/{id}/featured
     */
    @PutMapping("/{id}/featured")
    public ApiResponse<Void> setFeatured(
            @PathVariable Long id,
            @RequestParam Integer isFeatured) {

        Character character = new Character();
        character.setId(id);
        character.setIsFeatured(isFeatured);

        Character updated = characterService.update(character);
        if (updated != null) {
            String message = isFeatured == 1 ? "已设置为精选角色" : "已取消精选";
            return ApiResponse.success(message);
        } else {
            return ApiResponse.error("操作失败");
        }
    }

    /**
     * 设置角色排序权重
     * PUT /api/admin/character/{id}/sort-weight
     */
    @PutMapping("/{id}/sort-weight")
    public ApiResponse<Void> setSortWeight(
            @PathVariable Long id,
            @RequestParam Integer sortWeight) {

        Character character = new Character();
        character.setId(id);
        character.setSortWeight(sortWeight);

        Character updated = characterService.update(character);
        if (updated != null) {
            return ApiResponse.success("排序权重已更新");
        } else {
            return ApiResponse.error("操作失败");
        }
    }

    /**
     * 获取审核中的角色列表
     * GET /api/admin/character/pending-review
     */
    @GetMapping("/pending-review")
    public ApiResponse<PageResult<CharacterResponse>> getPendingReviewCharacters(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Page<Character> page = new Page<>(pageNum, pageSize);

        IPage<Character> result = characterService.getPublicCharacters(
                page,
                CharacterStatus.UNDER_REVIEW,
                null,
                null
        );

        PageResult<CharacterResponse> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));

        return ApiResponse.success(pageResult);
    }

    /**
     * 搜索角色（管理员可搜索所有状态）
     * GET /api/admin/character/search
     */
    @GetMapping("/search")
    public ApiResponse<PageResult<CharacterResponse>> searchCharacters(CharacterSearchRequest request) {
        Page<Character> page = new Page<>(request.getPageNum(), request.getPageSize());

        IPage<Character> result = characterService.searchCharacters(
                page,
                request.getKeyword(),
                request.getStatus() // 管理员可以搜索任何状态的角色
        );

        PageResult<CharacterResponse> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));

        return ApiResponse.success(pageResult);
    }

    /**
     * 根据创建者查询角色
     * GET /api/admin/character/creator/{creatorId}
     */
    @GetMapping("/creator/{creatorId}")
    public ApiResponse<PageResult<CharacterResponse>> getCharactersByCreator(
            @PathVariable Long creatorId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {

        Page<Character> page = new Page<>(pageNum, pageSize);

        IPage<Character> result = characterService.getCharactersByCreator(page, creatorId, status);

        PageResult<CharacterResponse> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));

        return ApiResponse.success(pageResult);
    }

    /**
     * 管理员编辑角色
     * PUT /api/admin/character/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<CharacterDetailResponse> updateCharacter(
            @PathVariable Long id,
            @Valid @RequestBody CharacterUpdateRequest request) {

        Character character = new Character();
        BeanUtils.copyProperties(request, character);
        character.setId(id);

        Character updated = characterService.update(character);
        CharacterDetailResponse response = convertToDetailResponse(updated);

        return ApiResponse.success("角色更新成功", response);
    }

    /**
     * 管理员删除角色
     * DELETE /api/admin/character/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCharacter(@PathVariable Long id) {
        characterService.delete(id);
        return ApiResponse.success("角色删除成功");
    }

    /**
     * 将Character实体转换为CharacterResponse
     */
    private CharacterResponse convertToResponse(Character character) {
        CharacterResponse response = new CharacterResponse();
        BeanUtils.copyProperties(character, response);
        response.setStatusName(CharacterStatus.getStatusName(character.getStatus()));
        return response;
    }

    /**
     * 将Character实体转换为CharacterDetailResponse
     */
    private CharacterDetailResponse convertToDetailResponse(Character character) {
        CharacterDetailResponse response = new CharacterDetailResponse();
        BeanUtils.copyProperties(character, response);
        response.setStatusName(CharacterStatus.getStatusName(character.getStatus()));
        return response;
    }
}