package com.wordflow.module.statistics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordflow.module.learning.entity.LearningPlan;
import com.wordflow.module.learning.entity.LearningRecord;
import com.wordflow.module.learning.mapper.LearningPlanMapper;
import com.wordflow.module.learning.mapper.LearningRecordMapper;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.statistics.dto.StatisticsDtos.DashboardStatsResponse;
import com.wordflow.module.statistics.dto.StatisticsDtos.RecentRecordVO;
import com.wordflow.module.statistics.dto.StatisticsDtos.WeeklyPoint;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务。
 *
 * 模块职责：
 *   - 首页仪表盘数据、近 7 天学习量、最近作答记录。
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ProgressService progressService;
    private final LearningPlanMapper planMapper;
    private final LearningRecordMapper recordMapper;
    private final WordMapper wordMapper;

    public DashboardStatsResponse dashboard(Long userId) {
        LocalDate today = progressService.currentStudyDay(userId);
        LearningPlan todayPlan = planMapper.selectOne(
                Wrappers.<LearningPlan>lambdaQuery()
                        .eq(LearningPlan::getUserId, userId)
                        .eq(LearningPlan::getPlanDate, today));
        long correct = recordMapper.countCorrect(userId);
        long total = recordMapper.countTotal(userId);
        double accuracy = total == 0 ? 0 : Math.round(correct * 1000.0 / total) / 10.0;
        return new DashboardStatsResponse(
                progressService.countLearned(userId),
                progressService.countComplete(userId),
                progressService.countDue(userId),
                todayPlan == null ? 0 : todayPlan.getNewWordCount(),
                todayPlan == null ? 0 : todayPlan.getCompletedCount(),
                countStreakDays(userId),
                accuracy);
    }

    public List<WeeklyPoint> weekly(Long userId) {
        LocalDate today = progressService.currentStudyDay(userId);
        LocalDateTime since = today.minusDays(6).atStartOfDay();
        List<Map<String, Object>> rows = recordMapper.countByDay(userId, since);
        Map<String, Long> byDay = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), ((Number) row.get("cnt")).longValue());
        }
        List<WeeklyPoint> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            result.add(new WeeklyPoint(day.toString(), byDay.getOrDefault(day.toString(), 0L)));
        }
        return result;
    }

    public List<RecentRecordVO> recent(Long userId, int limit) {
        return recordMapper.selectRecent(userId, Math.min(20, Math.max(1, limit))).stream()
                .map(this::toRecentVO)
                .toList();
    }

    private RecentRecordVO toRecentVO(LearningRecord record) {
        Word word = wordMapper.selectById(record.getWordId());
        return new RecentRecordVO(
                word == null ? "已删除单词" : word.getWord(),
                record.getStepType(),
                record.getIsCorrect() != null && record.getIsCorrect() == 1,
                record.getCreatedAt());
    }

    /** 连续学习天数：从今天（或昨天）开始向前连续统计已完成的计划日期。 */
    private int countStreakDays(Long userId) {
        List<LocalDate> dates = progressService.listLearnedDays(userId);
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate expected = progressService.currentStudyDay(userId);
        if (!dates.get(0).equals(expected) && !dates.get(0).equals(expected.minusDays(1))) {
            return 0;
        }
        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected) || date.equals(expected.minusDays(1))) {
                streak++;
                expected = date.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
