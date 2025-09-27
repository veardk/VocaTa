package com.vocata.voice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.voice.dto.TtsVoiceAddRequest;
import com.vocata.voice.dto.TtsVoiceResponse;
import com.vocata.voice.dto.TtsVoiceUpdateRequest;
import com.vocata.voice.dto.TtsVoiceListResponse;
import com.vocata.voice.entity.TtsVoice;
import com.vocata.voice.mapper.TtsVoiceMapper;
import com.vocata.voice.service.TtsVoiceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TTS音色服务实现
 */
@Service
public class TtsVoiceServiceImpl implements TtsVoiceService {

    private final TtsVoiceMapper ttsVoiceMapper;

    public TtsVoiceServiceImpl(TtsVoiceMapper ttsVoiceMapper) {
        this.ttsVoiceMapper = ttsVoiceMapper;
    }

    @Override
    public TtsVoiceResponse addVoice(TtsVoiceAddRequest request) {
        // 检查是否已存在相同的provider和providerVoiceId组合
        LambdaQueryWrapper<TtsVoice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TtsVoice::getProvider, request.getProvider())
                   .eq(TtsVoice::getProviderVoiceId, request.getProviderVoiceId());

        TtsVoice existingVoice = ttsVoiceMapper.selectOne(queryWrapper);
        if (existingVoice != null) {
            throw new BizException(ApiCode.PARAM_ERROR, "该服务商音色ID已存在");
        }

        // 创建新音色
        TtsVoice ttsVoice = new TtsVoice();
        BeanUtils.copyProperties(request, ttsVoice);

        ttsVoiceMapper.insert(ttsVoice);

        // 返回响应
        TtsVoiceResponse response = new TtsVoiceResponse();
        BeanUtils.copyProperties(ttsVoice, response);
        response.setId(ttsVoice.getId().toString());

        return response;
    }

    @Override
    public void deleteVoice(Long id) {
        TtsVoice ttsVoice = ttsVoiceMapper.selectById(id);
        if (ttsVoice == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND, "音色不存在");
        }

        ttsVoiceMapper.deleteById(id);
    }

    @Override
    public TtsVoiceResponse updateVoice(Long id, TtsVoiceUpdateRequest request) {
        TtsVoice ttsVoice = ttsVoiceMapper.selectById(id);
        if (ttsVoice == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND, "音色不存在");
        }

        // 检查是否存在相同的provider和providerVoiceId组合（排除当前记录）
        LambdaQueryWrapper<TtsVoice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TtsVoice::getProvider, request.getProvider())
                   .eq(TtsVoice::getProviderVoiceId, request.getProviderVoiceId())
                   .ne(TtsVoice::getId, id);

        TtsVoice existingVoice = ttsVoiceMapper.selectOne(queryWrapper);
        if (existingVoice != null) {
            throw new BizException(ApiCode.PARAM_ERROR, "该服务商音色ID已存在");
        }

        // 更新音色信息
        BeanUtils.copyProperties(request, ttsVoice);
        ttsVoiceMapper.updateById(ttsVoice);

        // 返回响应
        TtsVoiceResponse response = new TtsVoiceResponse();
        BeanUtils.copyProperties(ttsVoice, response);
        response.setId(ttsVoice.getId().toString());

        return response;
    }

    @Override
    public List<TtsVoiceListResponse> getVoiceList() {
        List<TtsVoice> voices = ttsVoiceMapper.selectList(null);

        return voices.stream()
                .map(voice -> {
                    TtsVoiceListResponse response = new TtsVoiceListResponse();
                    response.setId(voice.getId().toString());
                    response.setName(voice.getName());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TtsVoiceResponse> getFullVoiceList() {
        List<TtsVoice> voices = ttsVoiceMapper.selectList(null);

        return voices.stream()
                .map(voice -> {
                    TtsVoiceResponse response = new TtsVoiceResponse();
                    BeanUtils.copyProperties(voice, response);
                    response.setId(voice.getId().toString());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getProviderVoiceIdByName(String name) {
        LambdaQueryWrapper<TtsVoice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TtsVoice::getName, name);

        TtsVoice ttsVoice = ttsVoiceMapper.selectOne(queryWrapper);
        if (ttsVoice == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND, "音色不存在");
        }

        return ttsVoice.getProviderVoiceId();
    }
}