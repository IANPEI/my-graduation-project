import axios from "axios";
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router";

// 创建 Axios 实例
const service = axios.create({
  // 核心修改：匹配后端端口+上下文路径
  baseURL: "http://localhost:8089/rescue_admin",
  timeout: 5000, // 请求超时时间
  headers: {
    "Content-Type": "application/json;charset=utf-8",
  },
});

// 请求拦截器：添加 token
service.interceptors.request.use(
  (config) => {
    // 从本地存储获取 token
    const token = localStorage.getItem("token");
    if (token) {
      config.headers["Authorization"] = "Bearer " + token;
    }
    return config;
  },
  (error) => {
    console.error("请求错误：", error);
    return Promise.reject(error);
  },
);

// 响应拦截器：统一处理响应
service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === "blob") {
      return response.data;
    }

    const res = response.data;

    // 后端返回码 200 表示成功
    if (res.code !== 200) {
      ElMessage.error(res.msg || "请求失败");

      // 401 未授权
      if (res.code === 401) {
        ElMessageBox.confirm("登录已过期，请重新登录", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }).then(() => {
          localStorage.removeItem("token");
          localStorage.removeItem("identity");
          router.push("/login");
        });
      }

      return Promise.reject(res);
    }

    return res;
  },
  (error) => {
    console.error("响应错误：", error);

    if (error.response && error.response.status === 401) {
      ElMessageBox.confirm("登录已过期，请重新登录", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        localStorage.clear();
        router.push("/login");
      });
    }

    ElMessage.error(error.message || "服务器错误");
    return Promise.reject(error);
  },
);

export default service;
