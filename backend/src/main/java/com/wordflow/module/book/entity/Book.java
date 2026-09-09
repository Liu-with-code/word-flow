package com.wordflow.module.book.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 词书实体，对应表 learn_book。
 *
 * 模块职责：
 *   - 词书元数据：名称、编码、级别、简介、封面色、词数、排序。
 */
@Data
@TableName("learn_book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 唯一编码，如 CET4 / CET6 / KY / TOEFL */
    private String code;

    /** 级别标签：初中/高中/四级/六级/考研/托福/雅思/GRE */
    private String level;

    private String description;

    /** 前端封面主色（十六进制色值） */
    private String coverColor;

    private Integer wordCount;

    private Integer sortNo;

    /** 1-启用 0-停用 */
    private Integer status;

    /** 数据来源，用于版权/出处溯源 */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
