package com.wordflow.module.learning.dto;

import com.wordflow.module.ai.AiModels.ErrorItem;
import com.wordflow.module.word.dto.WordVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习模块数据传输对象集合。
 */
public final class LearningDtos {

    private LearningDtos() {
    }

    /** 今日计划总览 */
    public record TodayPlanResponse(
            LocalDate planDate,
            int total,
            int completed,
            int currentIndex,
            List<WordVO> words,
            Long bookId,
            String bookName
    ) {
    }

    /** AI 练习句 */
    public record PracticeResponse(String sentenceEn, String sentenceZh) {
    }

    /** 第一步：看英文选中文释义 */
    public record CheckZhRequest(Long wordId, String selectedChinese) {
    }

    /** 第二步：看中文选英文单词 */
    public record CheckEnRequest(Long wordId, String selectedWord) {
    }

    /** 中英互译提交 */
    public record TranslateRequest(Long wordId, String sentence, String userTranslation) {
    }

    /** 流式中英互译提交（direction: en2zh / zh2en） */
    public record TranslateStreamRequest(Long wordId, String sentence, String userTranslation, String direction) {
    }

    /** “我不会”参考译文请求 */
    public record TranslateHintRequest(Long wordId, String direction) {
    }

    /** “我不会”参考译文响应 */
    public record HintResponse(String hint, String sentence) {
    }

    /** 单步结果：前端依据 nextStep 驱动状态机 */
    public record StepResult(
            boolean correct,
            String message,
            String nextStep,
            Integer score,
            String comment,
            String standard,
            boolean finished,
            List<ErrorItem> errors
    ) {
    }

    /** 标记单词完成 */
    public record CompleteWordRequest(Long wordId) {
    }

    /** 今日单词全部学完，AI 生成总结短文 */
    public record FinishDayResponse(Long articleId, String title, String contentEn, int wordCount) {
    }
}
