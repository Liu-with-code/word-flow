package com.wordflow.module.learning;

import com.wordflow.common.Result;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.learning.dto.LearningDtos.CheckEnRequest;
import com.wordflow.module.learning.dto.LearningDtos.CheckZhRequest;
import com.wordflow.module.learning.dto.LearningDtos.CompleteWordRequest;
import com.wordflow.module.learning.dto.LearningDtos.FinishDayResponse;
import com.wordflow.module.learning.dto.LearningDtos.HintResponse;
import com.wordflow.module.learning.dto.LearningDtos.PracticeResponse;
import com.wordflow.module.learning.dto.LearningDtos.StepResult;
import com.wordflow.module.learning.dto.LearningDtos.TodayPlanResponse;
import com.wordflow.module.learning.dto.LearningDtos.TranslateHintRequest;
import com.wordflow.module.learning.dto.LearningDtos.TranslateRequest;
import com.wordflow.module.learning.dto.LearningDtos.TranslateStreamRequest;
import com.wordflow.module.learning.service.LearningService;
import com.wordflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 每日学习接口。
 *
 * 模块职责：
 *   - 暴露「今日计划 / 逐步作答 / AI 练习句 / 完成单词 / 总结短文」全套接口。
 * 你需要完成：
 *   - 无需大改；如需断点续学到具体步骤，配合服务层扩展。
 */
@Tag(name = "每日学习")
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    @Operation(summary = "获取今日计划")
    @GetMapping("/today")
    public Result<TodayPlanResponse> today() {
        return Result.ok(learningService.getToday(UserContext.getUserId()));
    }

    @Operation(summary = "开始今日学习")
    @PostMapping("/start")
    public Result<TodayPlanResponse> start() {
        return Result.ok(learningService.startToday(UserContext.getUserId()));
    }

    @Operation(summary = "协调今日计划（检测词书/目标变更后更新）")
    @PostMapping("/reconcile")
    public Result<TodayPlanResponse> reconcile() {
        return Result.ok(learningService.reconcileToday(UserContext.getUserId()));
    }

    @Operation(summary = "获取当前单词的 AI 练习句")
    @GetMapping("/practice/{wordId}")
    public Result<PracticeResponse> practice(@PathVariable Long wordId) {
        return Result.ok(learningService.getPracticeSentence(UserContext.getUserId(), wordId));
    }

    @Operation(summary = "第一步：看英文选中文释义")
    @PostMapping("/check-zh")
    public Result<StepResult> checkZh(@RequestBody CheckZhRequest request) {
        return Result.ok(learningService.checkZh(UserContext.getUserId(), request));
    }

    @Operation(summary = "第二步：看中文选英文单词")
    @PostMapping("/check-en")
    public Result<StepResult> checkEn(@RequestBody CheckEnRequest request) {
        return Result.ok(learningService.checkEn(UserContext.getUserId(), request));
    }

    @Operation(summary = "第三步：英译中（AI 批改）")
    @PostMapping("/translate-en")
    public Result<StepResult> translateEn(@RequestBody TranslateRequest request) {
        return Result.ok(learningService.translateEn(UserContext.getUserId(), request));
    }

    @Operation(summary = "流式批改中英互译（SSE，实时显示批改过程）")
    @PostMapping(value = "/translate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter translateStream(@RequestBody TranslateStreamRequest request) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(180_000L);
        CompletableFuture.runAsync(() -> {
            try {
                learningService.streamTranslate(userId, request,
                        chunk -> sendEvent(emitter, "chunk", chunk),
                        result -> {
                            sendEvent(emitter, "done", result);
                            emitter.complete();
                        });
            } catch (Exception ex) {
                sendEvent(emitter, "error", ex.getMessage());
                emitter.complete();
            }
        });
        return emitter;
    }

    @Operation(summary = "第四步：中译英（AI 批改）")
    @PostMapping("/translate-zh")
    public Result<StepResult> translateZh(@RequestBody TranslateRequest request) {
        return Result.ok(learningService.translateZh(UserContext.getUserId(), request));
    }

    @Operation(summary = "翻译不会做时获取参考译文")
    @PostMapping("/translate-hint")
    public Result<HintResponse> translateHint(@RequestBody TranslateHintRequest request) {
        return Result.ok(learningService.translateHint(UserContext.getUserId(), request));
    }

    @Operation(summary = "标记单词完成")
    @PostMapping("/complete-word")
    public Result<StepResult> completeWord(@RequestBody CompleteWordRequest request) {
        return Result.ok(learningService.completeWord(UserContext.getUserId(), request));
    }

    @Operation(summary = "今日单词全部完成，生成总结短文")
    @PostMapping("/finish-day")
    public Result<FinishDayResponse> finishDay() {
        return Result.ok(learningService.finishDay(UserContext.getUserId()));
    }

    @Operation(summary = "批改总结短文翻译")
    @PostMapping("/article/check")
    public Result<ArticleCheckResult> checkArticle(@RequestBody ArticleCheckRequest request) {
        return Result.ok(learningService.checkArticle(UserContext.getUserId(), request));
    }

    private void sendEvent(SseEmitter emitter, String type, Object payload) {
        try {
            emitter.send(SseEmitter.event().data(Map.of("type", type, "payload", payload)));
        } catch (Exception ignored) {
            // 客户端断开时忽略
        }
    }
}
