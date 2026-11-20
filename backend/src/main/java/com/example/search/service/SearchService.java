package com.example.search.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import com.example.search.doc.PostDocument;
import com.example.search.doc.UserDocument;

/**
 * 聚合搜索核心：一次请求，同时查 posts 索引、users 索引、必应图片。
 * 每个数据源各自实现 searchXxx 方法 —— 这就是"中台"的扩展点：
 * 以后要接入新数据源（比如视频），只要再加一个 searchXxx + 在聚合结果里加一个 key。
 */
@Service
public class SearchService {

    private final ElasticsearchOperations operations;
    private final ImageSearchService imageSearchService;

    public SearchService(ElasticsearchOperations operations, ImageSearchService imageSearchService) {
        this.operations = operations;
        this.imageSearchService = imageSearchService;
    }

    public List<PostDocument> searchPosts(String q, int page, int size) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        // 限制分页参数防止过大请求
        size = Math.min(size, 100);
        page = Math.max(0, page);

        NativeQuery query = NativeQuery.builder()
                .withQuery(b -> b.multiMatch(m -> m
                        .fields("title^2", "content", "tags^1.5")
                        .query(q)))
                .withPageable(PageRequest.of(page, size))
                .build();
        SearchHits<PostDocument> hits = operations.search(query, PostDocument.class);
        return hits.getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public List<UserDocument> searchUsers(String q, int page, int size) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        size = Math.min(size, 100);
        page = Math.max(0, page);

        NativeQuery query = NativeQuery.builder()
                .withQuery(b -> b.multiMatch(m -> m
                        .fields("username")
                        .query(q)))
                .withPageable(PageRequest.of(page, size))
                .build();
        SearchHits<UserDocument> hits = operations.search(query, UserDocument.class);
        return hits.getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public List<java.util.Map<String, String>> searchImages(String q, int limit) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        limit = Math.min(limit, 50);
        return imageSearchService.search(q, limit);
    }
}
