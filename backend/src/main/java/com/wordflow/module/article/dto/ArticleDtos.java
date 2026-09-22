package com.wordflow.module.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 短文模块数据传输对象。
 */
public final class ArticleDtos {

    private ArticleDtos() {
    }

    /** 短文翻译提交 */
    public record ArticleCheckRequest(
            @NotNull(message = "短文 ID 不能为空") Long articleId,
            @NotBlank(message = "译文不能为空")
            @Size(max = 20000, message = "译文最长 20000 字符") String userTranslation
    ) {
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
