package com.koala.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.Result;
import com.koala.service.ConfigService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 维护模式拦截器(见 6.9)：置于鉴权切面之后。
 * 开(system.maintenance_mode=1)时拦截 C 端写操作(POST)，返回 5001 + 维护文案；
 * 放行：GET 查询、全部 /admin/*（后台照常运维）、支付回调 /order/pay-notify（三方已扣款不可拒）。
 * 判断走 ConfigService 本地缓存，无 DB/Redis 额外开销。
 */
@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    private static final String DEFAULT_NOTICE = "系统维护中，请稍后再试";

    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public MaintenanceInterceptor(ConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (configService.getInt("system", "maintenance_mode", 0) != 1) {
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        // 后台照常运维；支付回调不可拒(资产一致)
        if (path.startsWith("/admin/") || "/order/pay-notify".equals(path)) {
            return true;
        }
        String notice = configService.get("system", "maintenance_notice", DEFAULT_NOTICE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(ErrorCode.MAINTENANCE.getCode(), notice)));
        return false;
    }
}
