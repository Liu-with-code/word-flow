package com.wordflow.module.word.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wordflow.common.BusinessException;
import com.wordflow.common.PageResult;
import com.wordflow.module.word.dto.WordVO;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.mapper.WordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 单词服务单元测试。
 *
 * 覆盖：分页查询、按词书抽词、详情不存在校验、VO 映射。
 */
@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordMapper wordMapper;

    private WordService wordService;

    @BeforeEach
    void setUp() {
        wordService = new WordService(wordMapper);
    }

    @Test
    void shouldPageWords_withBookFilter() {
        Word word = word(1L, 1L, "ability", "能力");
        Page<Word> page = new Page<>(1, 10);
        page.setRecords(List.of(word));
        page.setTotal(1);
        when(wordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<WordVO> result = wordService.page(null, 1L, null, 1, 10);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).word()).isEqualTo("ability");
        assertThat(result.getList().get(0).bookId()).isEqualTo(1L);
    }

    @Test
    void shouldPickGoalWords_whenEnoughUnlearnedWords() {
        List<Word> words = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> word((long) i, 2L, "word" + i, "释义" + i))
                .toList();
        when(wordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>(words));

        List<Word> picked = wordService.pickNewWords(1L, 3, 2L);

        assertThat(picked).hasSize(3);
        assertThat(picked).allMatch(w -> w.getBookId() == 2L);
        assertThat(picked).allSatisfy(w -> assertThat(words).contains(w));
    }

    @Test
    void shouldReturnAll_whenLessThanGoal() {
        List<Word> words = List.of(word(1L, 1L, "a", "甲"), word(2L, 1L, "b", "乙"));
        when(wordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>(words));

        List<Word> picked = wordService.pickNewWords(1L, 10, 1L);

        assertThat(picked).hasSize(2);
    }

    @Test
    void shouldThrowNotFound_whenWordMissing() {
        when(wordMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> wordService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("单词不存在");
    }

    private Word word(Long id, Long bookId, String text, String chinese) {
        Word word = new Word();
        word.setId(id);
        word.setBookId(bookId);
        word.setWord(text);
        word.setChinese(chinese);
        return word;
    }
}
