package com.wordflow.module.learning.service;

import com.wordflow.module.ai.AiModels.PracticeSentence;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.service.ArticleService;
import com.wordflow.module.book.service.BookService;
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
import com.wordflow.module.user.service.UserService;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
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
        com.wordflow.module.user.entity.User user = new com.wordflow.module.user.entity.User();
        user.setId(1L);
        user.setTimezone("Asia/Shanghai");
        user.setDayBoundaryHour(0);
        when(userService.getById(1L)).thenReturn(user);
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
