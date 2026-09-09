package com.wordflow.module.article.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成短文实体，对应表 learn_article。
 */
@Data
@TableName("learn_article")
public class LearningArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** DAILY_SUMMARY-今日总结 REVIEW-复习文章 */
    private String type;

    private String title;

    private String contentEn;

    private String contentZh;

    /** 单词ID列表 JSON */
    private String wordsJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

