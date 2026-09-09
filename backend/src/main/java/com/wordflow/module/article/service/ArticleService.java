package com.wordflow.module.article.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordflow.common.BusinessException;
import com.wordflow.common.ResultCode;
import com.wordflow.module.ai.AiModels.ArticleDraft;
import com.wordflow.module.ai.AiModels.AiWord;
import com.wordflow.module.ai.AiModels.JudgeRequest;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.article.dto.ArticleDtos.ErrorItem;
import com.wordflow.module.article.entity.ArticleAttempt;
import com.wordflow.module.article.entity.LearningArticle;
import com.wordflow.module.article.mapper.ArticleAttemptMapper;
import com.wordflow.module.article.mapper.LearningArticleMapper;
import com.wordflow.module.word.entity.Word;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 短文服务。
 *
 * 模块职责：
 *   - 调用 AI 生成「今日总结短文 / 复习短文」；
 *   - 调用 AI 批改用户译文，保存每次尝试，返回错误明细。
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    /** 通过阈值：正确率 >= 60 视为通过 */
    public static final int PASS_SCORE = 60;

    private final LearningArticleMapper articleMapper;
    private final ArticleAttemptMapper attemptMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public LearningArticle createArticle(Long userId, String type, List<Word> words,
                                         List<Integer> priorities, String extraInstruction) {
        List<AiWord> aiWords = java.util.stream.IntStream.range(0, words.size())
                .mapToObj(i -> new AiWord(
                        words.get(i).getWord(),
                        words.get(i).getChinese(),
                        priorities == null ? 1 : priorities.get(i)))
                .toList();
        ArticleDraft draft = aiService.generateArticle(type, aiWords, extraInstruction);

        LearningArticle article = new LearningArticle();
        article.setUserId(userId);
        article.setType(type);
        article.setTitle(draft.title());
        article.setContentEn(draft.contentEn());
        article.setContentZh(draft.contentZh());
        article.setWordsJson(words.stream().map(Word::getId).toList().toString());
        articleMapper.insert(article);
        return article;
    }

    public LearningArticle getOwnedArticle(Long userId, Long articleId) {
        LearningArticle article = articleMapper.selectById(articleId);
        if (article == null || !article.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        return article;
    }

    @Transactional(rollbackFor = Exception.class)
    public ArticleCheckResult grade(Long userId, Long articleId, String userTranslation) {
        LearningArticle article = getOwnedArticle(userId, articleId);
        TranslationJudgement judgement = aiService.judgeTranslation(new JudgeRequest(
                "ARTICLE",
                article.getContentEn(),
                "en",
                "zh",
                userTranslation,
                null,
                null,
                article.getContentZh()));

        ArticleAttempt attempt = new ArticleAttempt();
        attempt.setArticleId(articleId);
        attempt.setUserId(userId);
        attempt.setUserTranslation(userTranslation);
        attempt.setScore(BigDecimal.valueOf(judgement.score()));
        attempt.setIsPass(judgement.passed() ? 1 : 0);
        attempt.setErrorsJson(writeErrors(judgement.errors()));
        attemptMapper.insert(attempt);

        List<ErrorItem> errors = judgement.errors().stream()
                .map(e -> new ErrorItem(e.segment(), e.expected(), e.user(), e.suggestion()))
                .toList();
        return new ArticleCheckResult(
                articleId,
                judgement.passed(),
                judgement.score(),
                judgement.comment(),
                errors,
                article.getContentZh());
    }

    private String writeErrors(List<com.wordflow.module.ai.AiModels.ErrorItem> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (Exception ex) {
            return "[]";
        }
    }
}

