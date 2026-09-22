package com.wordflow.module.learning.service;

import com.wordflow.common.BusinessException;
import com.wordflow.module.ai.AiModels.PracticeSentence;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.service.ArticleService;
import com.wordflow.module.book.entity.Book;
import com.wordflow.module.book.service.BookService;
import com.wordflow.module.learning.dto.LearningDtos.AddWordsRequest;
import com.wordflow.module.learning.dto.LearningDtos.CheckEnRequest;
import com.wordflow.module.learning.dto.LearningDtos.CheckZhRequest;
import com.wordflow.module.learning.dto.LearningDtos.CompleteWordRequest;
import com.wordflow.module.learning.dto.LearningDtos.HintResponse;
import com.wordflow.module.learning.dto.LearningDtos.StepResult;
import com.wordflow.module.learning.dto.LearningDtos.TranslateHintRequest;
import com.wordflow.module.learning.dto.LearningDtos.TranslateStreamRequest;
import com.wordflow.module.learning.entity.LearningPlan;
import com.wordflow.module.learning.entity.LearningPlanWord;
import com.wordflow.module.learning.mapper.LearningPlanMapper;
import com.wordflow.module.learning.mapper.LearningPlanWordMapper;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.service.UserService;
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
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 每日学习服务单元测试。
 *
 * 覆盖：看英选义/看义选英、练习句缓存、流式批改回调、
 *       “我不会”参考译文、单词完成与计划推进。
 */
