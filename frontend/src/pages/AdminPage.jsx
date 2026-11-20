import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, message, Popconfirm, Space, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../context/AuthContext';

export default function AdminPage() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();
  const { isAdmin, user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    // 非管理员直接挡掉（后端接口本身也有拦截器兜底）
    if (user && !isAdmin) {
      message.error('无管理员权限');
      navigate('/');
    }
  }, [user, isAdmin]);

  const load = async () => {
    setLoading(true);
    try {
      setPosts(await api.get('/post/list'));
    } catch (e) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openAdd = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await api.put('/admin/post/update', { ...editing, ...values });
        message.success('更新成功，ES 已同步');
      } else {
        await api.post('/admin/post/add', values);
        message.success('新增成功，ES 已同步');
      }
      setModalOpen(false);
      load();
    } catch (e) {
      message.error(e.message);
    }
  };

  const remove = async (id) => {
    try {
      await api.delete('/admin/post/delete/' + id);
      message.success('删除成功');
      load();
    } catch (e) {
      message.error(e.message);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '标题', dataIndex: 'title', width: 220, ellipsis: true },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '标签', dataIndex: 'tags', width: 160, ellipsis: true },
    { title: '作者', dataIndex: 'author', width: 100 },
    {
      title: '操作', width: 150,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除？" onConfirm={() => remove(record.id)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>帖子管理（改动会自动同步到 Elasticsearch）</Typography.Title>
        <Button type="primary" onClick={openAdd}>新增帖子</Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={posts} loading={loading} />
      <Modal
        title={editing ? '编辑帖子' : '新增帖子'}
        open={modalOpen}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true, message: '请输入内容' }]}>
            <Input.TextArea rows={5} />
          </Form.Item>
          <Form.Item name="tags" label="标签（逗号分隔）">
            <Input placeholder="java,后端" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
