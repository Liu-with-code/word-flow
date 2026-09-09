package com.wordflow.module.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wordflow.common.BusinessException;
import com.wordflow.module.book.dto.BookVO;
import com.wordflow.module.book.entity.Book;
import com.wordflow.module.book.mapper.BookMapper;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 词书服务单元测试。
 *
 * 覆盖：列表与当前选中标记、选择词书、不存在校验。
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserMapper userMapper;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookMapper, userMapper);
    }

    @Test
    void shouldMarkActiveBook_whenListingBooks() {
        Book cet4 = book(1L, "CET4");
        Book cet6 = book(2L, "CET6");
        User user = new User();
        user.setId(1L);
        user.setActiveBookId(2L);
        when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(cet4, cet6));
        when(userMapper.selectById(1L)).thenReturn(user);

        List<BookVO> books = bookService.listBooks(1L);

        assertThat(books).hasSize(2);
        assertThat(books.get(0).active()).isFalse();
        assertThat(books.get(1).active()).isTrue();
    }

    @Test
    void shouldSelectBook_whenEnabled() {
        Book book = book(2L, "CET6");
        book.setStatus(1);
        User user = new User();
        user.setId(1L);
        user.setActiveBookId(1L);
        when(bookMapper.selectById(2L)).thenReturn(book);
        when(userMapper.selectById(1L)).thenReturn(user);

        bookService.selectBook(1L, 2L);

        verify(userMapper).updateById(user);
        assertThat(user.getActiveBookId()).isEqualTo(2L);
    }

    @Test
    void shouldThrowNotFound_whenBookMissing() {
        when(bookMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> bookService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("词书不存在");
    }

    private Book book(Long id, String code) {
        Book book = new Book();
        book.setId(id);
        book.setCode(code);
        book.setName(code + "核心词汇");
        book.setStatus(1);
        book.setWordCount(100);
        return book;
    }
}
