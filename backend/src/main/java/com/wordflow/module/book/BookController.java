package com.wordflow.module.book;

import com.wordflow.common.Result;
import com.wordflow.module.book.dto.BookVO;
import com.wordflow.module.book.service.BookService;
import com.wordflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 词书接口。
 *
 * 模块职责：
 *   - 词书列表、用户选择词书。
 */
@Tag(name = "词书")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "词书列表（含当前选中标记）")
    @GetMapping
    public Result<List<BookVO>> list() {
        return Result.ok(bookService.listBooks(UserContext.getUserId()));
    }

    @Operation(summary = "选择当前词书")
    @PostMapping("/{id}/select")
    public Result<Void> select(@PathVariable Long id) {
        bookService.selectBook(UserContext.getUserId(), id);
        return Result.ok(null);
    }
}
