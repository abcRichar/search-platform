import React, { useState } from 'react';
import { Card, Form, Input, Button, Typography, message } from 'antd';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api';

export default function RegisterPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      await api.post('/user/register', values);
      message.success('注册成功，请登录');
      navigate('/login');
    } catch (e) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="注册" style={{ maxWidth: 400, margin: '60px auto' }}>
      <Form layout="vertical" onFinish={onFinish}>
        <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input />
        </Form.Item>
        <Form.Item
          name="password"
          label="密码（至少 6 位）"
          rules={[{ required: true, min: 6, message: '密码至少 6 位' }]}
        >
          <Input.Password />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>注册</Button>
      </Form>
      <Typography.Paragraph style={{ marginTop: 16, textAlign: 'center' }}>
        已有账号？<Link to="/login">去登录</Link>
      </Typography.Paragraph>
    </Card>
  );
}
