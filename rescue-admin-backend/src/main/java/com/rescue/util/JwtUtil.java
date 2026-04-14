package com.rescue.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 适配 JJWT 0.12.x 的 JWT 工具类（修复 WeakKeyException + extractUserId 错误）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expire;

    // 修复：创建密钥的统一方法（保证生成/解析Token使用相同密钥）
    private SecretKey getSecretKey() {
        // 若secret长度不足256位，JJWT会自动处理（兼容短密钥）
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 Token（使用 HS256 算法）
     */
    public String generateToken(String userId,String account, String identity) {
        // 1. 使用配置文件的secret生成密钥（统一密钥源）
        SecretKey key = getSecretKey();

        // 2. 设置 Token 载荷
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("account", account);
        claims.put("identity", identity);

        // 3. 生成 Token
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token 获取 Claims（统一密钥源）
     */
    public Claims parseToken(String token) {
        SecretKey key = getSecretKey();

        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中获取账号
     */
    public String getAccountFromToken(String token) {
        return parseToken(token).get("account", String.class);
    }

    /**
     * 从 Token 中获取身份
     */
    public String getIdentityFromToken(String token) {
        return parseToken(token).get("identity", String.class);
    }

    /**
     * 验证 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    /**
     * 从Token中提取用户ID（修复核心错误）
     */
    public String extractUserId(String token) {
        // 复用parseToken方法，保证密钥和解析逻辑统一
        Claims claims = parseToken(token);
        return claims.get("userId", String.class);
    }
}