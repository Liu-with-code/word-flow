package com.wordflow.module.ai;

import com.wordflow.module.ai.AiModels.ArticleDraft;
import com.wordflow.module.ai.AiModels.AiWord;
import com.wordflow.module.ai.AiModels.JudgeRequest;
import com.wordflow.module.ai.AiModels.PracticeSentence;
import com.wordflow.module.ai.AiModels.TranslationJudgement;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 服务接口（策略模式）。
 *
 * 模块职责：
 *   - 屏蔽底层大模型差异：业务层只调用本接口，
 *     provider=mock 时用 {@link MockAiService}，provider=openai 时用 {@link OpenAiService}。
 * 你需要完成：
 *   - 后期可按需扩展接口方法（如生词润色、文章难度评估）。
 */
public interface AiService {

    /** 批改一句中英互译 */
    TranslationJudgement judgeTranslation(JudgeRequest request);

    /**
     * 流式批改一句中英互译。
     *
     * @param onChunk   大模型生成的增量文本（用于前端实时展示）
     * @param onComplete 生成完成后返回结构化批改结果
     */
    void streamJudgeTranslation(JudgeRequest request,
                                Consumer<String> onChunk,
                                Consumer<TranslationJudgement> onComplete);

    /** 生成包含当前单词、尽量复现已学单词的练习句 */
    PracticeSentence generatePracticeSentence(String currentWord, String currentChinese, List<String> previousWords);

    /**
     * 生成短文。
     *
     * @param scene            DAILY_SUMMARY / REVIEW
     * @param words            目标单词及优先级
     * @param extraInstruction 额外约束（如艾宾浩斯复习要求）
     */
    ArticleDraft generateArticle(String scene, List<AiWord> words, String extraInstruction);
}
