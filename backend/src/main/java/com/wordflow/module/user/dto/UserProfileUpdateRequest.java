package com.wordflow.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 个人资料更新请求体。
 */
public record UserProfileUpdateRequest(
        @Size(max = 50, message = "昵称最长 50 字符") String nickname,
        @Size(max = 255, message = "头像地址最长 255 字符") String avatarUrl,
        @Min(value = 1, message = "每日目标最少 1 个") @Max(value = 100, message = "每日目标最多 100 个")
        Integer dailyWordGoal,
        @Min(value = 1, message = "复习目标最少 1 个") @Max(value = 100, message = "复习目标最多 100 个")
        Integer dailyReviewGoal,
        @Size(max = 64, message = "时区标识最长 64 字符") String timezone,
        @Min(value = 0, message = "日边界小时 0-23") @Max(value = 23, message = "日边界小时 0-23")
        Integer dayBoundaryHour,
        @Min(value = 0, message = "夜间判定截止小时 0-12") @Max(value = 12, message = "夜间判定截止小时 0-12")
        Integer nightCutoffHour
) {
}
