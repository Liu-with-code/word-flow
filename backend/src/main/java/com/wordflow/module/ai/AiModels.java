package com.wordflow.module.ai;

import java.util.List;

/**
 * AI 模块数据模型（请求/响应）。
 *
 * 模块职责：
 *   - 统一 AI 交互的入参与出参，业务层只依赖这些结构，不关心具体模型。
 */
public final class AiModels {

    private AiModels() {
    }

    /** 参与 AI 生成的单词及优先级（优先级越高，在文章中重复越多） */
    public record AiWord(String word, String chinese, int priority) {
    }

    /** 翻译批改请求 */
    public record JudgeRequest(
            String scene,
            String sourceText,
            String sourceLang,
            String targetLang,
            String userTranslation,
            String requiredWord,
            String expectedMeaning,
            String standardAnswer
    ) {
    }

    /** 翻译批改结果 */
    public record TranslationJudgement(
            boolean passed,
            int score,
            String comment,
            String standardAnswer,
            List<ErrorItem> errors
    ) {
    }

    /** 批改错误明细（短文翻译时用于逐条展示） */
    public record ErrorItem(
            String segment,
            String expected,
            String user,
            String suggestion
    ) {
    }

    /** 带当前单词与已学单词的练习句子 */
    public record PracticeSentence(String sentenceEn, String sentenceZh) {
    }

    /** AI 生成的短文 */
    public record ArticleDraft(String title, String contentEn, String contentZh) {
    }
}
