// api/user.ts
import { request } from "../utils/request";

// 车主登录（手机号+验证码）
export const userLogin = (data: { phone: string; code: string }) => {
  return request({
    url: "/user/login",
    method: "POST",
    data,
  });
};

// 获取我的订单列表
export const getMyOrderList = (data: { pageNum: number; pageSize: number }) => {
  return request({
    url: "/order/my",
    method: "GET",
    data,
  });
};

// 提交救援订单
export const createOrder = (data: {
  faultType: string;
  address: string;
  phone: string;
}) => {
  return request({
    url: "/order/create",
    method: "POST",
    data,
  });
};