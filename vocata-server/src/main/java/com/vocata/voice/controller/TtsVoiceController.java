package com.vocata.voice.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.vocata.common.result.ApiResponse;
import com.vocata.voice.dto.TtsVoiceListResponse;
import com.vocata.voice.service.TtsVoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TTS音色客户端控制器
 * 需要登录才能访问
 */
@RestController
@RequestMapping("/api/client/tts-voice")
@SaCheckLogin
public class TtsVoiceController {

    @Autowired
    private TtsVoiceService ttsVoiceService;

    /**
     * 获取音色简化列表（仅包含id和name）
     * 客户端使用，需要登录
     */
    @GetMapping("/list")
    public ApiResponse<List<TtsVoiceListResponse>> getVoiceList() {
        List<TtsVoiceListResponse> voices = ttsVoiceService.getVoiceList();
        return ApiResponse.success("获取音色列表成功", voices);
    }
}