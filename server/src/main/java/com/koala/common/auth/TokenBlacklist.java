package com.koala.common.auth;

import com.koala.common.constant.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 登出黑名单：以 jti 为键写入 Redis，TTL 取 token 剩余有效期，过期自然消失。
 */
@Component
public class TokenBlacklist {

    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) {
            return;
        }
        redis.opsForValue().set(RedisKeys.AUTH_BLACKLIST + jti, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(RedisKeys.AUTH_BLACKLIST + jti));
    }
}
