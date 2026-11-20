package com.example.search.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 管理员拦截器：跟在登录拦截器后面，校验 role = admin */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(
                    objectMapper.writeValueAsString(Result.error(40300, "无管理员权限"))
                            .getBytes(StandardCharsets.UTF_8));
            return false;
        }
        return true;
    }
}
