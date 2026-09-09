package com.wordflow.module.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体，对应表 sys_user。
 *
 * 模块职责：
 *   - 用户基础信息：登录名、密码哈希、昵称、每日目标等。
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 哈希，禁止明文存储 */
    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    private Integer dailyWordGoal;

    /** 每日复习目标数 */
    private Integer dailyReviewGoal;

    /** 用户当前选择的词书ID */
    private Long activeBookId;

    /** 用户时区（IETF BCP 47，如 Asia/Shanghai） */
    private String timezone;

    /** 复习日边界小时 0-23：凌晨该点前背的词算前一天（0=不启用） */
    private Integer dayBoundaryHour;

    /** 夜间学习判定截止小时 0-12（触发智能弹窗的时间窗） */
    private Integer nightCutoffHour;

    /** 最近一次夜间学习智能提示日期（避免重复弹窗） */
    private LocalDate nightPromptDate;

    /** 最近登录时间（用于欢迎弹窗与长期未登录判定） */
    private LocalDateTime lastLoginAt;

    /** 最近一次设置弹窗处理时间（保存或暂不设置均记录，用于避免重复弹窗） */
    private LocalDateTime setupPromptAt;

    /** 1-启用 0-禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
