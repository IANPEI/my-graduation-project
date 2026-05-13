import request from "@/utils/request";

// 管理员工作台数据
export function getAdminHomeData() {
  return request({
    url: "/admin/home",
    method: "get",
  });
}

// 获取服务商列表
export function getAdminSupplierList(params) {
  return request({
    url: "/admin/supplier/list",
    method: "get",
    params,
  });
}

// 切换服务商状态
export function changeSupplierStatus(id, status) {
  return request({
    url: "/admin/supplier/status/" + id,
    method: "post",
    data: { status },
  });
}

// 获取服务商详情
export function getSupplierDetail(id) {
  return request({
    url: `/admin/supplier/detail/${id}`,
    method: "get",
  });
}

// 校验服务商ID是否唯一
export function checkSupplierIdUnique(id) {
  return request({
    url: "/admin/supplier/checkIdUnique",
    method: "get",
    params: { id },
  });
}

// 新增服务商
export function saveSupplier(data) {
  return request({
    url: "/admin/supplier/save",
    method: "post",
    data,
  });
}

// 编辑服务商
export function updateSupplier(data) {
  return request({
    url: "/admin/supplier/update",
    method: "post",
    data,
  });
}

// 获取系统配置
export function getSystemConfig() {
  return request({
    url: "/admin/system/config",
    method: "get",
  });
}

// 保存系统配置
export function saveSystemConfig(data) {
  return request({
    url: "/admin/system/config/save",
    method: "post",
    data,
  });
}

// 获取故障类型列表
export function getFaultTypeList() {
  return request({
    url: "/admin/system/faultType/list",
    method: "get",
  });
}

// 删除故障类型
export function deleteFaultType(id) {
  return request({
    url: "/admin/system/faultType/delete/" + id,
    method: "delete",
  });
}

export function getFaultTypeDetail(id) {
  return request({
    url: `/admin/system/faultType/detail/${id}`,
    method: "get",
  });
}

// 新增故障类型
export function saveFaultType(data) {
  return request({
    url: "/admin/system/faultType/save",
    method: "post",
    data,
  });
}

// 编辑故障类型
export function updateFaultType(data) {
  return request({
    url: "/admin/system/faultType/update",
    method: "post",
    data,
  });
}

// 服务商审核
export function approveSupplier(id, status) {
  return request({
    url: `/admin/supplier/audit/${id}`,
    method: "POST",
    data: { status },
  });
}

// 获取用户列表（车主/服务商/管理员）
export function getUserList(params) {
  return request({
    url: "/admin/user/list",
    method: "get",
    params,
  });
}

// 获取服务商好评率
export function getSupplierEvaluateList(params) {
  return request({
    url: "/admin/supplier/evaluate/list",
    method: "get",
    params,
  });
}

export function getAdminOrderList(params) {
  return request({
    url: "/admin/order/list",
    method: "GET",
    params,
  });
}
