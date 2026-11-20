package com.example.search.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：校验请求头 Authorization 里的 JWT。
 * 校验通过后把 userId 放进 request attribute，供 Controller 获取当前用户。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            return reject(response, 40100, "未登录");
        }
        try {
            Claims claims = jwtUtil.parse(token);
            request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            request.setAttribute("role", claims.get("role", String.class));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return reject(response, 40100, "登录已过期，请重新登录");
        }
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                objectMapper.writeValueAsString(Result.error(code, msg)).getBytes(StandardCharsets.UTF_8));
        return false;
    }
}
