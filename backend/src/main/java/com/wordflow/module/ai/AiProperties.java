package com.wordflow.module.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置项（application.yml 的 ai.*）。
 *
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** mock：离线规则模拟；openai：调用 OpenAI 兼容接口 */
    private String provider = "mock";

    private String baseUrl = "https://api.openai.com/v1";

    private String apiKey = "";

    private String model = "gpt-4o-mini";

    private long timeoutSeconds = 60;
}

