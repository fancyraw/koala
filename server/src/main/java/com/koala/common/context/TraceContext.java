package com.koala.common.context;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪 ID 上下文，写入 MDC 供日志 pattern 输出，同时回填统一响应体。
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";

    private TraceContext() {
    }

    public static String generate() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    public static void set(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
