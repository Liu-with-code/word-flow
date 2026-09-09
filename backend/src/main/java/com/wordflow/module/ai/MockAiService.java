package com.wordflow.module.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordflow.module.ai.AiModels.ArticleDraft;
import com.wordflow.module.ai.AiModels.AiWord;
import com.wordflow.module.ai.AiModels.JudgeRequest;
import com.wordflow.module.ai.AiModels.PracticeSentence;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.entity.AiLog;
import com.wordflow.module.ai.mapper.AiLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 离线 Mock AI 实现。
 *
 * 模块职责：
 *   - 在未配置大模型 API Key 时，用规则算法模拟「翻译批改 / 句子生成 / 短文生成」，
 *     让前后端全流程可离线联调。
 * 你需要完成：
 *   - 规则只能覆盖简单场景，正式使用请切换 provider=openai 并接入真实大模型。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiService implements AiService {

    private final AiLogMapper aiLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TranslationJudgement judgeTranslation(JudgeRequest request) {
        boolean passed;
        int score;
        if ("zh".equals(request.targetLang())) {
            // 英译中：比较用户译文与标准译文的汉字重合度
            score = chineseOverlapScore(request.userTranslation(), request.standardAnswer());
            int threshold = "ARTICLE".equals(request.scene()) ? 40 : 55;
            boolean matchesMeaning = request.expectedMeaning() != null
                    && request.userTranslation() != null
                    && request.userTranslation().contains(request.expectedMeaning());
            passed = score >= threshold || matchesMeaning;
        } else {
            // 中译英：必须包含目标单词，且与标准句有词级重合
            String user = request.userTranslation() == null ? "" : request.userTranslation().toLowerCase(Locale.ROOT);
            boolean containsWord = request.requiredWord() != null
                    && user.contains(request.requiredWord().toLowerCase(Locale.ROOT));
            score = wordOverlapScore(user, request.standardAnswer());
            passed = containsWord && score >= 45;
        }
        String comment = passed
                ? "翻译基本符合语义，继续保持！"
                : "与标准译文还有差距，请重点注意「"
                + (request.expectedMeaning() == null ? "关键表达" : request.expectedMeaning())
                + "」的翻译。";
        List<AiModels.ErrorItem> errors = passed ? List.of() : List.of(
                new AiModels.ErrorItem(
                        request.sourceText(),
                        request.standardAnswer(),
                        request.userTranslation(),
                        "请对照标准译文，重点检查关键词与语序"));
        saveLog(request.scene(), request.userTranslation(), null);
        return new TranslationJudgement(passed, score, comment, request.standardAnswer(), errors);
    }

    @Override
    public void streamJudgeTranslation(JudgeRequest request,
                                       Consumer<String> onChunk,
                                       Consumer<TranslationJudgement> onComplete) {
        // Mock 模式没有真正的流式，直接把点评作为一次增量发送，再立即返回结果。
        TranslationJudgement judgement = judgeTranslation(request);
        onChunk.accept(judgement.comment());
        onComplete.accept(judgement);
    }

    @Override
    public PracticeSentence generatePracticeSentence(String currentWord, String currentChinese,
                                                     List<String> previousWords) {
        // TODO(你)：接入真实大模型后，这里会生成更自然的例句，并自然复现已学单词。
        String sentenceEn = "I often see the word \"" + currentWord
                + "\" in daily life, and its Chinese meaning is \"" + currentChinese + "\".";
        String sentenceZh = "我经常在日常生活里看到单词 \"" + currentWord
                + "\"，它的中文意思是「" + currentChinese + "」。";
        if (previousWords != null && !previousWords.isEmpty()) {
            String joined = String.join(", ", previousWords);
            sentenceEn += " Meanwhile, I still remember the words " + joined + ".";
            sentenceZh += " 同时，我还记得单词 " + joined + "。";
        }
        saveLog("GENERATE_SENTENCE", currentWord, sentenceEn);
        return new PracticeSentence(sentenceEn, sentenceZh);
    }

    @Override
    public ArticleDraft generateArticle(String scene, List<AiWord> words, String extraInstruction) {
        // TODO(你)：接入真实大模型后，生成连贯、自然、符合艾宾浩斯频次的短文。
        StringBuilder en = new StringBuilder();
        StringBuilder zh = new StringBuilder();
        for (AiWord w : words) {
            en.append("I try to use the word \"").append(w.word())
                    .append("\" in my daily conversation, which means \"").append(w.chinese())
                    .append("\" in Chinese. ");
            zh.append("我尝试在日常对话中使用单词「").append(w.word())
                    .append("」，它在中文里的意思是「").append(w.chinese()).append("」。 ");
            if (w.priority() > 1) {
                en.append("I repeat \"").append(w.word())
                        .append("\" again and again because it is easy to forget. ");
                zh.append("我一遍又一遍地重复「").append(w.word())
                        .append("」，因为它很容易被遗忘。 ");
            }
        }
        saveLog("GENERATE_ARTICLE", scene, words.stream().map(AiWord::word).toList().toString());
        return new ArticleDraft(scene + " 复习短文", en.toString().trim(), zh.toString().trim());
    }

    private int chineseOverlapScore(String user, String standard) {
        if (user == null || user.isBlank() || standard == null || standard.isBlank()) {
            return 0;
        }
        long hits = standard.chars().distinct()
                .filter(c -> user.indexOf((char) c) >= 0)
                .count();
        return (int) Math.min(100, hits * 100L / standard.chars().distinct().count());
    }

    private int wordOverlapScore(String user, String standard) {
        if (user == null || user.isBlank() || standard == null || standard.isBlank()) {
            return 0;
        }
        String[] standardWords = standard.toLowerCase(Locale.ROOT).split("\\W+");
        long hits = 0;
        for (String sw : standardWords) {
            if (!sw.isBlank() && user.contains(sw)) {
                hits++;
            }
        }
        return (int) Math.min(100, hits * 100L / standardWords.length);
    }

    private void saveLog(String scene, String request, String response) {
        try {
            AiLog log = new AiLog();
            log.setScene(scene);
            log.setRequestJson(objectMapper.writeValueAsString(Map.of("payload", request)));
            log.setResponseJson(response == null ? "" : response);
            log.setModel("mock");
            aiLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志失败不影响主流程
        }
    }
}
