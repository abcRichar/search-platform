package com.example.search.service;

import org.springframework.util.DigestUtils;

import com.example.search.common.BusinessException;
import com.example.search.common.JwtUtil;
import com.example.search.mapper.UserMapper;
import com.example.search.model.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    /** 简单加盐：固定盐值 + MD5。生产环境强烈建议改用 BCrypt */
    private static final String SALT = "search-platform";
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_PASSWORD_LENGTH = 100;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public Long register(String username, String password) {
        // 输入验证
        if (username == null || username.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new BusinessException("用户名过长，不能超过" + MAX_USERNAME_LENGTH + "字符");
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            throw new BusinessException("用户名只能包含字母、数字、下划线和中文");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException("密码过长");
        }

        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encrypt(password));
        user.setRole("user");
        user.setAvatarUrl(null);
        userMapper.insert(user);
        return user.getId();
    }

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("用户名或密码不能为空");
        }

        User user = userMapper.findByUsername(username);
        if (user == null || !user.getPassword().equals(encrypt(password))) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generate(user.getId(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("avatarUrl", user.getAvatarUrl());
        return result;
    }

    public User getById(Long id) {
        if (id == null) {
            return null;
        }
        return userMapper.findById(id);
    }

    private String encrypt(String raw) {
        return DigestUtils.md5DigestAsHex((SALT + raw).getBytes(StandardCharsets.UTF_8));
    }
}
