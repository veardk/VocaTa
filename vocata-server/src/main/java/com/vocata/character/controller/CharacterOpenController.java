package com.vocata.character.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.character.dto.request.CharacterSearchRequest;
import com.vocata.character.dto.response.CharacterDetailResponse;
import com.vocata.character.dto.response.CharacterResponse;
import com.vocata.character.entity.Character;
import com.vocata.character.service.CharacterService;
import com.vocata.common.constant.CharacterStatus;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.result.ApiResponse;
import com.vocata.common.result.PageResult;
import com.vocata.user.service.UserFavoriteService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色公开接口 - 无需认证的公开API
 * 路径前缀: /api/open/character
 */
@RestController
@RequestMapping("/api/open/character")
public class CharacterOpenController {

    @Autowired
    private CharacterService characterService;

    @Autowired
    private UserFavoriteService userFavoriteService;

    /**
     * 获取公开角色列表
     * GET /api/open/character/list 或 /api/open/character/public
     */
    @GetMapping({"/list", "/public"})
    public ApiResponse<PageResult<CharacterResponse>> getPublicCharacters(CharacterSearchRequest request) {
        // 防止空指针异常，设置默认值
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        Page<Character> page = new Page<>(pageNum, pageSize);

        IPage<Character> result = characterService.getPublicCharacters(
                page,
                CharacterStatus.PUBLISHED, // 只返回已发布的
                request.getIsFeatured(),
                request.getTags(),
                request.getOrderBy(),
                request.getOrderDirection()
        );

        List<CharacterResponse> responseList = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        PageResult<CharacterResponse> pageResult = PageResult.of(
                pageNum,
                pageSize,
                result.getTotal(),
                responseList
        );

        return ApiResponse.success(pageResult);
    }

    /**
     * 搜索角色
     * GET /api/open/character/search?keyword=xxx
     */
    @GetMapping("/search")
    public ApiResponse<PageResult<CharacterResponse>> searchCharacters(CharacterSearchRequest request) {
        // 防止空指针异常，设置默认值
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        Page<Character> page = new Page<>(pageNum, pageSize);

        IPage<Character> result = characterService.searchCharacters(
                page,
                request.getKeyword(),
                CharacterStatus.PUBLISHED // 只搜索已发布的
        );

        List<CharacterResponse> responseList = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        PageResult<CharacterResponse> pageResult = PageResult.of(
                pageNum,
                pageSize,
                result.getTotal(),
                responseList
        );

        return ApiResponse.success(pageResult);
    }

    /**
     * 获取热门角色列表
     * GET /api/open/character/trending
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
     * GET /api/open/character/featured
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
     * 获取角色收藏数排行榜（公开接口）
     * GET /api/open/character/favorite-ranking
     */
    @GetMapping("/favorite-ranking")
    public ApiResponse<List<Map<String, Object>>> getFavoriteRanking(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        List<Map<String, Object>> result = userFavoriteService.getFavoriteRanking(limit);
        return ApiResponse.success(result);
    }

    /**
     * 根据角色编码或ID获取角色详情
     * GET /api/open/character/{characterCodeOrId}
     * 支持传入角色编码(string)或角色ID(数字)
     * 注意：此映射必须放在最后，避免拦截其他具体路径
     */
    @GetMapping("/{characterCodeOrId}")
    public ApiResponse<CharacterDetailResponse> getCharacterByCodeOrId(@PathVariable String characterCodeOrId) {
        Character character = null;

        // 首先尝试按ID查找（如果传入的是数字）
        try {
            Long id = Long.parseLong(characterCodeOrId);
            character = characterService.getById(id);
            // 确保角色已发布
            if (character != null && character.getStatus() != CharacterStatus.PUBLISHED) {
                character = null;
            }
        } catch (NumberFormatException e) {
            // 不是数字，忽略异常继续按编码查找
        }

        // 如果按ID未找到，尝试按角色编码查找
        if (character == null) {
            character = characterService.getByCharacterCode(characterCodeOrId);
        }

        if (character == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND, "角色不存在");
        }

        CharacterDetailResponse response = convertToDetailResponse(character);
        return ApiResponse.success(response);
    }

    /**
     * 将Character实体转换为CharacterResponse
     */
    private CharacterResponse convertToResponse(Character character) {
        CharacterResponse response = new CharacterResponse();
        BeanUtils.copyProperties(character, response);
        response.setStatusName(CharacterStatus.getStatusName(character.getStatus()));
        response.setCreatedAt(character.getCreateDate());
        response.setUpdatedAt(character.getUpdateDate());
        return response;
    }

    /**
     * 将Character实体转换为CharacterDetailResponse
     */
    private CharacterDetailResponse convertToDetailResponse(Character character) {
        CharacterDetailResponse response = new CharacterDetailResponse();
        BeanUtils.copyProperties(character, response);
        response.setStatusName(CharacterStatus.getStatusName(character.getStatus()));
        response.setCreatedAt(character.getCreateDate());
        response.setUpdatedAt(character.getUpdateDate());
        return response;
    }
}