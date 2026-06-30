package com.koala.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "koala.jwt")
public class JwtProperties {

    private String secret;
    private long userTtlMinutes = 4320;
    private long adminTtlMinutes = 720;
}
