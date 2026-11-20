package com.example.search.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.search.common.Result;
import com.example.search.model.User;
import com.example.search.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Long> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            throw new com.example.search.common.BusinessException("用户名和密码不能为空");
        }
        return Result.ok(userService.register(username, password));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            throw new com.example.search.common.BusinessException("用户名和密码不能为空");
        }
        return Result.ok(userService.login(username, password));
    }

    /** 当前登录用户信息（LoginInterceptor 已把 userId 放进 request） */
    @GetMapping("/me")
    public Result<User> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null); // 永远不把密码带出去
        }
        return Result.ok(user);
    }
}
