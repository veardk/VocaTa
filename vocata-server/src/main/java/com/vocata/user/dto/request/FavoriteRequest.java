package com.vocata.user.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vocata.common.config.LongDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * 收藏角色请求DTO
 */
public class FavoriteRequest {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    @JsonDeserialize(using = LongDeserializer.class)
    private Long characterId;

    public Long getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }
}