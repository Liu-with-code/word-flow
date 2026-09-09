package com.wordflow.module.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志实体，对应表 learn_ai_log。
 *
 * 模块职责：
 *   - 记录每次大模型请求与响应，便于调试 Prompt、排查费用与复现问题。
 */
@Data
@TableName("learn_ai_log")
public class AiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 场景：JUDGE_TRANSLATION/GENERATE_SENTENCE/GENERATE_ARTICLE */
    private String scene;

    private String requestJson;

    private String responseJson;

    private String model;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

