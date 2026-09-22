package com.wordflow.module.review.dto;

import com.wordflow.module.word.dto.WordVO;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * 复习模块数据传输对象。
 */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    /** 复习概览 */
    public record ReviewOverviewResponse(int dueCount) {
    }

    /** 单个单词在文章中的出现优先级 */
    public record WordFrequencyVO(String word, int priority) {
    }

    /** 开始复习响应 */
    public record ReviewStartResponse(
            boolean hasWords,
            Long articleId,
            String title,
            String contentEn,
            List<WordVO> words,
            List<WordFrequencyVO> frequency
    ) {
    }

    /** 夜间学习智能弹窗提示 */
    public record NightPromptResponse(boolean show, LocalDate date, int count) {
    }

    /** 用户对智能弹窗的答复 */
    public record NightPromptRequest(boolean apply) {
    }

    /** 复习热身：到期单词的快速自测卡片 */
    public record WarmupWordVO(
            WordVO word,
            int stage,
            String lastReviewAt,
            int priority,
            boolean marked
    ) {
    }

    /** 复习热身概览 */
    public record WarmupResponse(
            int dueCount,
            int total,
            int markedCount,
            List<WarmupWordVO> words
    ) {
    }

    /** 复习热身作答：remembered=true 表示记得（推进复习阶段） */
    public record WarmupMarkRequest(
            @NotNull(message = "单词 ID 不能为空") Long wordId,
            boolean remembered
    ) {
    }

    /** 热身作答结果 */
    public record WarmupMarkResponse(boolean remembered, int stage, String status, int remaining) {
    }
}
