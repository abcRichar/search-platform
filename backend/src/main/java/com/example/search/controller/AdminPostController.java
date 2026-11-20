package com.example.search.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.search.common.Result;
import com.example.search.model.Post;
import com.example.search.service.PostService;

/**
 * 管理端接口：挂在 /api/admin/** 下，
 * 由 LoginInterceptor + AdminInterceptor 双重拦截（登录 + admin 角色）。
 */
@RestController
@RequestMapping("/api/admin/post")
public class AdminPostController {

    private final PostService postService;

    public AdminPostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/add")
    public Result<Post> add(@RequestBody Post post, jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(postService.add(post, userId));
    }

    @PutMapping("/update")
    public Result<Post> update(@RequestBody Post post, jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(postService.update(post, userId));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.ok(null);
    }
}
