package com.wordflow.module.word.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wordflow.common.BusinessException;
import com.wordflow.common.PageResult;
import com.wordflow.common.ResultCode;
import com.wordflow.module.word.dto.WordVO;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 单词服务。
 *
 * 模块职责：
 *   - 词库分页查询、按 ID 查询、抽取今日未学单词。
 */
@Service
@RequiredArgsConstructor
public class WordService {

    private final WordMapper wordMapper;

    public PageResult<WordVO> page(String keyword, Long bookId, String level, long page, long size) {
        LambdaQueryWrapper<Word> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Word::getWord, keyword.trim())
                    .or().like(Word::getChinese, keyword.trim()));
        }
        if (bookId != null) {
            wrapper.eq(Word::getBookId, bookId);
        }
        if (StringUtils.hasText(level)) {
            wrapper.eq(Word::getLevel, level.trim());
        }
        wrapper.orderByAsc(Word::getWord);
        Page<Word> result = wordMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.convert(this::toVO));
    }

    public Word getById(Long wordId) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "单词不存在");
        }
        return word;
    }

    /**
     * 随机抽取用户尚未学过的单词。
     *
     * 实现说明：先查全部未学单词再在内存中洗牌，避免直接拼接 RAND() 的
     * SQL 注入风险，同时保证数量可控（目标 <= 100）。
     */
    public List<Word> pickNewWords(Long userId, int goal, Long bookId) {
        List<Word> all = wordMapper.selectList(
                new LambdaQueryWrapper<Word>()
                        .eq(Word::getBookId, bookId)
                        .apply("NOT EXISTS (SELECT 1 FROM learn_progress p "
                                + "WHERE p.user_id = {0} "
                                + "AND p.word_id = learn_word.id)", userId));
        Collections.shuffle(all);
        return all.size() <= goal ? all : all.subList(0, goal);
    }

    public WordVO toVO(Word word) {
        return new WordVO(
                word.getId(),
                word.getBookId(),
                word.getWord(),
                word.getPhonetic(),
                word.getChinese(),
                word.getPartOfSpeech(),
                word.getExampleEn(),
                word.getExampleZh(),
                word.getDifficulty(),
                word.getLevel());
    }
}
