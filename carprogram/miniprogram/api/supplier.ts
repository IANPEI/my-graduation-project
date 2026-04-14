// api/supplier.ts
import { request } from "../utils/request";

// 获取服务商工作台数据
export const getSupplierHomeData = () => {
  return request({
    url: "/supplier/home",
    method: "GET",
  });
};

// 接单
export const acceptOrder = (orderNo: string) => {
  return request({
    url: `/supplier/order/accept/${orderNo}`,
    method: "POST",
  });
};