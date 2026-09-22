package com.wordflow.module.learning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordflow.common.BusinessException;
import com.wordflow.common.ResultCode;
import com.wordflow.module.ai.AiModels;
import com.wordflow.module.ai.AiModels.JudgeRequest;
import com.wordflow.module.ai.AiModels.PracticeSentence;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.AiService;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckRequest;
import com.wordflow.module.article.dto.ArticleDtos.ArticleCheckResult;
import com.wordflow.module.article.entity.LearningArticle;
import com.wordflow.module.article.service.ArticleService;
import com.wordflow.module.book.entity.Book;
import com.wordflow.module.book.service.BookService;
import com.wordflow.module.learning.dto.LearningDtos.AddWordsRequest;
import com.wordflow.module.learning.dto.LearningDtos.CheckEnRequest;
import com.wordflow.module.learning.dto.LearningDtos.CheckZhRequest;
import com.wordflow.module.learning.dto.LearningDtos.CompleteWordRequest;
import com.wordflow.module.learning.dto.LearningDtos.FinishDayResponse;
import com.wordflow.module.learning.dto.LearningDtos.HintResponse;
import com.wordflow.module.learning.dto.LearningDtos.PracticeResponse;
import com.wordflow.module.learning.dto.LearningDtos.StepResult;
import com.wordflow.module.learning.dto.LearningDtos.TodayPlanResponse;
import com.wordflow.module.learning.dto.LearningDtos.TranslateHintRequest;
import com.wordflow.module.learning.dto.LearningDtos.TranslateRequest;
import com.wordflow.module.learning.dto.LearningDtos.TranslateStreamRequest;
import com.wordflow.module.learning.entity.LearningPlan;
import com.wordflow.module.learning.entity.LearningPlanWord;
import com.wordflow.module.learning.mapper.LearningPlanMapper;
import com.wordflow.module.learning.mapper.LearningPlanWordMapper;
import com.wordflow.module.progress.service.ProgressService;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.service.UserService;
import com.wordflow.module.word.dto.WordVO;
import com.wordflow.module.word.entity.Word;
import com.wordflow.module.word.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 每日学习服务（核心业务流程）。
 *
 * 流程（输出倒逼输入）：
 *   PREVIEW(完整示例) -> CHOOSE_ZH(看英选中) -> CHOOSE_EN(看中选英)
 *   -> TRANS_EN(英译中, AI批改) -> TRANS_ZH(中译英, AI批改)
 *   -> COMPLETE_WORD -> 全部完成后生成总结短文 -> ARTICLE(全文翻译批改, >=60 通过)
 */
@Service
@RequiredArgsConstructor
public class LearningService {

    private static final String SESSION_DAILY = "DAILY_LEARN";

    /** 练习句缓存容量上限，超出后按 LRU 淘汰 */
    private static final int PRACTICE_CACHE_CAPACITY = 512;

    /** 练习句中自然复现的薄弱单词数量上限 */
    private static final int PRACTICE_REVIEW_WORDS = 5;

    /** 每日目标取值范围与默认值 */
    private static final int MIN_DAILY_GOAL = 1;
    private static final int MAX_DAILY_GOAL = 100;
    private static final int DEFAULT_DAILY_GOAL = 20;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String PLAN_STATUS_COMPLETED = "COMPLETED";
    private static final String PLAN_STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final LearningPlanMapper planMapper;
    private final LearningPlanWordMapper planWordMapper;
    private final WordService wordService;
    private final BookService bookService;
    private final UserService userService;
    private final ProgressService progressService;
    private final ArticleService articleService;
    private final AiService aiService;
    private final Clock clock;

