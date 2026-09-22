package com.wordflow.module.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordflow.common.BusinessException;
import com.wordflow.common.ResultCode;
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
import com.wordflow.module.word.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 词书服务：列表（含每本书的学习进度）、切换当前词书、重新背诵。
 *
 * 进度按「账号 + 单词」维度统计，与当前选中词书无关，
 * 因此切换词书不会清空任何一本书的进度。
 */
@Service
@RequiredArgsConstructor
public class BookService {

    /** 词书启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 学习计划状态：进行中 */
    private static final String PLAN_STATUS_IN_PROGRESS = "IN_PROGRESS";

    /** 百分比换算基数 */
    private static final int PERCENT_BASE = 100;

    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final LearningPlanMapper planMapper;
    private final LearningPlanWordMapper planWordMapper;

    /**
     * 词书列表：一次返回全部词书的进度，前端切换词书时无需重新拉取。
     */
    public List<BookVO> listBooks(Long userId) {
        Long activeBookId = resolveActiveBookId(userId);
        List<Book> books = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, STATUS_ENABLED)
                        .orderByAsc(Book::getSortNo)
                        .orderByAsc(Book::getId));
        Map<Long, BookProgress> progressMap = loadBookProgress(userId);
        return books.stream()
                .map(book -> toVO(book,
                        Objects.equals(book.getId(), activeBookId),
                        progressMap.getOrDefault(book.getId(), BookProgress.EMPTY)))
                .toList();
    }

    public void selectBook(Long userId, Long bookId) {
        Book book = getById(bookId);
        if (book.getStatus() == null || book.getStatus() != STATUS_ENABLED) {
            throw new BusinessException(ResultCode.NOT_FOUND, "词书不存在或已停用");
        }
        User user = requireUser(userId);
        user.setActiveBookId(bookId);
        userMapper.updateById(user);
    }

    /**
     * 重新背诵指定词书：清空当前账号在本书的学习进度，使其重新可学。
     *
     * 仅清除该词书的进度行与今日「进行中」计划中属于该词书的明细；
     * 其他词书进度不受影响，已完成的计划、作答流水与已生成短文保留。
     * 若清理后今日计划已无单词，则删除该计划，下次开始学习时按新进度重新抽词。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<BookVO> restartBook(Long userId, Long bookId) {
        getById(bookId);
        List<Long> wordIds = listBookWordIds(bookId);
        if (!wordIds.isEmpty()) {
            clearTodayPlanWords(userId, wordIds);
            progressMapper.delete(Wrappers.<WordProgress>lambdaQuery()
                    .eq(WordProgress::getUserId, userId)
                    .in(WordProgress::getWordId, wordIds));
        }
        return listBooks(userId);
    }

    public Book getById(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "词书不存在");
        }
        return book;
    }

    public BookVO toVO(Book book, boolean active, BookProgress progress) {
        long total = book.getWordCount() == null ? 0L : book.getWordCount();
        long learned = progress.started();
        long remaining = total > learned ? total - learned : 0L;
        int percent = total <= 0 ? 0 : (int) Math.round(learned * PERCENT_BASE / (double) total);
        return new BookVO(
                book.getId(),
                book.getName(),
                book.getCode(),
                book.getLevel(),
                book.getDescription(),
                book.getCoverColor(),
                book.getWordCount(),
                book.getSortNo(),
                active,
                learned,
                progress.mastered(),
                remaining,
                Math.min(PERCENT_BASE, Math.max(0, percent)));
    }

    /** 从今日「进行中」计划里移除属于该词书的明细；计划被清空则一并删除。 */
    private void clearTodayPlanWords(Long userId, List<Long> bookWordIds) {
        LearningPlan plan = planMapper.selectOne(
                Wrappers.<LearningPlan>lambdaQuery()
                        .eq(LearningPlan::getUserId, userId)
                        .eq(LearningPlan::getPlanDate, LocalDate.now()));
        if (plan == null || !PLAN_STATUS_IN_PROGRESS.equals(plan.getStatus())) {
            return;
        }
        List<Long> planWordIds = planWordMapper.selectList(
                        Wrappers.<LearningPlanWord>lambdaQuery()
                                .eq(LearningPlanWord::getPlanId, plan.getId()))
                .stream()
                .map(LearningPlanWord::getWordId)
                .filter(bookWordIds::contains)
                .toList();
        if (planWordIds.isEmpty()) {
            return;
        }
        planWordMapper.delete(Wrappers.<LearningPlanWord>lambdaQuery()
                .eq(LearningPlanWord::getPlanId, plan.getId())
                .in(LearningPlanWord::getWordId, planWordIds));
        Long remain = planWordMapper.selectCount(Wrappers.<LearningPlanWord>lambdaQuery()
                .eq(LearningPlanWord::getPlanId, plan.getId()));
        if (remain == null || remain == 0) {
            planMapper.deleteById(plan.getId());
        }
    }

    /** 查询当前用户在各词书上的进度（未出现的词书视为 0）。 */
    private Map<Long, BookProgress> loadBookProgress(Long userId) {
        List<Map<String, Object>> rows = progressMapper.countByBook(userId);
        Map<Long, BookProgress> result = new HashMap<>(Math.max(4, rows.size() * 2));
        for (Map<String, Object> row : rows) {
            Long bookId = asLong(row.get("bookId"));
            if (bookId == null) {
                continue;
            }
            result.put(bookId, new BookProgress(asLong(row.get("started")), asLong(row.get("mastered"))));
        }
        return result;
    }

    private List<Long> listBookWordIds(Long bookId) {
        return wordMapper.selectIdsByBookId(bookId);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private Long resolveActiveBookId(Long userId) {
        User user = requireUser(userId);
        return user.getActiveBookId() == null ? 1L : user.getActiveBookId();
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    /** 单本词书的进度值对象。 */
    public record BookProgress(long started, long mastered) {

        /** 无进度（未学过该词书） */
        public static final BookProgress EMPTY = new BookProgress(0L, 0L);
    }
}
