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
 * provider=mock 时用 {@link MockAiService}，provider=openai 时用 {@link OpenAiService}。
 */
public interface AiService {

    /**
     * 批改一句中英互译。
     *
     * @param request 批改入参（场景、原文、用户译文、目标单词、标准译文）
     * @return 批改结论：是否通过、得分、评语、标准答案与错误明细
     */
    TranslationJudgement judgeTranslation(JudgeRequest request);

    /**
     * 流式批改一句中英互译。
     *
     * @param request    批改入参（场景、原文、用户译文、目标单词、标准译文）
     * @param onChunk    大模型生成的增量文本（用于前端实时展示）
     * @param onComplete 生成完成后返回结构化批改结果
     */
    void streamJudgeTranslation(JudgeRequest request,
                                Consumer<String> onChunk,
                                Consumer<TranslationJudgement> onComplete);

    /**
     * 生成包含当前单词、尽量复现已学单词的练习句。
     *
     * @param currentWord    当前学习的英文单词
     * @param currentChinese 当前单词的中文释义
     * @param previousWords  需要自然复现的已学/薄弱单词列表
     * @return 英文练习句及其标准中文翻译
     */
    PracticeSentence generatePracticeSentence(String currentWord, String currentChinese,
                                              List<String> previousWords);

    /**
     * 生成短文。
     *
     * @param scene            DAILY_SUMMARY / REVIEW
     * @param words            目标单词及优先级
     * @param extraInstruction 额外约束（如艾宾浩斯复习要求）
     * @return 短文草稿：标题、英文正文与标准中文翻译
     */
    ArticleDraft generateArticle(String scene, List<AiWord> words, String extraInstruction);
}
