package com.wordflow.module.learning.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录流水实体，对应表 learn_record。
 */
@Data
@TableName("learn_record")
public class LearningRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long wordId;

    /** DAILY_LEARN-每日学习 REVIEW-复习 ARTICLE-短文翻译 */
    private String sessionType;

    /** CHOOSE_ZH/CHOOSE_EN/TRANS_EN/TRANS_ZH/ARTICLE */
    private String stepType;

    private Integer isCorrect;

    private String userAnswer;

    private String aiFeedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

