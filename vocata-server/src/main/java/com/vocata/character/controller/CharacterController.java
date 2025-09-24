package com.vocata.character.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.character.dto.request.CharacterCreateRequest;
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
import com.vocata.common.utils.UserContext;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理接口 - 客户端API
 * 路径前缀: /api/client/character
 */
@RestController
@RequestMapping("/client/character")
public class CharacterController {

    @Autowired
    private CharacterService characterService;

    /**
     * 获取公开角色列表
     * GET /api/client/character/public
     */
    @GetMapping("/public")
    public ApiResponse<PageResult<CharacterResponse>> getPublicCharacters(CharacterSearchRequest request) {
        Page<Character> page = new Page<>(request.getPageNum(), request.getPageSize());

        IPage<Character> result = characterService.getPublicCharacters(
                page,
                CharacterStatus.PUBLISHED, // 只返回已发布的
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
     * 搜索角色
     * GET /api/client/character/search?keyword=xxx
     */
    @GetMapping("/search")
    public ApiResponse<PageResult<CharacterResponse>> searchCharacters(CharacterSearchRequest request) {
        Page<Character> page = new Page<>(request.getPageNum(), request.getPageSize());

        IPage<Character> result = characterService.searchCharacters(
                page,
                request.getKeyword(),
                CharacterStatus.PUBLISHED // 只搜索已发布的
        );

        PageResult<CharacterResponse> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));

        return ApiResponse.success(pageResult);
    }

    /**
     * 根据角色编码获取角色详情
     * GET /api/client/character/{characterCode}
     */
    @GetMapping("/{characterCode}")
    public ApiResponse<CharacterDetailResponse> getCharacterByCode(@PathVariable String characterCode) {
        Character character = characterService.getByCharacterCode(characterCode);
        if (character == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND, "角色不存在");
        }

        CharacterDetailResponse response = convertToDetailResponse(character);
        return ApiResponse.success(response);
    }

    /**
     * 获取热门角色列表
     * GET /api/client/character/trending
     */
    @GetMapping("/trending")
    public ApiResponse<List<CharacterResponse>> getTrendingCharacters(@RequestParam(defaultValue = "10") int limit) {
        List<Character> characters = characterService.getTrendingCharacters(limit);
        List<CharacterResponse> responses = characters.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    /**
     * 获取精选角色列表
     * GET /api/client/character/featured
     */
    @GetMapping("/featured")
    public ApiResponse<List<CharacterResponse>> getFeaturedCharacters(@RequestParam(defaultValue = "10") int limit) {
        List<Character> characters = characterService.getFeaturedCharacters(limit);
        List<CharacterResponse> responses = characters.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    /**
     * 创建角色（需要登录）
     * POST /api/client/character
     */
    @PostMapping
    @SaCheckLogin
    public ApiResponse<CharacterDetailResponse> createCharacter(@Valid @RequestBody CharacterCreateRequest request) {
        Character character = new Character();
        BeanUtils.copyProperties(request, character);

        Character created = characterService.create(character);
        CharacterDetailResponse response = convertToDetailResponse(created);

        return ApiResponse.success("角色创建成功", response);
    }

    /**
     * 更新角色（需要登录且有权限）
     * PUT /api/client/character/{id}
     */
    @PutMapping("/{id}")
    @SaCheckLogin
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
     * 删除角色（需要登录且有权限）
     * DELETE /api/client/character/{id}
     */
    @DeleteMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<Void> deleteCharacter(@PathVariable Long id) {
        characterService.delete(id);
        return ApiResponse.success("角色删除成功");
    }

    /**
     * 获取我创建的角色列表（需要登录）
     * GET /api/client/character/my
     */
    @GetMapping("/my")
    @SaCheckLogin
    public ApiResponse<PageResult<CharacterResponse>> getMyCharacters(CharacterSearchRequest request) {
        Long currentUserId = UserContext.getUserId();
        Page<Character> page = new Page<>(request.getPageNum(), request.getPageSize());

        IPage<Character> result = characterService.getCharactersByCreator(
                page,
                currentUserId,
                request.getStatus()
        );

        PageResult<CharacterResponse> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));

        return ApiResponse.success(pageResult);
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