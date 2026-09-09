package com.wordflow.module.user;

import com.wordflow.common.Result;
import com.wordflow.module.user.dto.UserProfileUpdateRequest;
import com.wordflow.module.user.dto.UserVO;
import com.wordflow.module.user.service.UserService;
import com.wordflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户信息接口。
 *
 * 模块职责：
 *   - 查询当前登录用户信息、更新个人资料。
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.toVO(userService.getById(UserContext.getUserId())));
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return Result.ok(userService.updateProfile(UserContext.getUserId(), request));
    }

    @Operation(summary = "上传头像（本地图片）")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.updateAvatar(UserContext.getUserId(), file));
    }

    @Operation(summary = "记录设置弹窗已处理（保存或暂不设置）")
    @PostMapping("/setup-prompt-seen")
    public Result<UserVO> markSetupPromptSeen() {
        return Result.ok(userService.markSetupPromptSeen(UserContext.getUserId()));
    }
}