@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-23T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private LearningPlanMapper planMapper;

    @Mock
    private LearningPlanWordMapper planWordMapper;

    @Mock
    private WordService wordService;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @Mock
    private ProgressService progressService;

    @Mock
    private ArticleService articleService;

    @Mock
    private AiService aiService;

    private LearningService learningService;

    @BeforeEach
    void setUp() {
        learningService = new LearningService(
                planMapper, planWordMapper, wordService, bookService, userService,
                progressService, articleService, aiService, FIXED_CLOCK);
    }

    @Test
    void shouldAppendWordsToTodayPlan_whenCapacityRemains() {
        stubCurrentUser();
        stubDailyGoal(10);
        LearningPlan plan = new LearningPlan();
        plan.setId(1L);
        plan.setUserId(1L);
        plan.setBookId(1L);
        plan.setPlanDate(LocalDate.of(2026, 8, 23));
        plan.setNewWordCount(3);
        plan.setCompletedCount(0);
        plan.setStatus("IN_PROGRESS");
        when(planMapper.selectOne(any())).thenReturn(plan);
        when(planWordMapper.selectList(any())).thenReturn(List.of(planWord(10L), planWord(11L), planWord(12L)));
        when(wordService.getById(any())).thenReturn(word(20L, "abandon", "放弃"));
        when(bookService.getById(1L)).thenReturn(book());

        learningService.addWordsToToday(1L, new AddWordsRequest(List.of(20L, 20L)));

        // 去重后只插入 1 条计划明细
        verify(planWordMapper, times(1)).insert(any(LearningPlanWord.class));
        verify(progressService).ensureProgress(1L, 20L);
        assertThat(plan.getNewWordCount()).isEqualTo(4);
    }

    @Test
    void shouldRejectAppend_whenNoPlanToday() {
        stubCurrentUser();
        when(planMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> learningService.addWordsToToday(1L, new AddWordsRequest(List.of(20L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("今日还没有学习计划");
    }

    @Test
    void shouldRejectAppend_whenGoalAlreadyReached() {
        stubCurrentUser();
        stubDailyGoal(3);
        LearningPlan plan = new LearningPlan();
        plan.setId(1L);
        plan.setUserId(1L);
        plan.setBookId(1L);
        plan.setNewWordCount(3);
        plan.setCompletedCount(3);
        plan.setStatus("COMPLETED");
        when(planMapper.selectOne(any())).thenReturn(plan);
        // 今日目标 3 个且已全部完成 -> 剩余容量 0
        when(planWordMapper.selectList(any())).thenReturn(List.of(
                completedPlanWord(10L), completedPlanWord(11L), completedPlanWord(12L)));

        assertThatThrownBy(() -> learningService.addWordsToToday(1L, new AddWordsRequest(List.of(20L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("今日目标已完成");
    }
    @Test
    void shouldAdvance_whenChineseMeaningCorrect() {
        when(wordService.getById(1L)).thenReturn(word(1L, "ability", "能力"));

        StepResult result = learningService.checkZh(1L, new CheckZhRequest(1L, "能力"));

        assertThat(result.correct()).isTrue();
        assertThat(result.nextStep()).isEqualTo("CHOOSE_EN");
        verify(progressService).recordAnswer(eq(1L), eq(1L), eq("DAILY_LEARN"),
                eq("CHOOSE_ZH"), eq(true), eq("能力"), any());
    }

    @Test
    void shouldBackToPreview_whenChineseMeaningWrong() {
        when(wordService.getById(1L)).thenReturn(word(1L, "ability", "能力"));

        StepResult result = learningService.checkZh(1L, new CheckZhRequest(1L, "错误释义"));

        assertThat(result.correct()).isFalse();
        assertThat(result.nextStep()).isEqualTo("PREVIEW");
    }

    @Test
    void shouldAdvance_whenEnglishWordCorrect() {
        when(wordService.getById(1L)).thenReturn(word(1L, "ability", "能力"));

        StepResult result = learningService.checkEn(1L, new CheckEnRequest(1L, "ability"));

        assertThat(result.correct()).isTrue();
        assertThat(result.nextStep()).isEqualTo("TRANS_EN");
    }

    @Test
    void shouldCachePracticeSentence() {
        stubCurrentUser();
        when(wordService.getById(1L)).thenReturn(word(1L, "ability", "能力"));
        when(aiService.generatePracticeSentence(anyString(), anyString(), anyList()))
                .thenReturn(new PracticeSentence("EN", "ZH"));

        learningService.getPracticeSentence(1L, 1L);
        learningService.getPracticeSentence(1L, 1L);

        verify(aiService, times(1)).generatePracticeSentence(anyString(), anyString(), anyList());
    }

    @Test
    void shouldStreamTranslate_andReturnStepResult() {
        stubCurrentUser();
        when(wordService.getById(1L)).thenReturn(word(1L, "ability", "能力"));
        when(aiService.generatePracticeSentence(anyString(), anyString(), anyList()))
                .thenReturn(new PracticeSentence("EN", "ZH"));
        learningService.getPracticeSentence(1L, 1L); // 预热练习句缓存

        doAnswer(invocation -> {
            java.util.function.Consumer<String> onChunk = invocation.getArgument(1);
            java.util.function.Consumer<TranslationJudgement> onComplete = invocation.getArgument(2);
            onChunk.accept("点评");
            onComplete.accept(new TranslationJudgement(
                    true, 88, "不错", "标准译文", List.of()));
            return null;
        }).when(aiService).streamJudgeTranslation(any(), any(), any());

        AtomicReference<String> chunkRef = new AtomicReference<>();
        AtomicReference<StepResult> resultRef = new AtomicReference<>();
        learningService.streamTranslate(1L,
                new TranslateStreamRequest(1L, "EN", "我的译文", "en2zh"),
                chunkRef::set, resultRef::set);

        assertThat(chunkRef.get()).isEqualTo("点评");
        assertThat(resultRef.get().correct()).isTrue();
        assertThat(resultRef.get().nextStep()).isEqualTo("TRANS_ZH");
        assertThat(resultRef.get().score()).isEqualTo(88);
        assertThat(resultRef.get().errors()).isEmpty();
        verify(progressService).recordAnswer(eq(1L), eq(1L), eq("DAILY_LEARN"),
                eq("TRANS_EN"), eq(true), eq("我的译文"), eq("不错"));
    }

    @Test
    void shouldReturnHint_andRecordWrong_whenHintRequested() {
        stubCurrentUser();
        when(wordService.getById(1L)).thenReturn(word(1L, "ability", "能力"));
        when(aiService.generatePracticeSentence(anyString(), anyString(), anyList()))
                .thenReturn(new PracticeSentence("EN", "ZH"));
        learningService.getPracticeSentence(1L, 1L);

        HintResponse response = learningService.translateHint(1L, new TranslateHintRequest(1L, "en2zh"));

        assertThat(response.hint()).isEqualTo("ZH");
        assertThat(response.sentence()).isEqualTo("EN");
        verify(progressService).recordAnswer(eq(1L), eq(1L), eq("DAILY_LEARN"),
                eq("TRANS_HINT"), eq(false), anyString(), any());
    }

    @Test
    void shouldCompletePlan_whenLastWordDone() {
        stubCurrentUser();
        LearningPlan plan = new LearningPlan();
        plan.setId(1L);
        plan.setUserId(1L);
        plan.setNewWordCount(1);
        plan.setCompletedCount(0);
        plan.setStatus("IN_PROGRESS");
        LearningPlanWord planWord = new LearningPlanWord();
        planWord.setPlanId(1L);
        planWord.setWordId(1L);
        planWord.setStatus("PENDING");
        when(planMapper.selectOne(any())).thenReturn(plan);
        when(planWordMapper.selectOne(any())).thenReturn(planWord);

        StepResult result = learningService.completeWord(1L, new CompleteWordRequest(1L));

        assertThat(result.correct()).isTrue();
        assertThat(result.finished()).isTrue();
        assertThat(result.nextStep()).isEqualTo("FINISH_DAY");
        assertThat(planWord.getStatus()).isEqualTo("COMPLETED");
        assertThat(plan.getStatus()).isEqualTo("COMPLETED");
        verify(progressService).markLearned(1L, 1L);
    }

    private void stubCurrentUser() {
        User user = new User();
        user.setId(1L);
        user.setTimezone("Asia/Shanghai");
        user.setDayBoundaryHour(0);
        when(userService.getById(1L)).thenReturn(user);
    }

    /** 指定每日目标：复用 stubCurrentUser 创建的同一个用户对象，修改其可变字段。 */
    private void stubDailyGoal(int goal) {
        User user = userService.getById(1L);
        if (user == null) {
            stubCurrentUser();
            user = userService.getById(1L);
        }
        user.setDailyWordGoal(goal);
    }

    private LearningPlanWord planWord(Long wordId) {
        LearningPlanWord planWord = new LearningPlanWord();
        planWord.setPlanId(1L);
        planWord.setUserId(1L);
        planWord.setWordId(wordId);
        planWord.setStatus("PENDING");
        return planWord;
    }

    private LearningPlanWord completedPlanWord(Long wordId) {
        LearningPlanWord planWord = planWord(wordId);
        planWord.setStatus("COMPLETED");
        return planWord;
    }

    private Book book() {
        Book book = new Book();
        book.setId(1L);
        book.setName("四级核心词汇");
        return book;
    }

    private Word word(Long id, String text, String chinese) {
        Word word = new Word();
        word.setId(id);
        word.setWord(text);
        word.setChinese(chinese);
        word.setExampleEn("EN");
        word.setExampleZh("ZH");
        return word;
    }
}
