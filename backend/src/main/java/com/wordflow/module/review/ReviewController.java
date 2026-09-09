package com.wordflow.module.review;

import com.wordflow.common.Result;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.review.dto.ReviewDtos.ReviewOverviewResponse;
import com.wordflow.module.review.dto.ReviewDtos.NightPromptRequest;
import com.wordflow.module.review.dto.ReviewDtos.NightPromptResponse;
import com.wordflow.module.review.dto.ReviewDtos.ReviewStartResponse;
import com.wordflow.module.review.service.ReviewService;
import com.wordflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 复习接口。
 */
@Tag(name = "复习")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "复习概览（待复习数量）")
    @GetMapping("/overview")
    public Result<ReviewOverviewResponse> overview() {
        return Result.ok(reviewService.overview(UserContext.getUserId()));
    }

    @Operation(summary = "开始复习（按艾宾浩斯曲线生成短文）")
    @PostMapping("/start")
    public Result<ReviewStartResponse> start() {
        return Result.ok(reviewService.startReview(UserContext.getUserId()));
    }

    @Operation(summary = "夜间学习智能弹窗（白天登录时询问是否把凌晨背的词提前加入今日复习）")
    @GetMapping("/night-prompt")
    public Result<NightPromptResponse> nightPrompt() {
        return Result.ok(reviewService.nightPrompt(UserContext.getUserId()));
    }

    @Operation(summary = "答复夜间学习智能弹窗")
    @PostMapping("/night-prompt")
    public Result<Void> answerNightPrompt(@RequestBody NightPromptRequest request) {
        reviewService.answerNightPrompt(UserContext.getUserId(), request.apply());
        return Result.ok(null);
    }

    @Operation(summary = "批改复习短文")
    @PostMapping("/check")
    public Result<ArticleCheckResult> check(@RequestBody ArticleCheckRequest request) {
        return Result.ok(reviewService.checkArticle(UserContext.getUserId(), request));
    }
}
