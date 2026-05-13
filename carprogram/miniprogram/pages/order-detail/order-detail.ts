import { request } from "../../utils/request";

interface OrderDetail {
  orderNo: string;
  faultTypeId?: number;
  faultTypeName?: string;
  username: string;
  phone: string;
  address: string;
  status: string;
  statusText?: string;
  createTime: string;
  createTimeText?: string;
  updateTime: string;
  cancelTime?: string;
  supplierId?: string;
  supplierName?: string;
  supplierContact?: string;
  supplierPhone?: string;
  staffName?: string;
  staffPhone?: string;
  staffSkill?: string;
  userId?: number;
  latitude?: number;
  longitude?: number;
  remark?: string;
  evaluateStatus: number;
  evaluate_status?: number;
}

interface MapMarker {
  id: number;
  latitude: number;
  longitude: number;
  iconPath: string;
  width: number;
  height: number;
}

Page({
  data: {
    orderDetail: {} as OrderDetail,
    orderNo: "",
    loading: true,
    showEvaluate: false,
    evaluateForm: { score: 5, content: "" },
    lat: 23.0,
    lng: 113.0,
    markers: [] as MapMarker[],
    trackTimer: null as number | null
  },

  onLoad(options) {
    const { orderNo } = options;
    if (!orderNo) {
      wx.showToast({ title: "订单编号不能为空", icon: "none" });
      wx.navigateBack();
      return;
    }
    this.setData({ orderNo }, () => {
      this.loadOrderDetail();
    });
  },

  // ==============================================
  // ✅ 【最终正确】实时获取服务商位置
  // 后端返回：{ lng: 106.xxx, lat: 29.xxx }
  // 前端直接用，不需要任何解析！
  // ==============================================
  startTrackSupplier() {
    this.stopTrackSupplier();

    const timer = setInterval(async () => {
      try {
        //  request 自动返回 data 里的内容
        const res = await request({
          url: "/track/get",
          method: "GET",
          data: { orderNo: this.data.orderNo }
        });

        // ✅ 直接拿经纬度！不需要解析！
        const lat = res.lat;
        const lng = res.lng;

        if (lat && lng) {
          console.log("获取到位置：", lat, lng); // 你会看到真实经纬度
          this.setData({
            lat: lat,
            lng: lng,
            markers: [{
              id: 0, latitude: lat, longitude: lng,
              iconPath: "/images/supplier.png",
              width: 32, height: 32
            }]
          });
        }
      } catch (err) {
        console.error("获取位置失败：", err);
      }
    }, 3000);

    this.setData({ trackTimer: timer });
  },

  stopTrackSupplier() {
    if (this.data.trackTimer) {
      clearInterval(this.data.trackTimer);
      this.setData({ trackTimer: null });
    }
  },

  onUnload() {
    this.stopTrackSupplier();
  },

  getStatusText(status: string): string {
    const statusMap: Record<string, string> = {
      pending: "待救援",
      processing: "救援中",
      completed: "已完成",
      cancelled: "已取消"
    };
    return statusMap[status] || "未知状态";
  },

  formatTime(timeStr: string | Date): string {
    if (!timeStr) return "";
    const date = new Date(timeStr);
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, "0")}-${date.getDate().toString().padStart(2, "0")} ${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
  },

  async getOrderDetail(orderNo: string) {
    const userInfo = wx.getStorageSync("userInfo");
    if (!userInfo || !userInfo.id) throw new Error("请先登录");
    return request({
      url: `/owner/order/detail/${orderNo}?userId=${userInfo.id}`,
      method: "GET",
    });
  },

  async cancelOrder(orderNo: string) {
    const userInfo = wx.getStorageSync("userInfo");
    if (!userInfo || !userInfo.id) throw new Error("请先登录");
    return request({ url: `/owner/order/cancel?orderNo=${orderNo}&userId=${userInfo.id}`, method: "POST" });
  },

  async evaluateOrder(params: { orderNo: string; score: number; content: string }) {
    const userInfo = wx.getStorageSync("userInfo");
    if (!userInfo || !userInfo.id) throw new Error("请先登录");
    const { orderNo, score, content } = params;
    return request({
      url: `/owner/order/evaluate?orderNo=${orderNo}&userId=${userInfo.id}&score=${score}&content=${encodeURIComponent(content)}`,
      method: "POST",
    });
  },

  async loadOrderDetail() {
    this.setData({ loading: true });
    this.stopTrackSupplier();

    try {
      const res = await this.getOrderDetail(this.data.orderNo);
      const orderDetail = {
        ...res,
        statusText: this.getStatusText(res.status),
        createTimeText: this.formatTime(res.createTime),
        evaluateStatus: Number(res.evaluateStatus),
        faultTypeId: res.fault_type_id,
        createTime: res.create_time,
        updateTime: res.update_time,
        cancelTime: res.cancel_time,
        supplierId: res.supplier_id,
        userId: res.user_id,
        supplierName: res.supplierName,
        supplierPhone: res.supplierPhone,
        staffName: res.staffName,
        staffPhone: res.staffPhone,
      };

      this.setData({ orderDetail, loading: false });

      // 救援中 → 启动实时位置
      if (orderDetail.status === "processing") {
        this.startTrackSupplier();
      }
    } catch (err) {
      console.error("加载详情失败：", err);
      this.setData({ loading: false });
      wx.showToast({ title: "加载详情失败", icon: "none" });
      setTimeout(() => wx.navigateBack(), 1500);
    }
  },

  async handleCancelOrder() {
    try {
      const confirmRes = await wx.showModal({
        title: "确认取消",
        content: "是否确认取消该订单？取消后将无法恢复",
        confirmColor: "#f53f3f",
      });
      if (!confirmRes.confirm) return;

      await this.cancelOrder(this.data.orderNo);
      wx.showToast({ title: "取消成功", icon: "success" });
      const pages = getCurrentPages();
      const prevPage = pages[pages.length - 2];
      if (prevPage && prevPage.loadOrderList) prevPage.loadOrderList();
      wx.navigateBack({ delta: 1 });
    } catch (err) {
      console.error("取消订单失败：", err);
      wx.showToast({ title: (err as Error).message || "取消失败", icon: "none" });
    }
  },

  async handleExportOrder() {
    const orderNo = this.data.orderNo;
    const userInfo = wx.getStorageSync("userInfo");

    if (!userInfo || !userInfo.id) {
      wx.showToast({ title: "请先登录", icon: "none" });
      return;
    }

    wx.showLoading({ title: "导出中..." });

    try {
      const res = await request({
        url: `/owner/order/export/${orderNo}?userId=${userInfo.id}`,
        method: "GET",
        requireJsonResponse: false,
        responseType: "arraybuffer",
      });

      const fs = wx.getFileSystemManager();
      const filePath = `${wx.env.USER_DATA_PATH}/订单_${orderNo}.xlsx`;

      fs.writeFile({
        filePath: filePath,
        data: res,
        encoding: "binary",
        success: () => {
          wx.openDocument({
            filePath: filePath,
            fileType: "xlsx",
            success: () => {
              wx.hideLoading();
              wx.showToast({ title: "导出成功", icon: "success" });
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({ title: "打开文件失败", icon: "none" });
            },
          });
        },
        fail: () => {
          wx.hideLoading();
          wx.showToast({ title: "保存失败", icon: "none" });
        },
      });
    } catch (err) {
      wx.hideLoading();
      console.error("导出失败：", err);
      wx.showToast({ title: "导出失败", icon: "none" });
    }
  },

  openEvaluateModal() {
    this.setData({ showEvaluate: true, evaluateForm: { score: 5, content: "" } });
  },

  closeEvaluateModal() {
    this.setData({ showEvaluate: false });
  },

  handleScoreChange(e: WechatMiniprogram.BaseEvent) {
    const score = Number(e.currentTarget.dataset.score);
    this.setData({ "evaluateForm.score": score });
  },

  handleContentInput(e: any) {
    this.setData({ "evaluateForm.content": e.detail.value });
  },

  async submitEvaluate() {
    try {
      await this.evaluateOrder({
        orderNo: this.data.orderNo,
        score: this.data.evaluateForm.score,
        content: this.data.evaluateForm.content || "无",
      });
      wx.showToast({ title: "评价成功", icon: "success" });
      this.setData({ showEvaluate: false });
      this.loadOrderDetail();
    } catch (err) {
      console.error("提交评价失败：", err);
      wx.showToast({ title: "评价失败", icon: "none" });
    }
  },

  handleCallPhone() {
    const { staffPhone, supplierPhone } = this.data.orderDetail;
    const phone = staffPhone || supplierPhone;
    if (!phone) {
      wx.showToast({ title: "暂无联系方式", icon: "none" });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  },

  onShow() {
    wx.hideTabBar({});
  },
});