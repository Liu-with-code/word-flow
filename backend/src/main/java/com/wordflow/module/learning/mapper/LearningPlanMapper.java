package com.wordflow.module.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.learning.entity.LearningPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * 学习计划 Mapper。
 */
@Mapper
public interface LearningPlanMapper extends BaseMapper<LearningPlan> {
}
