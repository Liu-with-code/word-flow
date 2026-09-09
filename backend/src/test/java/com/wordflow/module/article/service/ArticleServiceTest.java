package com.wordflow.module.article.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordflow.common.BusinessException;
import com.wordflow.module.ai.AiModels.ArticleDraft;
import com.wordflow.module.ai.AiModels.ErrorItem;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.article.entity.ArticleAttempt;
import com.wordflow.module.article.entity.LearningArticle;
import com.wordflow.module.article.mapper.ArticleAttemptMapper;
import com.wordflow.module.article.mapper.LearningArticleMapper;
import com.wordflow.module.word.entity.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 短文服务单元测试。
 *
 * 覆盖：AI 生成短文入库、文章归属校验、译文批改与尝试记录。
 */
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private LearningArticleMapper articleMapper;

    @Mock
    private ArticleAttemptMapper attemptMapper;

    @Mock
    private AiService aiService;

    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleService(articleMapper, attemptMapper, aiService, new ObjectMapper());
    }

    @Test
    void shouldCreateArticle_withWordsJson() {
        when(aiService.generateArticle(any(), any(), any()))
                .thenReturn(new ArticleDraft("标题", "EN", "中文"));
        Word w1 = word(1L, "ability");
        Word w2 = word(2L, "benefit");

        LearningArticle article = articleService.createArticle(1L, "DAILY_SUMMARY",
                List.of(w1, w2), List.of(1, 1), "要求");

        ArgumentCaptor<LearningArticle> captor = ArgumentCaptor.forClass(LearningArticle.class);
        verify(articleMapper).insert(captor.capture());
        LearningArticle saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("标题");
        assertThat(saved.getContentEn()).isEqualTo("EN");
        assertThat(saved.getWordsJson()).isEqualTo("[1, 2]");
    }

    @Test
    void shouldThrow_whenArticleNotOwned() {
        LearningArticle article = new LearningArticle();
        article.setId(9L);
        article.setUserId(2L);
        when(articleMapper.selectById(9L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.getOwnedArticle(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    void shouldGrade_andPersistAttempt() {
        LearningArticle article = new LearningArticle();
        article.setId(9L);
        article.setUserId(1L);
        article.setContentEn("EN");
        article.setContentZh("中文标准");
        when(articleMapper.selectById(9L)).thenReturn(article);
        when(aiService.judgeTranslation(any())).thenReturn(new TranslationJudgement(
                false, 50, "有偏差", "标准", List.of(
                        new ErrorItem("EN", "标准", "译文", "建议"))));

        ArticleCheckResult result = articleService.grade(1L, 9L, "我的译文");

        ArgumentCaptor<ArticleAttempt> captor = ArgumentCaptor.forClass(ArticleAttempt.class);
        verify(attemptMapper).insert(captor.capture());
        ArticleAttempt attempt = captor.getValue();
        assertThat(attempt.getScore()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(attempt.getIsPass()).isZero();
        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.standardZh()).isEqualTo("中文标准");
    }

    private Word word(Long id, String text) {
        Word word = new Word();
        word.setId(id);
        word.setWord(text);
        word.setChinese("释义");
        return word;
    }
}
