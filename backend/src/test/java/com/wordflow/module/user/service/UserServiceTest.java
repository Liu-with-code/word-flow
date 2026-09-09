package com.wordflow.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wordflow.common.BusinessException;
import com.wordflow.module.user.dto.UserProfileUpdateRequest;
import com.wordflow.module.user.dto.UserVO;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务单元测试。
 *
 * 覆盖：注册重名校验、登录成功/失败、资料更新与 VO 映射。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @org.junit.jupiter.api.io.TempDir
    private Path tempDir;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, tempDir.toString());
    }

    @Test
    void shouldThrowConflict_whenUsernameAlreadyExists() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> userService.register("alice", "123456", "爱丽丝"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已被占用");
    }

    @Test
    void shouldRegister_whenUsernameAvailable() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        User user = userService.register("alice", "123456", "爱丽丝");

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getNickname()).isEqualTo("爱丽丝");
        assertThat(user.getDailyWordGoal()).isEqualTo(20);
        assertThat(user.getDailyReviewGoal()).isEqualTo(20);
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(user.getPasswordHash()).isNotEqualTo("123456");
    }

    @Test
    void shouldLoginSuccess_whenPasswordMatches() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("alice");
        existing.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("123456"));
        existing.setStatus(1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        User user = userService.login("alice", "123456");

        assertThat(user.getId()).isEqualTo(1L);
        verify(userMapper).updateById(existing);
        assertThat(existing.getLastLoginAt()).isNotNull();
    }

    @Test
    void shouldRejectLogin_whenPasswordWrong() {
        User existing = new User();
        existing.setUsername("alice");
        existing.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("correct"));
        existing.setStatus(1);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThatThrownBy(() -> userService.login("alice", "wrong"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void shouldUpdateProfile_whenFieldsProvided() {
        User existing = new User();
        existing.setId(1L);
        existing.setNickname("旧昵称");
        existing.setTimezone("Asia/Shanghai");
        existing.setDayBoundaryHour(0);
        existing.setNightCutoffHour(6);
        when(userMapper.selectById(1L)).thenReturn(existing);

        UserVO vo = userService.updateProfile(1L, new UserProfileUpdateRequest(
                "新昵称", null, 30, 10, "America/New_York", 4, 6));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getNickname()).isEqualTo("新昵称");
        assertThat(saved.getDailyWordGoal()).isEqualTo(30);
        assertThat(saved.getTimezone()).isEqualTo("America/New_York");
        assertThat(saved.getDayBoundaryHour()).isEqualTo(4);
        assertThat(saved.getDailyReviewGoal()).isEqualTo(10);
        assertThat(vo.timezone()).isEqualTo("America/New_York");
        assertThat(vo.dayBoundaryHour()).isEqualTo(4);
        assertThat(vo.dailyReviewGoal()).isEqualTo(10);
    }

    @Test
    void shouldUploadAvatar_saveFileAndUpdateProfile() throws IOException {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("alice");
        existing.setNickname("爱丽丝");
        existing.setTimezone("Asia/Shanghai");
        when(userMapper.selectById(1L)).thenReturn(existing);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        UserVO vo = userService.updateAvatar(1L, file);

        assertThat(vo.avatarUrl()).startsWith("/uploads/avatar/avatar_1_").endsWith(".png");
        assertThat(existing.getAvatarUrl()).isEqualTo(vo.avatarUrl());
        assertThat(tempDir.resolve("avatar")).isDirectory();
        verify(userMapper).updateById(existing);
    }

    @Test
    void shouldRejectAvatar_whenNotImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> userService.updateAvatar(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }
}
