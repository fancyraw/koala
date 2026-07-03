package com.koala.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.Result;
import com.koala.entity.Admin;
import com.koala.entity.User;
import com.koala.enums.ValidFlag;
import com.koala.repository.AdminRepository;
import com.koala.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 鉴权切面：校验 JWT → 黑名单 → 账号有效性 → 后台超管能力位，写入 {@link AuthContext}。
 * 注意：servletPath 已不含 context-path(/api/v1)，故规则以业务路径书写。
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 免鉴权路径（业务路径，不含 /api/v1 前缀）。 */
    private static final String[] WHITELIST = {
            "/user/login",
            "/admin/login/qrcode",
            "/admin/login/check",
            "/admin/login/callback",
            "/admin/admins/accept",
            "/order/pay-notify",
            "/regions",
            "/categories",
            "/products",
            "/products/detail",
            "/error",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**"
    };

    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, TokenBlacklist blacklist,
                           UserRepository userRepository, AdminRepository adminRepository, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getServletPath();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isWhitelisted(path)) {
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return reject(response, ErrorCode.UNAUTHORIZED);
        }

        JwtUtil.ParsedToken parsed = jwtUtil.parse(token);
        if (parsed == null || parsed.principal == null) {
            return reject(response, ErrorCode.TOKEN_INVALID);
        }
        if (blacklist.isRevoked(parsed.jti)) {
            return reject(response, ErrorCode.UNAUTHORIZED);
        }

        Principal principal = parsed.principal;
        boolean isAdminPath = path.startsWith("/admin/");

        if (isAdminPath) {
            if (!principal.isAdmin()) {
                return reject(response, ErrorCode.FORBIDDEN);
            }
            Admin admin = adminRepository.findById(principal.getId());
            if (admin == null || !ValidFlag.ENABLED.is(admin.getIsValid())) {
                return reject(response, ErrorCode.ACCOUNT_DISABLED);
            }
            // 「管理员管理」仅超管可操作（能力位，非角色分层）
            if (MATCHER.match("/admin/admins/**", path) && !principal.isSuperAdmin()) {
                return reject(response, ErrorCode.FORBIDDEN);
            }
        } else {
            if (!principal.isUser()) {
                return reject(response, ErrorCode.FORBIDDEN);
            }
            User user = userRepository.findById(principal.getId());
            if (user == null || !ValidFlag.ENABLED.is(user.getIsValid())) {
                return reject(response, ErrorCode.ACCOUNT_DISABLED);
            }
        }

        AuthContext.set(principal);
        return true;
    }

    private boolean reject(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode)));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER)) {
            return header.substring(BEARER.length());
        }
        return null;
    }
}
