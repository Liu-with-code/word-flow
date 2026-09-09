package com.wordflow.module.progress.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户单词进度实体，对应表 learn_progress。
 *
 * 模块职责：
 *   - 记录单词学习状态、答对/答错次数与艾宾浩斯复习时间。
 * 你需要完成：
 *   - 如需「熟练度/模糊度」等更细指标，可在此扩展字段。
 */
@Data
@TableName("learn_progress")
public class WordProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long wordId;

    /** LEARNING-学习中 MASTERED-已学会 REVIEWING-复习中 COMPLETE-长期掌握 */
    private String status;

    /** 艾宾浩斯复习阶段 0-6 */
    private Integer stage;

    private Integer learnCount;

    private Integer correctCount;

    private Integer wrongCount;

    private LocalDateTime lastLearnedAt;

    private LocalDateTime nextReviewAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

