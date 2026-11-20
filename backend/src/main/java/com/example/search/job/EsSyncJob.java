package com.example.search.job;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.search.doc.PostDocument;
import com.example.search.doc.PostDocumentRepository;
import com.example.search.doc.UserDocument;
import com.example.search.doc.UserDocumentRepository;
import com.example.search.mapper.PostMapper;
import com.example.search.mapper.UserMapper;
import com.example.search.service.PostService;

/**
 * 定时全量同步：MySQL(主数据) -> ES(搜索层)。
 *
 * 为什么有了"管理员保存时立即同步"还需要它：
 * 1. 兜底人工改库 / 直接 SQL 修数据的场景
 * 2. ES 索引被误删后自动重建数据
 *
 * 数据量大时这里应该改成"按 update_time 增量同步"，demo 全量足够。
 */
@Component
public class EsSyncJob {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostService postService;
    private final PostDocumentRepository postEs;
    private final UserDocumentRepository userEs;

    public EsSyncJob(PostMapper postMapper, UserMapper userMapper, PostService postService,
                     PostDocumentRepository postEs, UserDocumentRepository userEs) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.postService = postService;
        this.postEs = postEs;
        this.userEs = userEs;
    }

    @Scheduled(fixedDelayString = "${app.sync.interval-ms}", initialDelay = 15000)
    public void sync() {
        List<PostDocument> postDocs = postMapper.findAll().stream()
                .map(postService::toDocument).toList();
        if (!postDocs.isEmpty()) {
            postEs.saveAll(postDocs);
        }
        List<UserDocument> userDocs = userMapper.findAll().stream().map(u -> {
            UserDocument d = new UserDocument();
            d.setId(u.getId());
            d.setUsername(u.getUsername());
            d.setAvatarUrl(u.getAvatarUrl());
            return d;
        }).toList();
        if (!userDocs.isEmpty()) {
            userEs.saveAll(userDocs);
        }
    }
}
