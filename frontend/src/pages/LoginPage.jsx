import React, { useState } from 'react';
import { Card, Form, Input, Button, Typography, message } from 'antd';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      const data = await api.post('/user/login', values);
      login(data);
      message.success('登录成功');
      navigate('/');
    } catch (e) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="登录" style={{ maxWidth: 400, margin: '60px auto' }}>
      <Form layout="vertical" onFinish={onFinish}>
        <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input placeholder="演示账号：admin" />
        </Form.Item>
        <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
          <Input.Password placeholder="演示密码：123456" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>登录</Button>
      </Form>
      <Typography.Paragraph style={{ marginTop: 16, textAlign: 'center' }}>
        没有账号？<Link to="/register">去注册</Link>
      </Typography.Paragraph>
    </Card>
  );
}
