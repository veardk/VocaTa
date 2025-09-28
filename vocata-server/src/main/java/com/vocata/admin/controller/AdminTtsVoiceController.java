package com.vocata.admin.controller;

import com.vocata.common.result.ApiResponse;
import com.vocata.voice.dto.TtsVoiceAddRequest;
import com.vocata.voice.dto.TtsVoiceResponse;
import com.vocata.voice.dto.TtsVoiceUpdateRequest;
import com.vocata.voice.dto.TtsVoiceListResponse;
import com.vocata.voice.service.TtsVoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 管理后台TTS音色管理控制器
 */
@RestController
@RequestMapping("/api/admin/tts-voice")
@Validated
public class AdminTtsVoiceController {

    @Autowired
    private TtsVoiceService ttsVoiceService;

    /**
     * 添加音色
     */
    @PostMapping
    public ApiResponse<TtsVoiceResponse> addVoice(@Valid @RequestBody TtsVoiceAddRequest request) {
        TtsVoiceResponse response = ttsVoiceService.addVoice(request);
        return ApiResponse.success("添加音色成功", response);
    }

    /**
     * 删除音色
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteVoice(@PathVariable Long id) {
        ttsVoiceService.deleteVoice(id);
        return ApiResponse.success("删除音色成功", true);
    }

    /**
     * 更新音色
     */
    @PutMapping("/{id}")
    public ApiResponse<TtsVoiceResponse> updateVoice(@PathVariable Long id,
                                                   @Valid @RequestBody TtsVoiceUpdateRequest request) {
        TtsVoiceResponse response = ttsVoiceService.updateVoice(id, request);
        return ApiResponse.success("更新音色成功", response);
    }

    /**
     * 获取音色完整列表（管理后台使用）
     */
    @GetMapping("/list")
    public ApiResponse<List<TtsVoiceResponse>> getFullVoiceList() {
        List<TtsVoiceResponse> voices = ttsVoiceService.getFullVoiceList();
        return ApiResponse.success("获取音色列表成功", voices);
    }
}