import React from 'react';
import { Routes, Route, Link, useNavigate } from 'react-router-dom';
import { Layout, Button, Space, Typography } from 'antd';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminPage from './pages/AdminPage';
import { useAuth } from './context/AuthContext';

const { Header, Content } = Layout;

export default function App() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ background: '#fff', borderBottom: '1px solid #eee', display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingInline: 24 }}>
        <Link to="/" style={{ fontSize: 18, fontWeight: 600, color: '#185FA5', textDecoration: 'none' }}>
          聚合搜索平台
        </Link>
        <Space>
          {isAdmin && <Button type="link" onClick={() => navigate('/admin')}>帖子管理</Button>}
          {user ? (
            <>
              <Typography.Text>你好，{user.username}</Typography.Text>
              <Button onClick={() => { logout(); navigate('/'); }}>退出</Button>
            </>
          ) : (
            <>
              <Button type="link" onClick={() => navigate('/login')}>登录</Button>
              <Button type="primary" onClick={() => navigate('/register')}>注册</Button>
            </>
          )}
        </Space>
      </Header>
      <Content style={{ padding: '24px 24px 48px' }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/admin" element={<AdminPage />} />
        </Routes>
      </Content>
    </Layout>
  );
}
