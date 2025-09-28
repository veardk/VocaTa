package com.vocata.character.service;

import com.vocata.character.dto.request.CharacterAiGenerateRequest;
import com.vocata.character.dto.response.CharacterAiGenerateResponse;

/**
 * AI角色生成服务接口
 */
public interface CharacterAiGenerateService {

    /**
     * 使用AI生成角色详细设定
     *
     * @param request 角色生成请求，包含基本角色信息
     * @return 生成的角色详细设定响应
     */
    CharacterAiGenerateResponse generateCharacter(CharacterAiGenerateRequest request);
}