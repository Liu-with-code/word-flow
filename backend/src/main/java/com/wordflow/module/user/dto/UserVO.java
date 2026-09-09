package com.wordflow.module.user.dto;

import java.time.LocalDateTime;

/**
 * 用户信息出参（不含密码等敏感字段）。
 */
public record UserVO(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        Integer dailyWordGoal,
        Integer dailyReviewGoal,
        Long activeBookId,
        String timezone,
        Integer dayBoundaryHour,
        Integer nightCutoffHour,
        LocalDateTime lastLoginAt,
        LocalDateTime setupPromptAt,
        LocalDateTime createdAt
) {
}
