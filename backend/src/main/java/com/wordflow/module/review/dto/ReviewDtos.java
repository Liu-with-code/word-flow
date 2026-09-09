package com.wordflow.module.review.dto;

import com.wordflow.module.word.dto.WordVO;

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
}
