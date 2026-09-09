package com.wordflow.module.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置项（application.yml 的 ai.*）。
 *
 * 你需要完成：
 *   - provider=openai 时，通过环境变量 AI_API_KEY 注入密钥；
 *   - 若使用国内大模型（通义千问/DeepSeek 等），base-url 与 model 替换即可。
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

