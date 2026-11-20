import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

// 请求拦截：自动带上 token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = 'Bearer ' + token;
  }
  return config;
});

// 响应拦截：统一处理 Result 结构和未登录
api.interceptors.response.use(
  (resp) => {
    const body = resp.data;
    if (body.code !== 0) {
      if (body.code === 40100) {
        localStorage.removeItem('token');
      }
      return Promise.reject(new Error(body.message || '请求失败'));
    }
    return body.data;
  },
  (err) => Promise.reject(new Error('网络错误，请检查后端是否启动'))
);

export default api;
