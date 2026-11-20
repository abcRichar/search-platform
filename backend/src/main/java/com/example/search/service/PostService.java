package com.example.search.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.search.common.BusinessException;
import com.example.search.doc.PostDocument;
import com.example.search.doc.PostDocumentRepository;
import com.example.search.mapper.PostMapper;
import com.example.search.mapper.UserMapper;
import com.example.search.model.Post;
import com.example.search.model.User;

@Service
public class PostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostDocumentRepository postEs;

    public PostService(PostMapper postMapper, UserMapper userMapper, PostDocumentRepository postEs) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.postEs = postEs;
    }

    public List<Post> listAll() {
        return postMapper.findAll();
    }

    public List<java.util.Map<String, Object>> listAllWithAuthor() {
        return postMapper.findAllWithAuthor();
    }

    public Post add(Post post, Long userId) {
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            throw new BusinessException("标题不能为空");
        }
        if (post.getContent() != null && post.getContent().length() > 10000) {
            throw new BusinessException("内容过长，不能超过10000字符");
        }
        post.setUserId(userId);
        postMapper.insert(post);
        // insert后立即同步，使用刚插入的对象而非再次查询
        syncOne(post);
        return post;
    }

    public Post update(Post post, Long userId) {
        Post existing = postMapper.findById(post.getId());
        if (existing == null) {
            throw new BusinessException("帖子不存在");
        }
        // 权限校验：只能修改自己的帖子
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("无权修改他人的帖子");
        }
        if (post.getContent() != null && post.getContent().length() > 10000) {
            throw new BusinessException("内容过长，不能超过10000字符");
        }
        postMapper.update(post);
        Post updated = postMapper.findById(post.getId());
        syncOne(updated);
        return updated;
    }

    public void delete(Long id) {
        postMapper.delete(id);
        postEs.deleteById(id);
    }

    /** 单条立即同步 MySQL -> ES，保证管理员改完马上能搜到 */
    private void syncOne(Post post) {
        postEs.save(toDocument(post));
    }

    public PostDocument toDocument(Post post) {
        PostDocument doc = new PostDocument();
        doc.setId(post.getId());
        doc.setTitle(post.getTitle());
        doc.setContent(post.getContent());
        doc.setTags(post.getTags());
        doc.setUserId(post.getUserId());
        doc.setCreateTime(post.getCreateTime());
        return doc;
    }

    /** 给列表接口补充作者名 */
    public String authorName(Post post) {
        if (post.getUserId() == null) return "";
        User u = userMapper.findById(post.getUserId());
        return u == null ? "" : u.getUsername();
    }
}
