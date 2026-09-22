package com.wordflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 全局时钟配置。
 *
 *     单元测试中注入 Clock.fixed(...) 即可让时间逻辑完全可复现，不依赖真实时间。
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
