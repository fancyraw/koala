package com.koala.common.constant;

/**
 * Redis key / 分布式锁前缀集中定义。所有 setNX / hset / getLock 的 key 前缀都从此处取，
 * 避免同一命名空间在多个 Service 里各自定义、语义漂移。
 */
public final class RedisKeys {

    // --- 业务数据 / 缓存 ---
    public static final String AUTH_BLACKLIST = "auth:blacklist:";
    public static final String ADMIN_LOGIN_SESSION = "admin:login:session:";
    public static final String ADMIN_INVITE_TOKEN = "admin:invite:token:";
    public static final String ADMIN_DASHBOARD = "admin:dashboard:";
    public static final String ORDER_SUBMIT_TOKEN = "order:submit:";

    // --- 分布式锁（Redisson getLock 的 key） ---
    public static final String LOCK_ORDER_SUBMIT = "lock:order:submit:";
    public static final String LOCK_COUPON_GRANT = "lock:coupon:grant:";

    private RedisKeys() {}
}
