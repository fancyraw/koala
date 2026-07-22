package com.koala.common.constant;

/**
 * sys_config 表的 (group, key) 常量集中定义，避免散落的 magic string。
 * 默认值仍在调用处指定（不同业务场景默认可能不同）。
 */
public final class ConfigKeys {

    private ConfigKeys() {}

    /** group=order */
    public static final class Order {
        public static final String GROUP = "order";
        public static final String PAY_TIMEOUT_MINUTES = "pay_timeout_minutes";
        public static final String AUTO_CONFIRM_DAYS = "auto_confirm_days";
        private Order() {}
    }

    /** group=payment */
    public static final class Payment {
        public static final String GROUP = "payment";
        public static final String ACTIVE_CHANNEL = "active_channel";
        private Payment() {}
    }

    /** group=shipping */
    public static final class Shipping {
        public static final String GROUP = "shipping";
        public static final String BASE_FEE = "base_fee";
        public static final String FREE_THRESHOLD = "free_threshold";
        private Shipping() {}
    }

    /** group=system */
    public static final class System {
        public static final String GROUP = "system";
        public static final String MAINTENANCE_MODE = "maintenance_mode";
        public static final String MAINTENANCE_NOTICE = "maintenance_notice";
        private System() {}
    }
}
