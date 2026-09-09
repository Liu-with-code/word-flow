package com.wordflow.module.article.dto;

import java.util.List;

/**
 * 短文模块数据传输对象。
 */
public final class ArticleDtos {

    private ArticleDtos() {
    }

    /** 短文翻译提交 */
    public record ArticleCheckRequest(Long articleId, String userTranslation) {
    }

    /** 短文批改结果 */
    public record ArticleCheckResult(
            Long articleId,
            boolean passed,
            int score,
            String comment,
            List<ErrorItem> errors,
            String standardZh
    ) {
    }

    /** 错误明细 */
    public record ErrorItem(String segment, String expected, String user, String suggestion) {
    }
}

