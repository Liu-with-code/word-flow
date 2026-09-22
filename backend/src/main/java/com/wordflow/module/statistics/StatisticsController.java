package com.wordflow.module.statistics;

import com.wordflow.common.PageResult;
import com.wordflow.common.Result;
import com.wordflow.module.statistics.dto.StatisticsDtos.DashboardStatsResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.RecentRecordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.ReviewForecastResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.StageDistributionResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeakWordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeeklyPoint;
import com.wordflow.module.statistics.service.StatisticsService;
import com.wordflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口。
 */
@Tag(name = "统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "首页仪表盘统计")
    @GetMapping("/dashboard")
    public Result<DashboardStatsResponse> dashboard() {
        return Result.ok(statisticsService.dashboard(UserContext.getUserId()));
    }

    @Operation(summary = "近 7 天学习量")
    @GetMapping("/weekly")
    public Result<List<WeeklyPoint>> weekly() {
        return Result.ok(statisticsService.weekly(UserContext.getUserId()));
    }

    @Operation(summary = "最近作答记录")
    @GetMapping("/recent")
    public Result<List<RecentRecordVO>> recent(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statisticsService.recent(UserContext.getUserId(), limit));
    }

    @Operation(summary = "记忆阶段分布（艾宾浩斯各阶段单词数量）")
    @GetMapping("/stage-distribution")
    public Result<StageDistributionResponse> stageDistribution() {
        return Result.ok(statisticsService.stageDistribution(UserContext.getUserId()));
    }

    @Operation(summary = "未来复习量预测（按用户学习日统计每日到期复习单词数）")
    @GetMapping("/review-forecast")
    public Result<ReviewForecastResponse> reviewForecast(@RequestParam(defaultValue = "14") int days) {
        return Result.ok(statisticsService.reviewForecast(UserContext.getUserId(), days));
    }

    @Operation(summary = "薄弱单词概览（答错次数最多的单词）")
    @GetMapping("/weak-words")
    public Result<List<WeakWordVO>> weakWords(@RequestParam(defaultValue = "true") boolean onlyWrong,
                                              @RequestParam(defaultValue = "6") int limit) {
        return Result.ok(statisticsService.weakWords(UserContext.getUserId(), onlyWrong, limit));
    }

    @Operation(summary = "错题本分页查询")
    @GetMapping("/weak-words/page")
    public Result<PageResult<WeakWordVO>> weakWordPage(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return Result.ok(statisticsService.weakWordPage(UserContext.getUserId(), page, size));
    }
}
