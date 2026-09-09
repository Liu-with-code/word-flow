package com.wordflow.module.progress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.progress.entity.WordProgress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 单词进度 Mapper。
 */
@Mapper
public interface WordProgressMapper extends BaseMapper<WordProgress> {
}

