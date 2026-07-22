package com.koala.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 全局时钟 bean：所有业务代码用 {@code LocalDateTime.now(clock)} 取当前时间，
 * 与 Jackson / MySQL serverTimezone 保持一致（Asia/Shanghai），
 * 同时留出测试注入 {@link Clock#fixed(java.time.Instant, ZoneId)} 的口子。
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
