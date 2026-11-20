package com.example.search.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.search.common.Result;
import com.example.search.model.Post;
import com.example.search.service.PostService;

@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /** 帖子列表（公开）：带作者名，浏览页面用 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(postService.listAllWithAuthor());
    }
}
