package com.vocata.voice.service;

import com.vocata.voice.dto.TtsVoiceAddRequest;
import com.vocata.voice.dto.TtsVoiceResponse;
import com.vocata.voice.dto.TtsVoiceUpdateRequest;
import com.vocata.voice.dto.TtsVoiceListResponse;

import java.util.List;

/**
 * TTS音色服务接口
 */
public interface TtsVoiceService {

    /**
     * 添加音色
     */
    TtsVoiceResponse addVoice(TtsVoiceAddRequest request);

    /**
     * 删除音色
     */
    void deleteVoice(Long id);

    /**
     * 更新音色
     */
    TtsVoiceResponse updateVoice(Long id, TtsVoiceUpdateRequest request);

    /**
     * 获取音色列表（仅包含id和name）- 客户端使用
     */
    List<TtsVoiceListResponse> getVoiceList();

    /**
     * 获取音色完整列表 - 管理后台使用
     */
    List<TtsVoiceResponse> getFullVoiceList();

    /**
     * 根据音色名称获取服务商音色ID
     * 用于角色音色查询
     */
    String getProviderVoiceIdByName(String name);
}