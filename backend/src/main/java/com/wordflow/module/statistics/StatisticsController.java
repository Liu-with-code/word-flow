package com.wordflow.module.statistics;

import com.wordflow.common.Result;
import com.wordflow.module.statistics.dto.StatisticsDtos.DashboardStatsResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.RecentRecordVO;
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
}
