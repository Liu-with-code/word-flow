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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 每日学习服务（核心业务流程）。
 *
 * 流程（输出倒逼输入）：
 *   PREVIEW(完整示例) -> CHOOSE_ZH(看英选中) -> CHOOSE_EN(看中选英)
 *   -> TRANS_EN(英译中, AI批改) -> TRANS_ZH(中译英, AI批改)
 *   -> COMPLETE_WORD -> 全部完成后生成总结短文 -> ARTICLE(全文翻译批改, >=60 通过)
 *
 */
@Service
@RequiredArgsConstructor
public class LearningService {

    private static final String SESSION_DAILY = "DAILY_LEARN";

    private final LearningPlanMapper planMapper;
    private final LearningPlanWordMapper planWordMapper;
    private final WordService wordService;
    private final BookService bookService;
    private final UserService userService;
    private final ProgressService progressService;
    private final ArticleService articleService;
    private final AiService aiService;
    private final Clock clock;

    /** 练习句缓存：key = userId:wordId，避免翻译步骤与生成步骤不一致 */
    private final Map<String, PracticeSentence> practiceCache = new ConcurrentHashMap<>();

    public TodayPlanResponse getToday(Long userId) {
        LearningPlan plan = findTodayPlan(userId);
        if (plan == null) {
            User user = userService.getById(userId);
            Book activeBook = bookService.getById(user.getActiveBookId() == null ? 1L : user.getActiveBookId());
            return new TodayPlanResponse(LocalDate.now(), 0, 0, 0, List.of(),
                    activeBook.getId(), activeBook.getName());
        }
        List<LearningPlanWord> planWords = planWordMapper.selectList(
                Wrappers.<LearningPlanWord>lambdaQuery()
                        .eq(LearningPlanWord::getPlanId, plan.getId())
                        .orderByAsc(LearningPlanWord::getOrderNo));
        List<WordVO> words = planWords.stream()
                .map(pw -> wordService.toVO(wordService.getById(pw.getWordId())))
                .toList();
        int currentIndex = 0;
        for (int i = 0; i < planWords.size(); i++) {
            if ("PENDING".equals(planWords.get(i).getStatus())) {
                currentIndex = i;
                break;
            }
            currentIndex = planWords.size();
        }
        Book book = bookService.getById(plan.getBookId());
        return new TodayPlanResponse(plan.getPlanDate(), planWords.size(),
                plan.getCompletedCount(), currentIndex, words,
                book.getId(), book.getName());
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
        int goal = user.getDailyWordGoal() == null ? 20
                : Math.max(1, Math.min(100, user.getDailyWordGoal()));

        LearningPlan plan = findTodayPlan(userId);
        if (plan == null) {
            createPlan(userId, bookId, goal);
            return;
        }
        if ("COMPLETED".equals(plan.getStatus())) {
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
        plan.setStatus("IN_PROGRESS");
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
        plan.setStatus("IN_PROGRESS");
        planMapper.updateById(plan);
        insertPlanWords(plan, picked);
    }

    /** 仅调整目标数量：目标变大则补充新词，变小则裁剪未完成的尾部单词。 */
    private void adjustPlan(LearningPlan plan, Long userId, Long bookId, int goal) {
        List<LearningPlanWord> current = listPlanWords(plan.getId());
        int completed = (int) current.stream()
                .filter(pw -> "COMPLETED".equals(pw.getStatus()))
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
                pw.setStatus("PENDING");
                planWordMapper.insert(pw);
                progressService.ensureProgress(userId, word.getId());
            }
            plan.setNewWordCount(current.size() + extra.size());
        } else if (goal < current.size()) {
            List<Long> removeIds = current.stream()
                    .filter(pw -> pw.getOrderNo() >= goal && "PENDING".equals(pw.getStatus()))
                    .map(LearningPlanWord::getWordId)
                    .toList();
            planWordMapper.delete(Wrappers.<LearningPlanWord>lambdaQuery()
                    .eq(LearningPlanWord::getPlanId, plan.getId())
                    .ge(LearningPlanWord::getOrderNo, goal)
                    .eq(LearningPlanWord::getStatus, "PENDING"));
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
            planWord.setStatus("PENDING");
            planWordMapper.insert(planWord);
            progressService.ensureProgress(plan.getUserId(), word.getId());
        }
    }

    /** 第一步：看英文选中文。选错则回到 PREVIEW 重新学习。 */
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

    /** 第二步：看中文选英文。选错则回到 PREVIEW，重新完成前两步。 */
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
     * 获取 AI 练习句（后续单词会自然复现已学单词）。
     * 同一单词的练习句只生成一次，之后直接命中缓存，供前端预取加速。
     */
    public PracticeResponse getPracticeSentence(Long userId, Long wordId) {
        Word word = wordService.getById(wordId);
        PracticeSentence cached = practiceCache.get(cacheKey(userId, wordId));
        if (cached != null) {
            return new PracticeResponse(cached.sentenceEn(), cached.sentenceZh());
        }
        List<String> previousWords = listTodayCompletedWords(userId);
        PracticeSentence sentence = aiService.generatePracticeSentence(
                word.getWord(), word.getChinese(), previousWords);
        practiceCache.put(cacheKey(userId, wordId), sentence);
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

    /** 第三步：英译中（AI 批改）。 */
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

    /** 第四步：中译英（AI 批改）。 */
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
        if (!"COMPLETED".equals(planWord.getStatus())) {
            planWord.setStatus("COMPLETED");
            planWord.setCompletedAt(LocalDateTime.now(clock));
            planWordMapper.updateById(planWord);

            plan.setCompletedCount(plan.getCompletedCount() + 1);
            if (plan.getCompletedCount() >= plan.getNewWordCount()) {
                plan.setStatus("COMPLETED");
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
        if (plan == null || !"COMPLETED".equals(plan.getStatus())) {
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
                plan.setStatus("COMPLETED");
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

    private List<String> listTodayCompletedWords(Long userId) {
        LearningPlan plan = findTodayPlan(userId);
        if (plan == null) {
            return List.of();
        }
        List<LearningPlanWord> completed = planWordMapper.selectList(
                Wrappers.<LearningPlanWord>lambdaQuery()
                        .eq(LearningPlanWord::getPlanId, plan.getId())
                        .eq(LearningPlanWord::getStatus, "COMPLETED")
                        .orderByAsc(LearningPlanWord::getOrderNo));
        return completed.stream().map(pw -> wordService.getById(pw.getWordId()).getWord()).toList();
    }

    private PracticeSentence cachedPractice(Long userId, Word word) {
        PracticeSentence sentence = practiceCache.get(cacheKey(userId, word.getId()));
        if (sentence == null) {
            sentence = new PracticeSentence(word.getExampleEn(), word.getExampleZh());
            practiceCache.put(cacheKey(userId, word.getId()), sentence);
        }
        return sentence;
    }

    private String cacheKey(Long userId, Long wordId) {
        return userId + ":" + wordId;
    }
}
