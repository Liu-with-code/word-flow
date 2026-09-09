package com.wordflow.module.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.learning.entity.LearningPlanWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划单词明细 Mapper。
 */
@Mapper
public interface LearningPlanWordMapper extends BaseMapper<LearningPlanWord> {
}

