package com.wordflow.module.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordflow.common.BusinessException;
import com.wordflow.common.ResultCode;
import com.wordflow.module.ai.AiModels.ArticleDraft;
import com.wordflow.module.ai.AiModels.AiWord;
import com.wordflow.module.ai.AiModels.JudgeRequest;
import com.wordflow.module.ai.AiModels.PracticeSentence;
import com.wordflow.module.ai.AiModels.TranslationJudgement;
import com.wordflow.module.ai.entity.AiLog;
import com.wordflow.module.ai.mapper.AiLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI 兼容大模型实现（Chat Completions）。
 *
 * 模块职责：
 *   - 通过 RestClient 调用 /chat/completions，统一解析 JSON 输出。
 *   - 兼容 OpenAI / DeepSeek / 通义千问等 OpenAI 协议服务。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiService implements AiService {

    private final AiProperties aiProperties;
    private final AiLogMapper aiLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TranslationJudgement judgeTranslation(JudgeRequest request) {
        JsonNode node = chat("JUDGE_TRANSLATION", judgeSystem(), judgeUser(request));
        return toJudgement(node, request);
    }

    @Override
    public void streamJudgeTranslation(JudgeRequest request,
                                       Consumer<String> onChunk,
                                       Consumer<TranslationJudgement> onComplete) {
        streamChat("JUDGE_TRANSLATION", judgeSystem(), judgeUser(request), onChunk, content -> {
            try {
                JsonNode node = extractJson(content);
                onComplete.accept(toJudgement(node, request));
            } catch (Exception e) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR,
                        "AI 批改结果解析失败：" + e.getMessage());
            }
        });
    }

    private String judgeSystem() {
        return """
                你是一名资深英语教师。请批改用户翻译：
                1. 判断是否“基本符合语义”（关键词、时态、语序可放宽，不苛求逐字），如果有错误请直接指出，并给出修改建议和完整的译文示例；
                2. score 为 0-100 的整数，>=60 时 passed=true；
                3. 必须返回合法 JSON：
                   {"passed":boolean,"score":int,"comment":"中文点评","standardAnswer":"标准译文",
                    "errors":[{"segment":"出错片段","expected":"期望表达","user":"用户表达","suggestion":"修改建议"}]}
                   errors 为空数组时表示没有明显错误；
                """;
    }

    private String judgeUser(JudgeRequest request) {
        return "请批改：源文本=" + request.sourceText()
                + "，目标语言=" + request.targetLang()
                + "，用户译文=" + request.userTranslation()
                + "，标准译文=" + request.standardAnswer()
                + "，必须出现的关键词=" + request.requiredWord();
    }

    private TranslationJudgement toJudgement(JsonNode node, JudgeRequest request) {
        boolean passed = node.path("passed").asBoolean(false);
        int score = node.path("score").asInt(0);
        String comment = node.path("comment").asText("");
        String standard = node.path("standardAnswer").asText(request.standardAnswer());
        List<AiModels.ErrorItem> errors = new java.util.ArrayList<>();
        for (JsonNode item : node.path("errors")) {
            errors.add(new AiModels.ErrorItem(
                    item.path("segment").asText(""),
                    item.path("expected").asText(""),
                    item.path("user").asText(""),
                    item.path("suggestion").asText("")));
        }
        return new TranslationJudgement(passed, score, comment, standard, errors);
    }

    @Override
    public PracticeSentence generatePracticeSentence(String currentWord, String currentChinese,
                                                     List<String> previousWords) {
        String system = """
                你是一名英语教学专家。请生成一个包含目标单词的英文短句，并给出中文翻译。
                要求：句子自然地道、长度适中；如果提供了已学单词，尽量在句子中自然复现 1-2 个。
                必须返回合法 JSON：{"sentenceEn":"英文短句","sentenceZh":"中文翻译"}
                """;
        String user = "目标单词=" + currentWord + "（" + currentChinese + "），已学单词=" + previousWords;
        JsonNode node = chat("GENERATE_SENTENCE", system, user);
        return new PracticeSentence(node.path("sentenceEn").asText(""), node.path("sentenceZh").asText(""));
    }

    @Override
    public ArticleDraft generateArticle(String scene, List<AiWord> words, String extraInstruction) {
        String wordList = words.stream()
                .map(w -> w.word() + "(" + w.chinese() + ", 优先级" + w.priority() + ")")
                .toList().toString();
        String system = """
                你是一名英语学习内容专家。请基于单词列表生成一篇用于翻译练习的英文短文：
                1. 场景 DAILY_SUMMARY：短文要包含全部今日单词，逻辑连贯、难度适中；
                2. 场景 REVIEW：严格按照优先级安排出现频率，优先级越高出现次数越多；
                3. 同时给出标准中文翻译；
                4. 必须返回合法 JSON：{"title":"标题","contentEn":"英文正文","contentZh":"中文翻译"}
                """;
        String user = "单词列表=" + wordList + "，额外要求=" + extraInstruction;
        JsonNode node = chat("GENERATE_ARTICLE", system, user);
        return new ArticleDraft(
                node.path("title").asText("短文"),
                node.path("contentEn").asText(""),
                node.path("contentZh").asText(""));
    }

    private JsonNode chat(String scene, String system, String user) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "AI 服务未配置：请设置环境变量 AI_API_KEY，或使用 ai.provider=mock 离线模式");
        }
        try {
            String url = aiProperties.getBaseUrl().endsWith("/")
                    ? aiProperties.getBaseUrl() + "chat/completions"
                    : aiProperties.getBaseUrl() + "/chat/completions";
            int timeoutMillis = (int) TimeUnit.SECONDS.toMillis(
                    Math.max(10, aiProperties.getTimeoutSeconds()));
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMillis);
            factory.setReadTimeout(timeoutMillis);
            RestClient client = RestClient.builder()
                    .requestFactory(factory)
                    .baseUrl(url)
                    .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                    .build();
            Map<String, Object> body = Map.of(
                    "model", aiProperties.getModel(),
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)));
            String responseBody = client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            saveLog(scene, user, responseBody);
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return extractJson(content);
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 服务调用失败：" + ex.getMessage());
        }
    }

    /**
     * 流式调用 Chat Completions（stream=true，SSE 格式）。
     * 使用 JDK 自带 HttpClient，避免引入 WebFlux 依赖。
     */
    private void streamChat(String scene, String system, String user,
                            Consumer<String> onChunk, Consumer<String> onDone) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR,
                    "AI 服务未配置：请设置环境变量 AI_API_KEY，或使用 ai.provider=mock 离线模式");
        }
        try {
            long timeoutSec = Math.max(10, aiProperties.getTimeoutSeconds());
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSec))
                    .build();
            String url = aiProperties.getBaseUrl().endsWith("/")
                    ? aiProperties.getBaseUrl() + "chat/completions"
                    : aiProperties.getBaseUrl() + "/chat/completions";
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getModel());
            body.put("temperature", 0.4);
            body.put("stream", true);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(60, timeoutSec * 3)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("HTTP " + response.statusCode() + ": " + errorBody);
            }
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    JsonNode node = objectMapper.readTree(data);
                    String delta = node.path("choices").path(0).path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        content.append(delta);
                        onChunk.accept(delta);
                    }
                }
            }
            saveLog(scene, user, content.toString());
            onDone.accept(content.toString());
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 流式调用失败：" + ex.getMessage());
        }
    }

    /** 从模型输出中提取第一个 JSON 对象（兼容模型在 JSON 前后加说明文字的情况）。 */
    private JsonNode extractJson(String content) throws Exception {
        String text = content == null ? "" : content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("模型未返回合法 JSON");
        }
        return objectMapper.readTree(text.substring(start, end + 1));
    }

    private void saveLog(String scene, String request, String response) {
        try {
            AiLog log = new AiLog();
            log.setScene(scene);
            log.setRequestJson(request);
            log.setResponseJson(response);
            log.setModel(aiProperties.getModel());
            aiLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志失败不影响主流程
        }
    }
}
