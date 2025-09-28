package com.vocata.voice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocata.voice.entity.TtsVoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * TTS音色Mapper
 */
@Mapper
public interface TtsVoiceMapper extends BaseMapper<TtsVoice> {
}