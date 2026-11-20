package com.example.search.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.search.common.AdminInterceptor;
import com.example.search.common.LoginInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor, AdminInterceptor adminInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // /api/user/me 需要登录；/api/admin/** 需要登录 + 管理员（两个拦截器依次执行）
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/user/me", "/api/admin/**");
        registry.addInterceptor(adminInterceptor).addPathPatterns("/api/admin/**");
    }
}
