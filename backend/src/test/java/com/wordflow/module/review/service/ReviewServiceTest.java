package com.wordflow.module.review.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.article.entity.LearningArticle;
import com.wordflow.module.article.service.ArticleService;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.review.dto.ReviewDtos.NightPromptResponse;
import com.wordflow.module.review.dto.ReviewDtos.ReviewOverviewResponse;
import com.wordflow.module.review.dto.ReviewDtos.ReviewStartResponse;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 复习服务单元测试。
 *
 * 覆盖：到期概览、复习短文生成、夜间学习智能弹窗的判定与答复、
 *       紧急度排序（经短文优先级间接验证）。
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Clock DAY_CLOCK =
            Clock.fixed(Instant.parse("2026-08-23T06:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private static final Clock NIGHT_CLOCK =
            Clock.fixed(Instant.parse("2026-08-22T21:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private ProgressService progressService;

    @Mock
    private WordService wordService;

    @Mock
    private ArticleService articleService;

    @Mock
    private AiService aiService;

    @Mock
    private UserMapper userMapper;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                progressService, wordService, articleService, aiService,
                new ObjectMapper(), userMapper, DAY_CLOCK);
    }

    @Test
    void shouldReturnDueCount_whenOverview() {
        when(progressService.countDue(1L)).thenReturn(6L);

        ReviewOverviewResponse overview = reviewService.overview(1L);

        assertThat(overview.dueCount()).isEqualTo(6);
    }

    @Test
    void shouldReturnEmpty_whenNoDueWords() {
        when(progressService.listDueProgress(1L)).thenReturn(List.of());

        ReviewStartResponse response = reviewService.startReview(1L);

        assertThat(response.hasWords()).isFalse();
        assertThat(response.articleId()).isNull();
    }

    @Test
    void shouldGenerateArticleForDueWords_withPriorityOrder() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        when(progressService.zoneOf(1L)).thenReturn(zone);
        when(userMapper.selectById(1L)).thenReturn(user(0, null));
        WordProgress urgent = progress(1, LocalDateTime.now().minusDays(3), 1);
        WordProgress normal = progress(2, LocalDateTime.now().plusDays(1), 5);
        when(progressService.listDueProgress(1L)).thenReturn(new ArrayList<>(List.of(urgent, normal)));
        when(wordService.getById(1L)).thenReturn(word(1L, "urgent"));
        when(wordService.getById(2L)).thenReturn(word(2L, "normal"));
        LearningArticle article = new LearningArticle();
        article.setId(9L);
        article.setTitle("复习");
        article.setContentEn("EN");
        when(articleService.createArticle(eq(1L), eq("REVIEW"), anyList(), anyList(), any()))
                .thenReturn(article);

        ReviewStartResponse response = reviewService.startReview(1L);

        assertThat(response.hasWords()).isTrue();
        assertThat(response.articleId()).isEqualTo(9L);
        assertThat(response.frequency()).hasSize(2);
        // 逾期 3 天 + 早期阶段 -> 优先级 4，排第一
        assertThat(response.frequency().get(0).priority()).isEqualTo(4);
        assertThat(response.frequency().get(1).priority()).isEqualTo(1);
    }

    @Test
    void shouldLimitArticleWords_byReviewGoal() {
        when(progressService.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Shanghai"));
        User user = user(0, null);
        user.setDailyReviewGoal(2);
        when(userMapper.selectById(1L)).thenReturn(user);
        WordProgress w1 = progress(1, null, 1);
        WordProgress w2 = progress(2, null, 1);
        WordProgress w3 = progress(3, null, 1);
        when(progressService.listDueProgress(1L)).thenReturn(new ArrayList<>(List.of(w1, w2, w3)));
        when(wordService.getById(1L)).thenReturn(word(1L, "a"));
        when(wordService.getById(2L)).thenReturn(word(2L, "b"));
        LearningArticle article = new LearningArticle();
        article.setId(9L);
        article.setContentEn("EN");
        when(articleService.createArticle(eq(1L), eq("REVIEW"), anyList(), anyList(), any()))
                .thenReturn(article);

        ReviewStartResponse response = reviewService.startReview(1L);

        assertThat(response.words()).hasSize(2);
    }

    @Test
    void shouldHideNightPrompt_whenBoundaryEnabled() {
        when(progressService.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Shanghai"));
        when(userMapper.selectById(1L)).thenReturn(user(4, null));

        NightPromptResponse response = reviewService.nightPrompt(1L);

        assertThat(response.show()).isFalse();
    }

    @Test
    void shouldHideNightPrompt_whenAlreadyAskedToday() {
        when(progressService.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Shanghai"));
        when(userMapper.selectById(1L)).thenReturn(user(0, LocalDate.of(2026, 8, 23)));

        NightPromptResponse response = reviewService.nightPrompt(1L);

        assertThat(response.show()).isFalse();
    }

    @Test
    void shouldShowNightPrompt_inDaytime_withNightWords() {
        when(progressService.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Shanghai"));
        when(userMapper.selectById(1L)).thenReturn(user(0, null));
        when(progressService.listNightWords(eq(1L), any(), any()))
                .thenReturn(List.of(progress(1, null, 1), progress(2, null, 1), progress(3, null, 1)));

        NightPromptResponse response = reviewService.nightPrompt(1L);

        assertThat(response.show()).isTrue();
        assertThat(response.count()).isEqualTo(3);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 8, 23));
    }

    @Test
    void shouldHideNightPrompt_beforeCutoffHour() {
        reviewService = new ReviewService(
                progressService, wordService, articleService, aiService,
                new ObjectMapper(), userMapper, NIGHT_CLOCK);
        when(progressService.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Shanghai"));
        when(userMapper.selectById(1L)).thenReturn(user(0, null));
        when(progressService.listNightWords(eq(1L), any(), any()))
                .thenReturn(List.of(progress(1, null, 1)));

        NightPromptResponse response = reviewService.nightPrompt(1L);

        // 凌晨 5 点（< cutoff 6），尚未到白天，不弹窗
        assertThat(response.show()).isFalse();
    }

    @Test
    void shouldApplyNightWords_whenUserConfirms() {
        when(progressService.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Shanghai"));
        User user = user(0, null);
        when(userMapper.selectById(1L)).thenReturn(user);
        List<WordProgress> nightWords = List.of(progress(1, null, 1));
        when(progressService.listNightWords(eq(1L), any(), any())).thenReturn(nightWords);

        reviewService.answerNightPrompt(1L, true);

        verify(progressService).makeDueNow(1L, nightWords);
        assertThat(user.getNightPromptDate()).isEqualTo(LocalDate.of(2026, 8, 23));
    }

    @Test
    void shouldKeepSchedule_whenUserDeclines() {
        when(progressService.zoneOf(1L)).thenReturn(ZoneId.of("Asia/Shanghai"));
        User user = user(0, null);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(progressService.listNightWords(eq(1L), any(), any()))
                .thenReturn(List.of(progress(1, null, 1)));

        reviewService.answerNightPrompt(1L, false);

        verify(progressService, never()).makeDueNow(any(), any());
        assertThat(user.getNightPromptDate()).isEqualTo(LocalDate.of(2026, 8, 23));
    }

    @Test
    void shouldAdvanceWords_whenArticlePassed() {
        when(articleService.grade(eq(1L), eq(9L), eq("译文"))).thenReturn(
                new com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult(
                        9L, true, 85, "通过", List.of(), "标准"));
        LearningArticle article = new LearningArticle();
        article.setId(9L);
        article.setUserId(1L);
        article.setWordsJson("[1, 2]");
        when(articleService.getOwnedArticle(1L, 9L)).thenReturn(article);

        ArticleCheckResult result = reviewService.checkArticle(
                1L, new ArticleCheckRequest(9L, "译文"));

        assertThat(result.passed()).isTrue();
        verify(progressService).advanceReview(1L, 1L);
        verify(progressService).advanceReview(1L, 2L);
    }

    private User user(int boundary, LocalDate promptDate) {
        User user = new User();
        user.setId(1L);
        user.setTimezone("Asia/Shanghai");
        user.setDailyReviewGoal(20);
        user.setDayBoundaryHour(boundary);
        user.setNightCutoffHour(6);
        user.setNightPromptDate(promptDate);
        return user;
    }

    private WordProgress progress(long id, LocalDateTime nextReviewAt, int stage) {
        WordProgress progress = new WordProgress();
        progress.setId(id);
        progress.setUserId(1L);
        progress.setWordId(id);
        progress.setNextReviewAt(nextReviewAt);
        progress.setStage(stage);
        return progress;
    }

    private Word word(Long id, String text) {
        Word word = new Word();
        word.setId(id);
        word.setWord(text);
        word.setChinese("释义");
        return word;
    }
}
