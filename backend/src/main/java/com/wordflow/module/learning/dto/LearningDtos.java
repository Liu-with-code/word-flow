package com.wordflow.module.learning.dto;

import com.wordflow.module.ai.AiModels.ErrorItem;
import com.wordflow.module.word.dto.WordVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习模块数据传输对象集合。
 *
 * 入参对象由 Controller 层的 {@code @Valid} 触发校验，Service 层不再重复判空。
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
            String bookName,
            int dailyWordGoal
    ) {
    }

    /** AI 练习句 */
    public record PracticeResponse(String sentenceEn, String sentenceZh) {
    }

    /** 第一步：看英文选中文释义 */
    public record CheckZhRequest(
            @NotNull(message = "单词 ID 不能为空") Long wordId,
            @NotBlank(message = "所选释义不能为空") String selectedChinese
    ) {
    }

    /** 第二步：看中文选英文单词 */
    public record CheckEnRequest(
            @NotNull(message = "单词 ID 不能为空") Long wordId,
            @NotBlank(message = "所选单词不能为空") String selectedWord
    ) {
    }

    /** 中英互译提交 */
    public record TranslateRequest(
            @NotNull(message = "单词 ID 不能为空") Long wordId,
            @NotBlank(message = "原句不能为空") String sentence,
            @NotBlank(message = "译文不能为空")
            @Size(max = 4000, message = "译文最长 4000 字符") String userTranslation
    ) {
    }

    /** 流式中英互译提交（direction: en2zh / zh2en） */
    public record TranslateStreamRequest(
            @NotNull(message = "单词 ID 不能为空") Long wordId,
            @NotBlank(message = "原句不能为空") String sentence,
            @NotBlank(message = "译文不能为空")
            @Size(max = 4000, message = "译文最长 4000 字符") String userTranslation,
            @NotBlank(message = "翻译方向不能为空") String direction
    ) {
    }

    /** “我不会”参考译文请求 */
    public record TranslateHintRequest(
            @NotNull(message = "单词 ID 不能为空") Long wordId,
            @NotBlank(message = "翻译方向不能为空") String direction
    ) {
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
    public record CompleteWordRequest(
            @NotNull(message = "单词 ID 不能为空") Long wordId
    ) {
    }

    /** 追加单词到今日计划（错题本「加入今日学习」） */
    public record AddWordsRequest(
            @NotEmpty(message = "请至少选择一个单词")
            @Size(max = 50, message = "单次最多追加 50 个单词")
            List<Long> wordIds
    ) {
    }

    /** 今日单词全部学完，AI 生成总结短文 */
    public record FinishDayResponse(Long articleId, String title, String contentEn, int wordCount) {
    }
}
