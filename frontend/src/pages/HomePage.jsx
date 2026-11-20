import React, { useState } from 'react';
import { Input, Tabs, List, Avatar, Tag, Card, Typography, Empty, Spin, Image, Space } from 'antd';
import api from '../api';

const { Search } = Input;
const { Title, Paragraph } = Typography;

export default function HomePage() {
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [keyword, setKeyword] = useState('');

  const doSearch = async (value) => {
    const trimmed = value?.trim();
    if (!trimmed) return;
    if (trimmed.length > 200) {
      setResult({ posts: [], users: [], images: [], error: '搜索关键词过长' });
      return;
    }
    setLoading(true);
    try {
      const data = await api.get('/search/all', { params: { q: trimmed, page: 0, size: 20 } });
      setResult(data);
      setKeyword(trimmed);
    } catch (e) {
      setResult({ posts: [], users: [], images: [], error: e.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 960, margin: '0 auto' }}>
      <div style={{ textAlign: 'center', margin: '48px 0 32px' }}>
        <Title level={2} style={{ color: '#185FA5' }}>一站式聚合搜索</Title>
        <Paragraph type="secondary">同时搜索帖子、用户、必应图片——一个框，全部结果</Paragraph>
        <Search
          size="large"
          placeholder="输入关键词，例如：elasticsearch / 张三 / 手机壁纸"
          enterButton="搜索"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onSearch={doSearch}
          style={{ maxWidth: 560 }}
        />
      </div>

      <Spin spinning={loading}>
        {result && (
          <Tabs
            defaultActiveKey="posts"
            items={[
              {
                key: 'posts',
                label: `帖子（${result.posts?.length || 0}）`,
                children: <PostResults posts={result.posts} keyword={keyword} />
              },
              {
                key: 'users',
                label: `用户（${result.users?.length || 0}）`,
                children: <UserResults users={result.users} />
              },
              {
                key: 'images',
                label: `图片（${result.images?.length || 0}）`,
                children: <ImageResults images={result.images} />
              }
            ]}
          />
        )}
      </Spin>
    </div>
  );
}

function highlight(text, keyword) {
  if (!text) return '';
  if (!keyword) return text;
  const parts = text.split(new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'));
  return parts.map((p, i) =>
    p.toLowerCase() === keyword.toLowerCase()
      ? <span key={i} style={{ color: '#c0392b', fontWeight: 600 }}>{p}</span>
      : p
  );
}

function PostResults({ posts, keyword }) {
  if (!posts?.length) return <Empty description="没有找到相关帖子" />;
  return (
    <List
      itemLayout="vertical"
      dataSource={posts}
      renderItem={(p) => (
        <List.Item>
          <Card size="small" hoverable>
            <Title level={5} style={{ marginTop: 0 }}>{highlight(p.title, keyword)}</Title>
            <Paragraph type="secondary">{highlight(p.content, keyword)}</Paragraph>
            <Space wrap>
              {p.tags?.split(',').filter(Boolean).map((t) => <Tag key={t} color="blue">{t}</Tag>)}
              <Typography.Text type="secondary">ID: {p.id}</Typography.Text>
            </Space>
          </Card>
        </List.Item>
      )}
    />
  );
}

function UserResults({ users }) {
  if (!users?.length) return <Empty description="没有找到相关用户" />;
  return (
    <List
      dataSource={users}
      renderItem={(u) => (
        <List.Item>
          <List.Item.Meta
            avatar={<Avatar size={48} src={u.avatarUrl}>{u.username?.[0]}</Avatar>}
            title={u.username}
            description={'用户 ID: ' + u.id}
          />
        </List.Item>
      )}
    />
  );
}

function ImageResults({ images }) {
  if (!images?.length) return <Empty description="图片搜索没有结果（必应可能被网络拦截或接口改版）" />;
  return (
    <Image.PreviewGroup>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 12 }}>
        {images.map((img, i) => (
          <Card key={i} size="small" hoverable
            cover={
              <Image
                src={img.thumbnail}
                fallback="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxODAiIGhlaWdodD0iMTIwIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZWVlIi8+PHRleHQgeD0iOTAiIHk9IjYwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5IiBmb250LXNpemU9IjEyIj7lrb7jgILljYrkur3mlL3lv708L3RleHQ+PC9zdmc+"
                style={{ objectFit: 'cover', height: 140, width: '100%' }}
              />
            }>
            <Card.Meta
              title={<span style={{ fontSize: 12 }}>{(img.title || '').slice(0, 30)}</span>}
              description={<a href={img.url} target="_blank" rel="noreferrer" style={{ fontSize: 12 }}>查看原图</a>}
            />
          </Card>
        ))}
      </div>
    </Image.PreviewGroup>
  );
}
