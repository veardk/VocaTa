package com.vocata.voice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.vocata.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 音色配置表
 * 管理角色与TTS提供商音色的映射关系
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vocata_voice_profile")
public class VoiceProfile extends BaseEntity {

    /**
     * 音色配置ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 音色唯一标识符（业务用）
     */
    private String voiceId;

    /**
     * 音色名称
     */
    private String voiceName;

    /**
     * 音色描述
     */
    private String description;

    /**
     * 语言类型（zh-CN, en-US等）
     */
    private String language;

    /**
     * 性别（male, female, neutral）
     */
    private String gender;

    /**
     * TTS提供商类型（xunfei, volcan, azure等）
     */
    private String provider;

    /**
     * 提供商音色参数（如：x4_xiaoyan, aisjiuxu等）
     */
    private String providerVoiceId;

    /**
     * 配置参数（JSON格式存储语速、音量等）
     */
    private String configParams;

    /**
     * 是否启用（0-禁用，1-启用）
     */
    private Integer isEnabled;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 备注信息
     */
    private String remarks;
}