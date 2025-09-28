package com.vocata.user.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vocata.common.config.LongDeserializer;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量检查收藏状态请求DTO
 */
public class BatchCheckFavoriteRequest {

    /**
     * 角色ID列表
     */
    @NotEmpty(message = "角色ID列表不能为空")
    @JsonDeserialize(contentUsing = LongDeserializer.class)
    private List<Long> characterIds;

    public List<Long> getCharacterIds() {
        return characterIds;
    }

    public void setCharacterIds(List<Long> characterIds) {
        this.characterIds = characterIds;
    }
}