    /**
     * 练习句缓存：key = userId:wordId，用于保证翻译步骤与生成步骤使用同一句子。
     *
     * 采用同步访问的 LRU 有界缓存（{@link LinkedHashMap}），
     * 超过 {@value #PRACTICE_CACHE_CAPACITY} 条后淘汰最久未使用的记录。
     */
    private final Map<String, PracticeSentence> practiceCache =
            new LinkedHashMap<>(16, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PracticeSentence> eldest) {
                    return size() > PRACTICE_CACHE_CAPACITY;
                }
            };

    public TodayPlanResponse getToday(Long userId) {
        User user = userService.getById(userId);
        int dailyGoal = normalizeDailyGoal(user.getDailyWordGoal());
        LearningPlan plan = findTodayPlan(userId);
        if (plan == null) {
            Book activeBook = bookService.getById(user.getActiveBookId() == null ? 1L : user.getActiveBookId());
            return new TodayPlanResponse(LocalDate.now(), 0, 0, 0, List.of(),
                    activeBook.getId(), activeBook.getName(), dailyGoal);
        }
        List<LearningPlanWord> planWords = listPlanWords(plan.getId());
        List<WordVO> words = planWords.stream()
                .map(pw -> wordService.toVO(wordService.getById(pw.getWordId())))
                .toList();
        int currentIndex = 0;
        for (int i = 0; i < planWords.size(); i++) {
            if (STATUS_PENDING.equals(planWords.get(i).getStatus())) {
                currentIndex = i;
                break;
            }
            currentIndex = planWords.size();
        }
        Book book = bookService.getById(plan.getBookId());
        return new TodayPlanResponse(plan.getPlanDate(), planWords.size(),
                plan.getCompletedCount(), currentIndex, words,
                book.getId(), book.getName(), dailyGoal);
    }

    /**
     * 开始今日学习：无计划时创建，有计划时先做“计划协调”
     * （检测是否更换词书/调整每日目标），再返回最新计划。幂等。
     */
    @Transactional(rollbackFor = Exception.class)
    public TodayPlanResponse startToday(Long userId) {
        reconcileTodayPlan(userId);
        return getToday(userId);
    }

    /** 主动触发今日计划协调（前端在保存设置 / 更换词书后调用）。 */
    @Transactional(rollbackFor = Exception.class)
    public TodayPlanResponse reconcileToday(Long userId) {
        reconcileTodayPlan(userId);
        return getToday(userId);
    }

    /**
     * 今日计划协调：
     *   - 无计划：按当前词书 + 每日目标创建；
     *   - 已完成计划：保持不变；
     *   - 进行中计划：词书变了则重建，目标变了则增删单词。
     */
    private void reconcileTodayPlan(Long userId) {
        User user = userService.getById(userId);
        Long bookId = user.getActiveBookId() == null ? 1L : user.getActiveBookId();
        bookService.getById(bookId);
        int goal = normalizeDailyGoal(user.getDailyWordGoal());

        LearningPlan plan = findTodayPlan(userId);
        if (plan == null) {
            createPlan(userId, bookId, goal);
            return;
        }
        if (PLAN_STATUS_COMPLETED.equals(plan.getStatus())) {
            return;
        }
        boolean bookChanged = !Objects.equals(bookId, plan.getBookId());
        boolean goalChanged = goal != plan.getNewWordCount();
        if (!bookChanged && !goalChanged) {
            return;
        }
        if (bookChanged) {
            rebuildPlan(plan, userId, bookId, goal);
        } else {
            adjustPlan(plan, userId, bookId, goal);
        }
    }

    private void createPlan(Long userId, Long bookId, int goal) {
        List<Word> picked = wordService.pickNewWords(userId, goal, bookId);
        if (picked.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "词库中没有新词了，请扩充词库");
        }
        LearningPlan plan = new LearningPlan();
        plan.setUserId(userId);
        plan.setBookId(bookId);
        plan.setPlanDate(todayOf(userId));
        plan.setNewWordCount(picked.size());
        plan.setCompletedCount(0);
        plan.setStatus(PLAN_STATUS_IN_PROGRESS);
        planMapper.insert(plan);
        insertPlanWords(plan, picked);
    }

    /** 更换词书：重建今日计划，并回收旧词书中“从未作答”的占位进度。 */
    private void rebuildPlan(LearningPlan plan, Long userId, Long bookId, int goal) {
        List<LearningPlanWord> oldWords = listPlanWords(plan.getId());
        planWordMapper.delete(Wrappers.<LearningPlanWord>lambdaQuery()
                .eq(LearningPlanWord::getPlanId, plan.getId()));
        progressService.removePlaceholderProgress(userId,
                oldWords.stream().map(LearningPlanWord::getWordId).toList());

        List<Word> picked = wordService.pickNewWords(userId, goal, bookId);
        plan.setBookId(bookId);
        plan.setNewWordCount(picked.size());
        plan.setCompletedCount(0);
        plan.setStatus(PLAN_STATUS_IN_PROGRESS);
        planMapper.updateById(plan);
        insertPlanWords(plan, picked);
    }

    /** 仅调整目标数量：目标变大则补充新词，变小则裁剪未完成的尾部单词。 */
    private void adjustPlan(LearningPlan plan, Long userId, Long bookId, int goal) {
        List<LearningPlanWord> current = listPlanWords(plan.getId());
        int completed = (int) current.stream()
                .filter(pw -> STATUS_COMPLETED.equals(pw.getStatus()))
                .count();
        if (goal > current.size()) {
            int addCount = goal - current.size();
            List<Word> extra = wordService.pickNewWords(userId, addCount, bookId);
            int order = current.size();
            for (Word word : extra) {
                LearningPlanWord pw = new LearningPlanWord();
                pw.setPlanId(plan.getId());
                pw.setUserId(userId);
                pw.setWordId(word.getId());
                pw.setOrderNo(order++);
                pw.setStatus(STATUS_PENDING);
                planWordMapper.insert(pw);
                progressService.ensureProgress(userId, word.getId());
            }
            plan.setNewWordCount(current.size() + extra.size());
        } else if (goal < current.size()) {
            List<Long> removeIds = current.stream()
                    .filter(pw -> pw.getOrderNo() >= goal && STATUS_PENDING.equals(pw.getStatus()))
                    .map(LearningPlanWord::getWordId)
                    .toList();
            planWordMapper.delete(Wrappers.<LearningPlanWord>lambdaQuery()
                    .eq(LearningPlanWord::getPlanId, plan.getId())
                    .ge(LearningPlanWord::getOrderNo, goal)
                    .eq(LearningPlanWord::getStatus, STATUS_PENDING));
            progressService.removePlaceholderProgress(userId, removeIds);
            plan.setNewWordCount(Math.max(goal, completed));
        }
        plan.setBookId(bookId);
        planMapper.updateById(plan);
    }

    private List<LearningPlanWord> listPlanWords(Long planId) {
        return planWordMapper.selectList(Wrappers.<LearningPlanWord>lambdaQuery()
                .eq(LearningPlanWord::getPlanId, planId)
                .orderByAsc(LearningPlanWord::getOrderNo));
    }

    private void insertPlanWords(LearningPlan plan, List<Word> words) {
        int order = 0;
        for (Word word : words) {
            LearningPlanWord planWord = new LearningPlanWord();
            planWord.setPlanId(plan.getId());
            planWord.setUserId(plan.getUserId());
            planWord.setWordId(word.getId());
            planWord.setOrderNo(order++);
            planWord.setStatus(STATUS_PENDING);
            planWordMapper.insert(planWord);
            progressService.ensureProgress(plan.getUserId(), word.getId());
        }
    }

    /**
     * 追加单词到今日计划（错题本「加入今日学习」）。
     *
     * 规则：无今日计划则报错；已存在的单词不重复添加；追加后总数不超过每日目标，
     * 超出部分截断；已完成（COMPLETED）的计划被追加后重新置为进行中。
     */
    @Transactional(rollbackFor = Exception.class)
    public TodayPlanResponse addWordsToToday(Long userId, AddWordsRequest request) {
        LearningPlan plan = findTodayPlan(userId);
        if (plan == null) {
            throw new BusinessException(ResultCode.CONFLICT, "今日还没有学习计划，请先开始今日学习");
        }
        User user = userService.getById(userId);
        int goal = normalizeDailyGoal(user.getDailyWordGoal());

        List<LearningPlanWord> current = listPlanWords(plan.getId());
        int completedCount = (int) current.stream()
                .filter(pw -> STATUS_COMPLETED.equals(pw.getStatus()))
                .count();
        int capacity = goal - completedCount;
        if (capacity <= 0) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "今日目标已完成（" + completedCount + "/" + goal + "），可先调高每日目标再追加");
        }

        Set<Long> existingWordIds = current.stream()
                .map(LearningPlanWord::getWordId)
                .collect(Collectors.toSet());
        List<Long> candidates = request.wordIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(wordId -> !existingWordIds.contains(wordId))
                .toList();
        if (candidates.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "所选单词已在今日计划中");
        }
        if (candidates.size() > capacity) {
            candidates = candidates.subList(0, capacity);
        }

        int order = current.size();
        for (Long wordId : candidates) {
            // 校验存在性，避免脏数据写入计划
            wordService.getById(wordId);
            LearningPlanWord planWord = new LearningPlanWord();
            planWord.setPlanId(plan.getId());
            planWord.setUserId(userId);
            planWord.setWordId(wordId);
            planWord.setOrderNo(order++);
            planWord.setStatus(STATUS_PENDING);
            planWordMapper.insert(planWord);
            progressService.ensureProgress(userId, wordId);
        }
        plan.setNewWordCount(current.size() + candidates.size());
        if (PLAN_STATUS_COMPLETED.equals(plan.getStatus())) {
            plan.setStatus(PLAN_STATUS_IN_PROGRESS);
        }
        planMapper.updateById(plan);
        return getToday(userId);
    }

    public StepResult checkZh(Long userId, CheckZhRequest request) {
        Word word = wordService.getById(request.wordId());
        boolean correct = word.getChinese().equals(request.selectedChinese());
        progressService.recordAnswer(userId, word.getId(), SESSION_DAILY, "CHOOSE_ZH",
                correct, request.selectedChinese(), null);
        if (!correct) {
            return new StepResult(false, "释义选错了，请重新学习该单词的完整示例。",
                    "PREVIEW", null, null, word.getExampleEn(), false, List.of());
        }
        return new StepResult(true, "释义正确！", "CHOOSE_EN", null, null, null, false, List.of());
    }

    public StepResult checkEn(Long userId, CheckEnRequest request) {
        Word word = wordService.getById(request.wordId());
        boolean correct = word.getWord().equals(request.selectedWord());
        progressService.recordAnswer(userId, word.getId(), SESSION_DAILY, "CHOOSE_EN",
                correct, request.selectedWord(), null);
        if (!correct) {
            return new StepResult(false, "单词选错了，请重新学习并再次完成前两步。",
                    "PREVIEW", null, null, word.getChinese(), false, List.of());
        }
        return new StepResult(true, "单词识别正确！", "TRANS_EN", null, null, null, false, List.of());
    }

    /**
     * 获取 AI 练习句：命中缓存直接返回，未命中则生成并缓存，供前端预取加速。
     */
    public PracticeResponse getPracticeSentence(Long userId, Long wordId) {
        Word word = wordService.getById(wordId);
        String key = cacheKey(userId, wordId);
        synchronized (practiceCache) {
            PracticeSentence cached = practiceCache.get(key);
            if (cached != null) {
                return new PracticeResponse(cached.sentenceEn(), cached.sentenceZh());
            }
        }
        List<String> previousWords = listTodayCompletedWords(userId);
        PracticeSentence sentence = aiService.generatePracticeSentence(
                word.getWord(), word.getChinese(), previousWords);
        synchronized (practiceCache) {
            practiceCache.put(key, sentence);
        }
        return new PracticeResponse(sentence.sentenceEn(), sentence.sentenceZh());
    }

    /** 流式批改翻译：onChunk 实时推送增量文本，onComplete 返回最终结果。 */
    public void streamTranslate(Long userId, TranslateStreamRequest request,
                                Consumer<String> onChunk, Consumer<StepResult> onComplete) {
        Word word = wordService.getById(request.wordId());
        PracticeSentence sentence = cachedPractice(userId, word);
        boolean en2zh = "en2zh".equals(request.direction());
        String scene = en2zh ? "TRANS_EN" : "TRANS_ZH";
        String standard = en2zh ? sentence.sentenceZh() : sentence.sentenceEn();
        JudgeRequest judgeRequest = new JudgeRequest(
                scene,
                request.sentence(),
                en2zh ? "en" : "zh",
                en2zh ? "zh" : "en",
                request.userTranslation(),
                word.getWord(),
                word.getChinese(),
                standard);
        aiService.streamJudgeTranslation(judgeRequest, onChunk, judgement -> {
            progressService.recordAnswer(userId, word.getId(), SESSION_DAILY, scene,
                    judgement.passed(), request.userTranslation(), judgement.comment());
            String nextStep;
            if (!judgement.passed()) {
                nextStep = scene;
            } else {
                nextStep = en2zh ? "TRANS_ZH" : "COMPLETE_WORD";
            }
            onComplete.accept(new StepResult(judgement.passed(), judgement.comment(), nextStep,
                    judgement.score(), judgement.comment(), judgement.standardAnswer(), false,
                    judgement.errors()));
        });
    }

    /**
     * “我不会”：返回大模型已生成的标准译文。
     * 一旦请求参考译文，该单词按“不通过”记录一次作答（计入错误统计），
     * 前端随后会重新开始学习该单词。
     */
    public HintResponse translateHint(Long userId, TranslateHintRequest request) {
        Word word = wordService.getById(request.wordId());
        PracticeSentence sentence = cachedPractice(userId, word);
        boolean en2zh = "en2zh".equals(request.direction());
        String hint = en2zh ? sentence.sentenceZh() : sentence.sentenceEn();
        String source = en2zh ? sentence.sentenceEn() : sentence.sentenceZh();
        progressService.recordAnswer(userId, word.getId(), SESSION_DAILY, "TRANS_HINT",
                false, request.direction() + ":" + hint, "用户请求参考译文，本单词判定为未通过");
        return new HintResponse(hint, source);
    }

    public StepResult translateEn(Long userId, TranslateRequest request) {
        Word word = wordService.getById(request.wordId());
        PracticeSentence sentence = cachedPractice(userId, word);
        TranslationJudgement judgement = aiService.judgeTranslation(new JudgeRequest(
                "TRANS_EN",
                request.sentence(),
                "en",
                "zh",
                request.userTranslation(),
                word.getWord(),
                word.getChinese(),
                sentence.sentenceZh()));
        progressService.recordAnswer(userId, word.getId(), SESSION_DAILY, "TRANS_EN",
                judgement.passed(), request.userTranslation(), judgement.comment());
        if (!judgement.passed()) {
            return new StepResult(false, judgement.comment(), "TRANS_EN",
                    judgement.score(), judgement.comment(), judgement.standardAnswer(), false,
                    judgement.errors());
        }
        return new StepResult(true, "英译中通过！", "TRANS_ZH",
                judgement.score(), judgement.comment(), judgement.standardAnswer(), false,
                judgement.errors());
    }

    public StepResult translateZh(Long userId, TranslateRequest request) {
        Word word = wordService.getById(request.wordId());
        PracticeSentence sentence = cachedPractice(userId, word);
        TranslationJudgement judgement = aiService.judgeTranslation(new JudgeRequest(
                "TRANS_ZH",
                request.sentence(),
                "zh",
                "en",
                request.userTranslation(),
                word.getWord(),
                word.getChinese(),
                sentence.sentenceEn()));
        progressService.recordAnswer(userId, word.getId(), SESSION_DAILY, "TRANS_ZH",
                judgement.passed(), request.userTranslation(), judgement.comment());
        if (!judgement.passed()) {
            return new StepResult(false, judgement.comment(), "TRANS_ZH",
                    judgement.score(), judgement.comment(), judgement.standardAnswer(), false,
                    judgement.errors());
        }
        return new StepResult(true, "中译英通过！", "COMPLETE_WORD",
                judgement.score(), judgement.comment(), judgement.standardAnswer(), false,
                judgement.errors());
    }

    /** 标记单词完成并进入艾宾浩斯第 1 轮。 */
    @Transactional(rollbackFor = Exception.class)
    public StepResult completeWord(Long userId, CompleteWordRequest request) {
        LearningPlan plan = findTodayPlan(userId);
        if (plan == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "今日没有学习计划");
        }
        LearningPlanWord planWord = planWordMapper.selectOne(
                Wrappers.<LearningPlanWord>lambdaQuery()
                        .eq(LearningPlanWord::getPlanId, plan.getId())
                        .eq(LearningPlanWord::getWordId, request.wordId()));
        if (planWord == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该单词不在今日计划中");
        }
        if (!STATUS_COMPLETED.equals(planWord.getStatus())) {
            planWord.setStatus(STATUS_COMPLETED);
            planWord.setCompletedAt(LocalDateTime.now(clock));
            planWordMapper.updateById(planWord);

            plan.setCompletedCount(plan.getCompletedCount() + 1);
            if (plan.getCompletedCount() >= plan.getNewWordCount()) {
                plan.setStatus(PLAN_STATUS_COMPLETED);
            }
            planMapper.updateById(plan);
            progressService.markLearned(userId, request.wordId());
        }
        boolean finished = plan.getCompletedCount() >= plan.getNewWordCount();
        return new StepResult(true, "单词学习完成！", finished ? "FINISH_DAY" : "NEXT_WORD",
                null, null, null, finished, List.of());
    }

    /** 今日单词全部学完后：AI 生成包含全部今日单词的总结短文。 */
    @Transactional(rollbackFor = Exception.class)
    public FinishDayResponse finishDay(Long userId) {
        LearningPlan plan = findTodayPlan(userId);
        if (plan == null || !PLAN_STATUS_COMPLETED.equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "今日单词尚未全部完成");
        }
        List<LearningPlanWord> planWords = planWordMapper.selectList(
                Wrappers.<LearningPlanWord>lambdaQuery()
                        .eq(LearningPlanWord::getPlanId, plan.getId())
                        .orderByAsc(LearningPlanWord::getOrderNo));
        List<Word> words = planWords.stream()
                .map(pw -> wordService.getById(pw.getWordId()))
                .toList();
        LearningArticle article = articleService.createArticle(
                userId, "DAILY_SUMMARY", words, null,
                "请把今日所学单词自然融入一篇连贯短文，不要逐个罗列。");
        return new FinishDayResponse(article.getId(), article.getTitle(),
                article.getContentEn(), words.size());
    }

    /** 批改今日总结短文；通过后将计划置为完成态。 */
    @Transactional(rollbackFor = Exception.class)
    public ArticleCheckResult checkArticle(Long userId, ArticleCheckRequest request) {
        ArticleCheckResult result = articleService.grade(userId, request.articleId(),
                request.userTranslation());
        if (result.passed()) {
            LearningPlan plan = findTodayPlan(userId);
            if (plan != null) {
                plan.setStatus(PLAN_STATUS_COMPLETED);
                planMapper.updateById(plan);
            }
        }
        return result;
    }

    private LearningPlan findTodayPlan(Long userId) {
        return planMapper.selectOne(
                Wrappers.<LearningPlan>lambdaQuery()
                        .eq(LearningPlan::getUserId, userId)
                        .eq(LearningPlan::getPlanDate, todayOf(userId)));
    }

    /** 用户时区 + 日边界下的“今天”。 */
    private LocalDate todayOf(Long userId) {
        User user = userService.getById(userId);
        ZoneId zone;
        try {
            zone = ZoneId.of(user.getTimezone() == null || user.getTimezone().isBlank()
                    ? "Asia/Shanghai" : user.getTimezone());
        } catch (Exception ex) {
            zone = ZoneId.systemDefault();
        }
        int boundary = user.getDayBoundaryHour() == null ? 0
                : Math.max(0, Math.min(23, user.getDayBoundaryHour()));
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));
        return now.getHour() < boundary ? now.toLocalDate().minusDays(1) : now.toLocalDate();
    }

    /**
     * 练习句的「自然复现单词」列表：今日已完成的单词 + 薄弱单词。
     *
     * 答错过的单词会持续出现在后续练习句里，让用户在语境中反复输出直到掌握。
     */
    private List<String> listTodayCompletedWords(Long userId) {
        List<String> result = new ArrayList<>();
        LearningPlan plan = findTodayPlan(userId);
        if (plan != null) {
            List<LearningPlanWord> completed = planWordMapper.selectList(
                    Wrappers.<LearningPlanWord>lambdaQuery()
                            .eq(LearningPlanWord::getPlanId, plan.getId())
                            .eq(LearningPlanWord::getStatus, STATUS_COMPLETED)
                            .orderByAsc(LearningPlanWord::getOrderNo));
            completed.stream()
                    .map(pw -> wordService.getById(pw.getWordId()).getWord())
                    .forEach(result::add);
        }
        int weakQuota = Math.max(0, PRACTICE_REVIEW_WORDS - result.size());
        if (weakQuota > 0) {
            progressService.listWeakWordIds(userId, weakQuota).stream()
                    .map(wordId -> wordService.getById(wordId).getWord())
                    .filter(word -> !result.contains(word))
                    .forEach(result::add);
        }
        return result;
    }

    /** 每日目标归一化：空值取默认值，并收敛到合法区间。 */
    private int normalizeDailyGoal(Integer dailyWordGoal) {
        if (dailyWordGoal == null) {
            return DEFAULT_DAILY_GOAL;
        }
        return Math.max(MIN_DAILY_GOAL, Math.min(MAX_DAILY_GOAL, dailyWordGoal));
    }

    /** 读取练习句：命中缓存直接返回，未命中则用词典例句兜底并写入缓存。 */
    private PracticeSentence cachedPractice(Long userId, Word word) {
        String key = cacheKey(userId, word.getId());
        synchronized (practiceCache) {
            PracticeSentence cached = practiceCache.get(key);
            if (cached != null) {
                return cached;
            }
            PracticeSentence sentence = new PracticeSentence(word.getExampleEn(), word.getExampleZh());
            practiceCache.put(key, sentence);
            return sentence;
        }
    }

    private String cacheKey(Long userId, Long wordId) {
        return userId + ":" + wordId;
    }
}
