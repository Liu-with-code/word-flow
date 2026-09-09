package com.wordflow.module.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wordflow.common.BusinessException;
import com.wordflow.common.ResultCode;
import com.wordflow.module.book.dto.BookVO;
import com.wordflow.module.book.entity.Book;
import com.wordflow.module.book.mapper.BookMapper;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 词书服务。
 *
 * 模块职责：
 *   - 词书列表（标记当前用户选中的词书）；
 *   - 用户切换词书（更新 sys_user.active_book_id）；
 *   - 按 ID 校验词书是否存在。
 * 你需要完成：
 *   - 词书封面图片上传/外链管理（目前用 cover_color 替代封面图）；
 *   - 词书内单词量较大时，可在列表中额外返回「已学/未学」统计。
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    public List<BookVO> listBooks(Long userId) {
        Long activeBookId = resolveActiveBookId(userId);
        List<Book> books = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1)
                        .orderByAsc(Book::getSortNo)
                        .orderByAsc(Book::getId));
        return books.stream()
                .map(book -> toVO(book, Objects.equals(book.getId(), activeBookId)))
                .toList();
    }

    /** 用户选择某本词书作为当前词书。 */
    public void selectBook(Long userId, Long bookId) {
        Book book = getById(bookId);
        if (book.getStatus() == null || book.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "词书不存在或已停用");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setActiveBookId(bookId);
        userMapper.updateById(user);
    }

    public Book getById(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "词书不存在");
        }
        return book;
    }

    public BookVO toVO(Book book, boolean active) {
        return new BookVO(
                book.getId(),
                book.getName(),
                book.getCode(),
                book.getLevel(),
                book.getDescription(),
                book.getCoverColor(),
                book.getWordCount(),
                book.getSortNo(),
                active);
    }

    private Long resolveActiveBookId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user.getActiveBookId() == null ? 1L : user.getActiveBookId();
    }
}
