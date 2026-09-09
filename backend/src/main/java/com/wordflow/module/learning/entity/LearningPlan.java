package com.wordflow.module.learning.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日学习计划实体，对应表 learn_plan。
 */
@Data
@TableName("learn_plan")
public class LearningPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 当日计划对应的词书ID */
    private Long bookId;

    private LocalDate planDate;

    private Integer newWordCount;

    private Integer completedCount;

    /** IN_PROGRESS-进行中 COMPLETED-已完成 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
