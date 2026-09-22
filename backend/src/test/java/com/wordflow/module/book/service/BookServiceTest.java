package com.wordflow.module.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wordflow.common.BusinessException;
import com.wordflow.module.book.dto.BookVO;
import com.wordflow.module.book.entity.Book;
import com.wordflow.module.book.mapper.BookMapper;
import com.wordflow.module.learning.entity.LearningPlan;
import com.wordflow.module.learning.entity.LearningPlanWord;
import com.wordflow.module.learning.mapper.LearningPlanMapper;
import com.wordflow.module.learning.mapper.LearningPlanWordMapper;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.mapper.WordProgressMapper;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.mapper.WordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 词书服务单元测试。
 *
 * 覆盖：列表与当前选中标记、每本书进度计算、选择词书、不存在校验、重新背诵清理范围。
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private WordMapper wordMapper;

    @Mock
    private WordProgressMapper progressMapper;

    @Mock
    private LearningPlanMapper planMapper;

    @Mock
    private LearningPlanWordMapper planWordMapper;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookMapper, userMapper, wordMapper,
                progressMapper, planMapper, planWordMapper);
    }

    @Test
    void shouldMarkActiveBook_whenListingBooks() {
        Book cet4 = book(1L, "CET4");
        Book cet6 = book(2L, "CET6");
        when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(cet4, cet6));
        when(userMapper.selectById(1L)).thenReturn(user(2L));
        when(progressMapper.countByBook(1L)).thenReturn(List.of());

        List<BookVO> books = bookService.listBooks(1L);

        assertThat(books).hasSize(2);
        assertThat(books.get(0).active()).isFalse();
        assertThat(books.get(1).active()).isTrue();
    }

    @Test
    void shouldComputeProgressPerBook_independently() {
        Book cet4 = book(1L, "CET4");
        Book cet6 = book(2L, "CET6");
        when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(cet4, cet6));
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        // CET4：已学 25 / 100，其中 5 个已掌握；CET6：无记录
        when(progressMapper.countByBook(1L)).thenReturn(List.of(
                Map.of("bookId", 1L, "started", 25L, "mastered", 5L)));

        List<BookVO> books = bookService.listBooks(1L);

        BookVO cet4VO = books.get(0);
        assertThat(cet4VO.learnedCount()).isEqualTo(25);
        assertThat(cet4VO.masteredCount()).isEqualTo(5);
        assertThat(cet4VO.remainingCount()).isEqualTo(75);
        assertThat(cet4VO.progressPercent()).isEqualTo(25);

        BookVO cet6VO = books.get(1);
        assertThat(cet6VO.learnedCount()).isZero();
        assertThat(cet6VO.progressPercent()).isZero();
        assertThat(cet6VO.remainingCount()).isEqualTo(100);
    }

    @Test
    void shouldSelectBook_whenEnabled() {
        Book book = book(2L, "CET6");
        when(bookMapper.selectById(2L)).thenReturn(book);
        User user = user(1L);
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

    @Test
    void shouldClearBookProgress_whenRestart() {
        Book book = book(1L, "CET4");
        when(bookMapper.selectById(1L)).thenReturn(book);
        when(wordMapper.selectIdsByBookId(1L)).thenReturn(List.of(10L, 11L));
        // 今日无进行中计划
        when(planMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(book));
        when(progressMapper.countByBook(1L)).thenReturn(List.of());

        List<BookVO> books = bookService.restartBook(1L, 1L);

        // 删除的进度范围限定在该词书的单词上
        verify(progressMapper).delete(any(LambdaQueryWrapper.class));
        assertThat(books).hasSize(1);
        assertThat(books.get(0).learnedCount()).isZero();
        assertThat(books.get(0).progressPercent()).isZero();
    }

    @Test
    void shouldDeleteEmptyPlan_whenRestartClearsTodayPlanWords() {
        Book book = book(1L, "CET4");
        when(bookMapper.selectById(1L)).thenReturn(book);
        when(wordMapper.selectIdsByBookId(1L)).thenReturn(List.of(10L));
        LearningPlan plan = new LearningPlan();
        plan.setId(7L);
        plan.setUserId(1L);
        plan.setStatus("IN_PROGRESS");
        when(planMapper.selectOne(any())).thenReturn(plan);
        LearningPlanWord planWord = new LearningPlanWord();
        planWord.setPlanId(7L);
        planWord.setWordId(10L);
        when(planWordMapper.selectList(any())).thenReturn(List.of(planWord));
        // 清理后计划已无单词
        when(planWordMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(book));
        when(progressMapper.countByBook(1L)).thenReturn(List.of());

        bookService.restartBook(1L, 1L);

        verify(planWordMapper).delete(any(LambdaQueryWrapper.class));
        verify(planMapper).deleteById(7L);
    }

    @Test
    void shouldKeepPlan_whenNotInProgress() {
        Book book = book(1L, "CET4");
        when(bookMapper.selectById(1L)).thenReturn(book);
        when(wordMapper.selectIdsByBookId(1L)).thenReturn(List.of(10L));
        LearningPlan plan = new LearningPlan();
        plan.setId(7L);
        plan.setStatus("COMPLETED");
        when(planMapper.selectOne(any())).thenReturn(plan);
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(book));
        when(progressMapper.countByBook(1L)).thenReturn(List.of());

        bookService.restartBook(1L, 1L);

        // 已完成计划视为历史记录，保留不动
        verify(planWordMapper, never()).delete(any());
        verify(planMapper, never()).deleteById(anyLong());
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

    private Word word(Long id) {
        Word word = new Word();
        word.setId(id);
        word.setBookId(1L);
        word.setWord("word" + id);
        return word;
    }

    private User user(Long activeBookId) {
        User user = new User();
        user.setId(1L);
        user.setActiveBookId(activeBookId);
        return user;
    }
}
