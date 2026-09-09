package com.wordflow.module.review.service;

import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.article.entity.LearningArticle;
import com.wordflow.module.article.service.ArticleService;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.review.dto.ReviewDtos.ReviewOverviewResponse;
import com.wordflow.module.review.dto.ReviewDtos.NightPromptResponse;
import com.wordflow.module.review.dto.ReviewDtos.ReviewStartResponse;
import com.wordflow.module.review.dto.ReviewDtos.WordFrequencyVO;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import com.wordflow.module.word.dto.WordVO;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.service.WordService;
import com.wordflow.module.learning.service.LearningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 复习服务。
 *
 * 模块职责：
 *   - 按艾宾浩斯曲线选出到期单词；
 *   - 计算每个单词的紧急度（优先级），让 AI 生成文章时高频复现易忘词；
 *   - 批改通过后推进每个单词的复习阶段。
 *
 * 优先级规则（可调）：
 *   基础 1 + 逾期 1 天加 1（最多 +2）+ 早期阶段(1-2 轮)加 1，最高 4。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProgressService progressService;
    private final WordService wordService;
    private final ArticleService articleService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final Clock clock;

    public ReviewOverviewResponse overview(Long userId) {
        return new ReviewOverviewResponse((int) progressService.countDue(userId));
    }

    /**
     * 开始一次复习：选出到期单词 -> 按紧急度排序 -> AI 生成短文。
     * 若没有到期单词，返回 hasWords=false，前端展示「今日无需复习」。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewStartResponse startReview(Long userId) {
        List<WordProgress> due = progressService.listDueProgress(userId);
        if (due.isEmpty()) {
            return new ReviewStartResponse(false, null, null, null, List.of(), List.of());
        }
        User user = userMapper.selectById(userId);
        int reviewGoal = user == null || user.getDailyReviewGoal() == null
                ? 20 : Math.max(1, Math.min(100, user.getDailyReviewGoal()));
        ZoneId zone = progressService.zoneOf(userId);
        due.sort((a, b) -> Integer.compare(urgency(b, zone), urgency(a, zone)));
        due = due.size() <= reviewGoal ? due : new ArrayList<>(due.subList(0, reviewGoal));

        List<Word> words = due.stream()
                .map(p -> wordService.getById(p.getWordId()))
                .toList();
        List<Integer> priorities = due.stream().map(p -> urgency(p, zone)).toList();
        List<WordFrequencyVO> frequency = java.util.stream.IntStream.range(0, words.size())
                .mapToObj(i -> new WordFrequencyVO(words.get(i).getWord(), priorities.get(i)))
                .toList();

        LearningArticle article = articleService.createArticle(
                userId, "REVIEW", words, priorities,
                "请严格按优先级安排单词出现次数，优先级越高出现次数越多，并控制文章难度适中。");
        List<WordVO> wordVOs = words.stream().map(wordService::toVO).toList();
        return new ReviewStartResponse(true, article.getId(), article.getTitle(),
                article.getContentEn(), wordVOs, frequency);
    }

    /**
     * 夜间学习智能弹窗：
     *   - 日边界已启用（day_boundary_hour > 0）时不弹窗（方案一已自动处理）；
     *   - 仅统计「今天凌晨 0 点到 night_cutoff_hour」之间学过的单词；
     *   - 当天已询问过则不重复弹。
     */
    public NightPromptResponse nightPrompt(Long userId) {
        User user = userMapper.selectById(userId);
        ZoneId zone = progressService.zoneOf(userId);
        LocalDate today = ZonedDateTime.now(clock.withZone(zone)).toLocalDate();
        int boundary = user.getDayBoundaryHour() == null ? 0 : Math.max(0, Math.min(23, user.getDayBoundaryHour()));
        int cutoff = user.getNightCutoffHour() == null ? 6 : Math.max(0, Math.min(12, user.getNightCutoffHour()));

        if (boundary > 0) {
            return new NightPromptResponse(false, today, 0);
        }
        if (today.equals(user.getNightPromptDate())) {
            return new NightPromptResponse(false, today, 0);
        }

        LocalDateTime windowStart = LocalDateTime.of(today, LocalTime.MIDNIGHT);
        LocalDateTime windowEnd = LocalDateTime.of(today, LocalTime.of(cutoff, 0));
        List<WordProgress> nightWords = progressService.listNightWords(userId, windowStart, windowEnd);
        boolean show = !nightWords.isEmpty() && ZonedDateTime.now(clock.withZone(zone)).getHour() >= cutoff;
        return new NightPromptResponse(show, today, nightWords.size());
    }

    /**
     * 用户答复智能弹窗：
     *   - apply=true：把凌晨背的单词立即置为到期，加入今日复习；
     *   - apply=false：保持默认计划（次日 0:00）；
     *   无论哪种，当天不再重复询问。
     */
    @Transactional(rollbackFor = Exception.class)
    public void answerNightPrompt(Long userId, boolean apply) {
        User user = userMapper.selectById(userId);
        ZoneId zone = progressService.zoneOf(userId);
        LocalDate today = ZonedDateTime.now(clock.withZone(zone)).toLocalDate();
        int cutoff = user.getNightCutoffHour() == null ? 6 : Math.max(0, Math.min(12, user.getNightCutoffHour()));

        LocalDateTime windowStart = LocalDateTime.of(today, LocalTime.MIDNIGHT);
        LocalDateTime windowEnd = LocalDateTime.of(today, LocalTime.of(cutoff, 0));
        List<WordProgress> nightWords = progressService.listNightWords(userId, windowStart, windowEnd);
        if (apply && !nightWords.isEmpty()) {
            progressService.makeDueNow(userId, nightWords);
        }
        user.setNightPromptDate(today);
        userMapper.updateById(user);
    }

    /** 批改复习文章；通过后按曲线推进所有单词的阶段。 */
    @Transactional(rollbackFor = Exception.class)
    public ArticleCheckResult checkArticle(Long userId, ArticleCheckRequest request) {
        ArticleCheckResult result = articleService.grade(userId, request.articleId(),
                request.userTranslation());
        if (result.passed()) {
            LearningArticle article = articleService.getOwnedArticle(userId, request.articleId());
            for (Long wordId : parseWordIds(article.getWordsJson())) {
                progressService.advanceReview(userId, wordId);
            }
        }
        return result;
    }

    /**
     * 紧急度计算：基础 1；每逾期 1 天 +1（最多 +2）；复习阶段越靠前越紧急 +1。
     */
    private int urgency(WordProgress progress, ZoneId zone) {
        int score = 1;
        if (progress.getNextReviewAt() != null) {
            long overdueDays = Math.max(0, Duration.between(
                    progress.getNextReviewAt(), LocalDateTime.now(clock.withZone(zone))).toDays());
            score += Math.min(2, (int) overdueDays);
        }
        if (progress.getStage() != null && progress.getStage() <= 2) {
            score += 1;
        }
        return Math.min(4, score);
    }

    private List<Long> parseWordIds(String wordsJson) {
        try {
            return objectMapper.readValue(wordsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (Exception ex) {
            return List.of();
        }
    }
}
