package com.vocata.character.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocata.character.entity.TagStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签统计数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，提供基础CRUD操作
 * 复杂查询通过Service层使用QueryWrapper实现
 */
@Mapper
public interface TagStatsMapper extends BaseMapper<TagStats> {

}