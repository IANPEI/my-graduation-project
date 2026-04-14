import request from "@/utils/request";

// 服务商工作台数据
export function getSupplierHomeData() {
  return request({
    url: "/supplier/home",
    method: "get",
  });
}

// 获取服务商订单列表
export function getSupplierOrderList(params) {
  return request({
    url: "/supplier/order/list",
    method: "get",
    params,
  });
}

// 下载订单Excel
export function exportOrder(orderNo) {
  return request({
    url: "/supplier/export/" + orderNo,
    method: "get",
    responseType: "blob",
  });
}

//订单详情
export function getOrderDetail(orderNo) {
  return request({
    url: "/supplier/order/detail/" + orderNo,
    method: "get",
  });
}

// 接单
export function acceptOrder(orderNo) {
  return request({
    url: "/supplier/order/accept/" + orderNo,
    method: "post",
  });
}

// 完成订单
export function completeOrder(orderNo) {
  return request({
    url: "/supplier/order/complete/" + orderNo,
    method: "post",
  });
}

// 获取救援人员列表
export function getStaffList(params) {
  return request({
    url: "/supplier/staff/list",
    method: "get",
    params,
  });
}

// 切换人员在线状态
export function changeStaffStatus(id, status) {
  return request({
    url: "/supplier/staff/status/" + id,
    method: "post",
    data: { status },
  });
}

// 删除救援人员
export function deleteStaff(id) {
  return request({
    url: "/supplier/staff/delete/" + id,
    method: "delete",
  });
}

// 新增救援人员
export function addStaff(staffForm) {
  return request({
    url: "/supplier/staff/add",
    method: "post",
    data: staffForm,
  });
}

// 编辑救援人员
export function editStaff(staffForm) {
  return request({
    url: "/supplier/staff/edit",
    method: "post",
    data: staffForm,
  });
}

// 获取订单统计数据
export function getOrderStatistics() {
  return request({
    url: "/supplier/order/statistics",
    method: "get",
  });
}

// 获取订单评价
export function getOrderEvaluate(orderNo) {
  return request({
    url: "/supplier/evaluate/" + orderNo,
    method: "get",
  });
}
