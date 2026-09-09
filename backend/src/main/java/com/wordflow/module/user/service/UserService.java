package com.wordflow.module.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordflow.common.BusinessException;
import com.wordflow.common.ResultCode;
import com.wordflow.module.user.dto.UserProfileUpdateRequest;
import com.wordflow.module.user.dto.UserVO;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 用户服务。
 *
 * 模块职责：
 *   - 注册、登录校验、用户资料查询与更新。
 * 你需要完成：
 *   - 头像上传：建议接入 OSS/本地静态目录，并在 updateProfile 中更新 avatarUrl。
 *   - 找回密码/修改密码流程。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final String uploadDir;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    public UserService(UserMapper userMapper,
                       @Value("${wordflow.upload-dir:uploads}") String uploadDir) {
        this.userMapper = userMapper;
        this.uploadDir = uploadDir;
    }

    public User register(String username, String rawPassword, String nickname) {
        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已被占用");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setAvatarUrl("");
        user.setDailyWordGoal(20);
        user.setDailyReviewGoal(20);
        user.setLastLoginAt(LocalDateTime.now());
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String rawPassword) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    public UserVO updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getById(userId);
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.dailyWordGoal() != null) {
            if (request.dailyWordGoal() < 1 || request.dailyWordGoal() > 100) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "每日目标须在 1-100 之间");
            }
            user.setDailyWordGoal(request.dailyWordGoal());
        }
        if (request.dailyReviewGoal() != null) {
            if (request.dailyReviewGoal() < 1 || request.dailyReviewGoal() > 100) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "复习目标须在 1-100 之间");
            }
            user.setDailyReviewGoal(request.dailyReviewGoal());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            user.setTimezone(request.timezone().trim());
        }
        if (request.dayBoundaryHour() != null) {
            user.setDayBoundaryHour(request.dayBoundaryHour());
        }
        if (request.nightCutoffHour() != null) {
            user.setNightCutoffHour(request.nightCutoffHour());
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    /** 上传本地头像：保存文件并更新 avatarUrl。 */
    public UserVO updateAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片不能超过 2MB");
        }
        String ext = resolveImageExtension(file);
        try {
            Path dir = Path.of(uploadDir, "avatar").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;
            Path target = dir.resolve(filename).normalize();
            file.transferTo(target);

            User user = getById(userId);
            user.setAvatarUrl("/uploads/avatar/" + filename);
            userMapper.updateById(user);
            return toVO(user);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "头像保存失败，请重试");
        }
    }

    /** 记录设置弹窗已处理（保存或暂不设置），服务端持久化，避免每次打开重复弹出。 */
    public UserVO markSetupPromptSeen(Long userId) {
        User user = getById(userId);
        user.setSetupPromptAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toVO(user);
    }

    public UserVO toVO(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getDailyWordGoal(),
                user.getDailyReviewGoal(),
                user.getActiveBookId(),
                user.getTimezone(),
                user.getDayBoundaryHour(),
                user.getNightCutoffHour(),
                user.getLastLoginAt(),
                user.getSetupPromptAt(),
                user.getCreatedAt());
    }

    private String resolveImageExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String type = contentType.toLowerCase();
            if (type.contains("jpeg") || type.contains("jpg")) {
                return ".jpg";
            }
            if (type.contains("png")) {
                return ".png";
            }
            if (type.contains("webp")) {
                return ".webp";
            }
            if (type.contains("gif")) {
                return ".gif";
            }
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return ".jpg";
            }
            if (lower.endsWith(".png")) {
                return ".png";
            }
            if (lower.endsWith(".webp")) {
                return ".webp";
            }
            if (lower.endsWith(".gif")) {
                return ".gif";
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 JPG/PNG/WebP/GIF 格式图片");
    }
}
