package com.koala.common.auth;

import com.koala.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。subject=主体ID，claim 含 type/isSuper，jti 作登出黑名单键。
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_SUPER = "isSuper";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtUtil(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueUserToken(Long userId) {
        return issue(Principal.TYPE_USER, userId, false, props.getUserTtlMinutes());
    }

    public String issueAdminToken(Long adminId, boolean isSuper) {
        return issue(Principal.TYPE_ADMIN, adminId, isSuper, props.getAdminTtlMinutes());
    }

    private String issue(String type, Long id, boolean isSuper, long ttlMinutes) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString().replace("-", ""))
                .setSubject(String.valueOf(id))
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_SUPER, isSuper)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMinutes * 60_000))
                .signWith(key)
                .compact();
    }

    /** 解析并验签，失败返回 null。 */
    public ParsedToken parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            Claims c = jws.getBody();
            Principal principal = new Principal(
                    c.get(CLAIM_TYPE, String.class),
                    Long.valueOf(c.getSubject()),
                    Boolean.TRUE.equals(c.get(CLAIM_SUPER, Boolean.class)));
            return new ParsedToken(c.getId(), principal, c.getExpiration());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static class ParsedToken {
        public final String jti;
        public final Principal principal;
        public final Date expiration;

        public ParsedToken(String jti, Principal principal, Date expiration) {
            this.jti = jti;
            this.principal = principal;
            this.expiration = expiration;
        }

        public long remainingSeconds() {
            long ms = expiration.getTime() - System.currentTimeMillis();
            return ms > 0 ? ms / 1000 : 0;
        }
    }
}
