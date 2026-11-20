package com.example.search.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.search.common.Result;
import com.example.search.doc.PostDocument;
import com.example.search.doc.UserDocument;
import com.example.search.service.SearchService;

/** 聚合搜索接口：一次请求返回所有数据源的结果 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/all")
    public Result<Map<String, Object>> searchAll(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (q == null || q.isBlank()) {
            throw new com.example.search.common.BusinessException("搜索关键词不能为空");
        }
        if (q.length() > 200) {
            throw new com.example.search.common.BusinessException("搜索关键词过长");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<PostDocument> posts = searchService.searchPosts(q, page, size);
        List<UserDocument> users = searchService.searchUsers(q, page, size);
        List<Map<String, String>> images = searchService.searchImages(q, size);

        result.put("posts", posts);
        result.put("users", users);
        result.put("images", images);
        result.put("total", posts.size() + users.size() + images.size());
        result.put("page", page);
        result.put("size", size);
        return Result.ok(result);
    }
}
