package com.wordflow.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 词书 JSONL 数据导入器（一次性工具）。
 *
 * 模块职责：
 *   - 把 database/raw 下的 KyleBing/english-vocabulary JSONL 词书
 *     解析并批量写入 learn_word，回写 learn_book.word_count。
 *
 * 运行方式（在 backend 目录下）：
 *   mvn spring-boot:run -Dspring-boot.run.profiles=import
 *       -Dspring-boot.run.arguments="--wordflow.import-dir=../database/raw"
 *
 * 幂等性：按 (book_id, word) 唯一键 upsert，可重复执行；
 *         已存在单词保留原 ID，用户学习进度不会丢失。
 *
 */
@Slf4j
@Component
@Profile("import")
@RequiredArgsConstructor
public class BookDataImporter implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wordflow.import-dir:../database/raw}")
    private String importDir;

    /**
     * 词书定义：文件名 -> (id, code, level, 难度)。
     * id 与 database/migrate_book.sql 中预置的词书保持一致。
     */
    private static final Map<String, BookMeta> BOOK_META = Map.ofEntries(
            Map.entry("四级.jsonl", new BookMeta(1L, "CET4", "四级", 2)),
            Map.entry("六级.jsonl", new BookMeta(2L, "CET6", "六级", 3)),
            Map.entry("考研.jsonl", new BookMeta(3L, "KY", "考研", 4)),
            Map.entry("托福.jsonl", new BookMeta(4L, "TOEFL", "托福", 4)),
            Map.entry("雅思.jsonl", new BookMeta(5L, "IELTS", "雅思", 4)),
            Map.entry("GRE.jsonl", new BookMeta(6L, "GRE", "GRE", 5)),
            Map.entry("高中.jsonl", new BookMeta(7L, "SENIOR", "高中", 3)),
            Map.entry("初中.jsonl", new BookMeta(8L, "JUNIOR", "初中", 1)));

    @Override
    public void run(ApplicationArguments args) {
        Path dir = Path.of(importDir);
        if (!Files.isDirectory(dir)) {
            log.error("导入目录不存在：{}", dir.toAbsolutePath());
            System.exit(1);
        }
        log.info("开始导入词书，目录：{}", dir.toAbsolutePath());
        try (Stream<Path> paths = Files.list(dir)) {
            List<Path> files = paths
                    .filter(p -> p.toString().endsWith(".jsonl"))
                    .sorted()
                    .toList();
            for (Path file : files) {
                importBook(file);
            }
        } catch (IOException e) {
            log.error("读取导入目录失败", e);
            System.exit(1);
        }
        log.info("全部词书导入完成");
        System.exit(0);
    }

    private void importBook(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        BookMeta meta = BOOK_META.get(fileName);
        if (meta == null) {
            log.warn("跳过未登记的词书文件：{}（如需导入请在 BOOK_META 中登记）", fileName);
            return;
        }
        upsertBook(meta, fileName);

        // 同一文件内同一单词可能出现多次（不同释义/词性），先按单词合并，
        // 再统一 upsert，避免后出现的释义覆盖前面的释义。
        Map<String, ParsedWord> merged = new LinkedHashMap<>();
        int lineNo = 0;
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                ParsedWord parsed = parseWord(line);
                if (parsed != null) {
                    merged.merge(parsed.word(), parsed, ParsedWord::merge);
                }
            }
        }
        List<Object[]> batch = new ArrayList<>(merged.size());
        for (ParsedWord word : merged.values()) {
            batch.add(word.toParams(meta));
            if (batch.size() >= 500) {
                flush(batch);
            }
        }
        flush(batch);
        refreshWordCount(meta.id());
        log.info("词书「{}」导入完成：{} 行，共写入 {} 词", meta.code(), lineNo,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM learn_word WHERE book_id = ?", Long.class, meta.id()));
    }

    private void upsertBook(BookMeta meta, String fileName) {
        jdbcTemplate.update("""
                        INSERT INTO learn_book (id, name, code, level, description, cover_color, word_count, sort_no, status, source)
                        VALUES (?, ?, ?, ?, ?, ?, 0, ?, 1, 'KyleBing/english-vocabulary')
                        ON DUPLICATE KEY UPDATE
                            name = VALUES(name),
                            level = VALUES(level),
                            sort_no = VALUES(sort_no),
                            source = VALUES(source)
                        """,
                meta.id(),
                meta.level() + "核心词汇",
                meta.code(),
                meta.level(),
                meta.level() + "考试/课标核心词汇，由 KyleBing/english-vocabulary 整理。",
                meta.coverColor(),
                meta.sortNo());
    }

    /** 解析 JSONL 单行；无法解析返回 null。 */
    private ParsedWord parseWord(String line) {
        try {
            JsonNode node = objectMapper.readTree(line);
            String word = text(node, "word");
            if (word == null || word.isBlank()) {
                return null;
            }
            String phonetic = firstNonBlank(text(node, "us"), text(node, "uk"));
            Set<String> translations = readTranslations(node.get("translations"));
            Set<String> parts = readPartOfSpeech(node.get("translations"));

            String exampleEn = "";
            String exampleZh = "";
            JsonNode sentences = node.get("sentences");
            if (sentences != null && sentences.isArray() && !sentences.isEmpty()) {
                JsonNode first = sentences.get(0);
                exampleEn = text(first, "sentence");
                exampleZh = text(first, "translation");
            }
            if (exampleEn.isBlank()) {
                JsonNode phrases = node.get("phrases");
                if (phrases != null && phrases.isArray() && !phrases.isEmpty()) {
                    JsonNode first = phrases.get(0);
                    exampleEn = text(first, "phrase");
                    exampleZh = text(first, "translation");
                }
            }
            return new ParsedWord(word, phonetic, translations, parts, exampleEn, exampleZh);
        } catch (IOException e) {
            log.warn("解析失败，已跳过该行：{}", e.getMessage());
            return null;
        }
    }

    private void flush(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                        INSERT INTO learn_word
                            (book_id, word, phonetic, chinese, part_of_speech, example_en, example_zh, difficulty, level)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            phonetic = VALUES(phonetic),
                            chinese = VALUES(chinese),
                            part_of_speech = VALUES(part_of_speech),
                            example_en = VALUES(example_en),
                            example_zh = VALUES(example_zh),
                            difficulty = VALUES(difficulty),
                            level = VALUES(level)
                        """,
                batch);
        batch.clear();
    }

    private void refreshWordCount(Long bookId) {
        jdbcTemplate.update(
                "UPDATE learn_book SET word_count = (SELECT COUNT(*) FROM learn_word WHERE book_id = ?) WHERE id = ?",
                bookId, bookId);
    }

    private Set<String> readTranslations(JsonNode translations) {
        Set<String> result = new LinkedHashSet<>();
        if (translations == null || !translations.isArray()) {
            return result;
        }
        for (JsonNode item : translations) {
            String value = text(item, "translation");
            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private Set<String> readPartOfSpeech(JsonNode translations) {
        Set<String> result = new LinkedHashSet<>();
        if (translations == null || !translations.isArray()) {
            return result;
        }
        for (JsonNode item : translations) {
            String type = text(item, "type");
            if (type != null && !type.isBlank()) {
                result.add(type);
            }
        }
        return result;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return "";
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record BookMeta(Long id, String code, String level, int difficulty, String coverColor, int sortNo) {

        private BookMeta(Long id, String code, String level, int difficulty) {
            this(id, code, level, difficulty, defaultColor(code), 10 + (int) (id * 10));
        }

        private static String defaultColor(String code) {
            return switch (code) {
                case "CET4" -> "#6366f1";
                case "CET6" -> "#8b5cf6";
                case "KY" -> "#0ea5e9";
                case "TOEFL" -> "#f59e0b";
                case "IELTS" -> "#10b981";
                case "GRE" -> "#ef4444";
                case "SENIOR" -> "#ec4899";
                case "JUNIOR" -> "#22c55e";
                default -> "#6366f1";
            };
        }
    }

    /** 解析后的单词（同一单词在文件内出现多次时合并释义、词性与例句）。 */
    private record ParsedWord(
            String word,
            String phonetic,
            Set<String> translations,
            Set<String> parts,
            String exampleEn,
            String exampleZh) {

        private static ParsedWord merge(ParsedWord first, ParsedWord second) {
            Set<String> translations = new LinkedHashSet<>(first.translations);
            translations.addAll(second.translations);
            Set<String> parts = new LinkedHashSet<>(first.parts);
            parts.addAll(second.parts);
            String phonetic = firstNonBlank(first.phonetic, second.phonetic);
            String exampleEn = firstNonBlank(first.exampleEn, second.exampleEn);
            String exampleZh = firstNonBlank(first.exampleZh, second.exampleZh);
            return new ParsedWord(first.word, phonetic, translations, parts, exampleEn, exampleZh);
        }

        private Object[] toParams(BookMeta meta) {
            return new Object[]{
                    meta.id(),
                    word,
                    truncate(phonetic, 64),
                    truncate(normalizeChinese(translations), 255),
                    truncate(String.join("/", parts), 16),
                    truncate(exampleEn, 512),
                    truncate(exampleZh, 512),
                    meta.difficulty(),
                    meta.code()
            };
        }

        /**
         * 清洗合并后的中文释义：
         * 1. 按全角/半角分号拆分为独立释义；
         * 2. 去空白、去完全重复；
         * 3. 移除“被更长释义包含”的短释义（如保留“放弃，抛弃”，丢弃单独的“放弃”）。
         */
        private static String normalizeChinese(Set<String> rawTranslations) {
            LinkedHashSet<String> segments = new LinkedHashSet<>();
            for (String raw : rawTranslations) {
                for (String part : raw.split("[；;]")) {
                    String clean = part.trim()
                            .replaceAll("\\s+", " ")
                            .replaceAll("[，,] +", "，")
                            .replaceAll(" +[，,]", "，");
                    if (!clean.isEmpty()) {
                        segments.add(clean);
                    }
                }
            }
            List<String> list = new ArrayList<>(segments);
            List<String> keep = new ArrayList<>();
            for (String seg : list) {
                boolean contained = false;
                for (String other : list) {
                    if (!other.equals(seg) && other.contains(seg)) {
                        contained = true;
                        break;
                    }
                }
                if (!contained) {
                    keep.add(seg);
                }
            }
            return String.join("；", keep);
        }
    }
}
