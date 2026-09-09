package com.wordflow.module.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordflow.module.ai.AiModels.JudgeRequest;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.mapper.AiLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mock AI 服务单元测试。
 *
 * 覆盖：英译中/中译英批改通过与否、流式回调。
 */
@ExtendWith(MockitoExtension.class)
class MockAiServiceTest {

    @Mock
    private AiLogMapper aiLogMapper;

    private MockAiService mockAiService;

    @BeforeEach
    void setUp() {
        mockAiService = new MockAiService(aiLogMapper, new ObjectMapper());
    }

    @Test
    void shouldPassEn2Zh_whenMeaningCovered() {
        JudgeRequest request = new JudgeRequest(
                "TRANS_EN", "She has the ability.", "en", "zh",
                "她很有能力", "ability", "能力", "标准译文");

        TranslationJudgement judgement = mockAiService.judgeTranslation(request);

        assertThat(judgement.passed()).isTrue();
    }

    @Test
    void shouldFailEn2Zh_whenMeaningMissing() {
        JudgeRequest request = new JudgeRequest(
                "TRANS_EN", "She has the ability.", "en", "zh",
                "完全无关的内容", "ability", "能力", "标准译文");

        TranslationJudgement judgement = mockAiService.judgeTranslation(request);

        assertThat(judgement.passed()).isFalse();
    }

    @Test
    void shouldPassZh2En_whenRequiredWordPresent() {
        JudgeRequest request = new JudgeRequest(
                "TRANS_ZH", "她有能力。", "zh", "en",
                "I have the ability to solve it.", "ability", "能力",
                "I have the ability to solve it.");

        TranslationJudgement judgement = mockAiService.judgeTranslation(request);

        assertThat(judgement.passed()).isTrue();
    }

    @Test
    void shouldStreamComment_thenComplete() {
        JudgeRequest request = new JudgeRequest(
                "TRANS_EN", "text", "en", "zh", "x", "word", "释义", "标准");
        AtomicReference<String> chunk = new AtomicReference<>();
        AtomicReference<TranslationJudgement> completed = new AtomicReference<>();

        mockAiService.streamJudgeTranslation(request, chunk::set, completed::set);

        assertThat(chunk.get()).isNotBlank();
        assertThat(completed.get()).isNotNull();
    }
}
