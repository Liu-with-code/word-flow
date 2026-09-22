package com.wordflow.module.review.service;

import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.article.entity.LearningArticle;
import com.wordflow.module.article.service.ArticleService;
import com.wordflow.module.progress.entity.WordProgress;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.review.dto.ReviewDtos.NightPromptResponse;
import com.wordflow.module.review.dto.ReviewDtos.ReviewOverviewResponse;
import com.wordflow.module.review.dto.ReviewDtos.ReviewStartResponse;
import com.wordflow.module.review.dto.ReviewDtos.WarmupMarkResponse;
import com.wordflow.module.review.dto.ReviewDtos.WarmupResponse;
import com.wordflow.module.review.dto.ReviewDtos.WarmupWordVO;
import com.wordflow.module.review.dto.ReviewDtos.WordFrequencyVO;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.mapper.UserMapper;
import com.wordflow.module.word.dto.WordVO;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.service.WordService;
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
 * 复习服务：到期筛选、紧急度排序、复习热身自测与短文批改。
 *
 * 紧急度规则：基础 1 + 每逾期 1 天加 1（最多 +2）+ 早期阶段（1-2 轮）加 1，最高 4。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    /** 复习热身单次最多展示的单词数 */
    private static final int WARMUP_LIMIT = 20;

    /** 紧急度：基础分与各加成上限 */
    private static final int URGENCY_BASE = 1;
    private static final int URGENCY_MAX_OVERDUE_BONUS = 2;
    private static final int URGENCY_EARLY_STAGE_BONUS = 1;
    private static final int URGENCY_MAX = 4;

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
     * 无到期单词时返回 hasWords=false。
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
        List<WordVO> reviewWords = words.stream().map(wordService::toVO).toList();
        return new ReviewStartResponse(true, article.getId(), article.getTitle(),
                article.getContentEn(), reviewWords, frequency);
    }

    /**
     * 复习热身：把到期单词按紧急度排好后交给用户快速自测。
     *
     * 已记住的词直接推进复习阶段，只为真正不熟的词生成复习短文，减少无效翻译量。
     */
    public WarmupResponse warmup(Long userId) {
        ZoneId zone = progressService.zoneOf(userId);
        List<WordProgress> due = new ArrayList<>(progressService.listDueProgress(userId));
        due.sort((a, b) -> Integer.compare(urgency(b, zone), urgency(a, zone)));
        List<WordProgress> candidates = due.size() <= WARMUP_LIMIT
                ? due : new ArrayList<>(due.subList(0, WARMUP_LIMIT));
        List<WarmupWordVO> words = candidates.stream()
                .map(progress -> new WarmupWordVO(
                        wordService.toVO(wordService.getById(progress.getWordId())),
                        progress.getStage() == null ? 0 : progress.getStage(),
                        progress.getNextReviewAt() == null ? null : progress.getNextReviewAt().toString(),
                        urgency(progress, zone),
                        false))
                .toList();
        return new WarmupResponse(due.size(), words.size(), 0, words);
    }

    /**
     * 热身作答：remembered=true 视为通过一轮复习并推进阶段；
     * false 则保持当前阶段，等待短文复习巩固。两种情况都会写入作答流水。
     */
    @Transactional(rollbackFor = Exception.class)
    public WarmupMarkResponse markWarmup(Long userId, Long wordId, boolean remembered) {
        WordProgress progress = progressService.ensureProgress(userId, wordId);
        progressService.recordAnswer(userId, wordId, "REVIEW", "REVIEW_WARMUP",
                remembered, remembered ? "记得" : "不熟", null);
        if (remembered) {
            progressService.advanceReview(userId, wordId);
            progress = progressService.ensureProgress(userId, wordId);
        }
        return new WarmupMarkResponse(
                remembered,
                progress.getStage() == null ? 0 : progress.getStage(),
                progress.getStatus(),
                (int) progressService.countDue(userId));
    }

    /**
     * 夜间学习智能弹窗：
     *   - 日边界已启用（day_boundary_hour > 0）时不弹窗（已由日边界自动处理）；
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
        int score = URGENCY_BASE;
        if (progress.getNextReviewAt() != null) {
            long overdueDays = Math.max(0, Duration.between(
                    progress.getNextReviewAt(), LocalDateTime.now(clock.withZone(zone))).toDays());
            score += Math.min(URGENCY_MAX_OVERDUE_BONUS, (int) overdueDays);
        }
        if (progress.getStage() != null && progress.getStage() <= URGENCY_MAX_OVERDUE_BONUS) {
            score += URGENCY_EARLY_STAGE_BONUS;
        }
        return Math.min(URGENCY_MAX, score);
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
