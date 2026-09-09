package com.wordflow.module.word;

import com.wordflow.common.PageResult;
import com.wordflow.common.Result;
import com.wordflow.module.word.dto.WordVO;
import com.wordflow.module.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 词库接口。
 *
 * 模块职责：
 *   - 提供词库查询能力（前端「词库」页面使用）。
 * 你需要完成：
 *   - 管理端词库维护接口。
 */
@Tag(name = "词库")
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @Operation(summary = "分页查询词库")
    @GetMapping
    public Result<PageResult<WordVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(wordService.page(keyword, bookId, level, page, size));
    }

    @Operation(summary = "查询单词详情")
    @GetMapping("/{id}")
    public Result<WordVO> detail(@PathVariable Long id) {
        return Result.ok(wordService.toVO(wordService.getById(id)));
    }
}
