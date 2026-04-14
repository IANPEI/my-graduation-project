import request from "@/utils/request";

// 登录（移除verifyCode参数）
export function login(data) {
  return request({
    url: "/login",
    method: "post",
    data, // 仅传 account 和 password
  });
}

// 登出
export function logout() {
  return request({
    url: "/login/logout",
    method: "post",
  });
}
