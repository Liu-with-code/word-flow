package com.wordflow.module.article.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 短文翻译尝试实体，对应表 learn_article_attempt。
 */
@Data
@TableName("learn_article_attempt")
public class ArticleAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long userId;

    private String userTranslation;

    private BigDecimal score;

    private String errorsJson;

    private Integer isPass;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

