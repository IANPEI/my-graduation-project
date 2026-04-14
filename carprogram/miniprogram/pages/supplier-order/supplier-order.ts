import { request } from '../../utils/request';

// 订单类型定义
interface OrderItem {
  orderNo: string;
  faultType: string;
  ownerName: string;
  ownerPhone: string;
  rescueAddress: string;
  createTime: string;
  acceptTime?: string;
  completeTime?: string;
  status: string;
  loading?: boolean;
}

// 筛选列表类型
interface FilterItem {
  key: string;
  name: string;
}

// 救援师傅类型
interface StaffItem {
  id: string;
  name: string;
  phone: string;
  skill: string;
}

// 页面自定义方法类型
interface PageCustomMethods {
  loadOrderList(): Promise<void>;
  switchOrderStatus(e: WechatMiniprogram.BaseEvent<{ status: string }>): void;
  handleAcceptOrder(e: WechatMiniprogram.BaseEvent<{ orderNo: string }>): Promise<void>;
  handleCompleteOrder(e: WechatMiniprogram.BaseEvent<{ orderNo: string }>): Promise<void>;
  handleCancelOrder(e: WechatMiniprogram.BaseEvent<{ orderNo: string }>): Promise<void>;
  getStatusText(status: string): string;
  getStatusColor(status: string): string;
  getActiveStatusText(): string;
  goBack(): void;
  startLocationUpload(orderNo: string): void;
  stopLocationUpload(): void;
  selectStaff(e: WechatMiniprogram.BaseEvent<{ id: string; name: string }>): void;
  closeStaffModal(): void;
  confirmAssignStaff(): Promise<void>;
  
}

Page<{
  orderList: OrderItem[];
  activeStatus: string;
  filterList: FilterItem[];
  loading: boolean;
  total?: number;
  locationTimer: number | null;
  showStaffModal: boolean;
  staffList: StaffItem[];
  selectedStaffId: string;
  currentOrderNo: string;
}, PageCustomMethods>({
  data: {
    orderList: [] as OrderItem[],
    activeStatus: 'all',
    filterList: [
      { key: 'all', name: '全部订单' },
      { key: 'pending', name: '待接单' },
      { key: 'processing', name: '已接单' },
      { key: 'completed', name: '已完成' },
      { key: 'cancelled', name: '已取消' }
    ] as FilterItem[],
    loading: false,
    total: undefined as number | undefined,
    locationTimer: null as number | null,
    showStaffModal: false,
    staffList: [] as StaffItem[],
    selectedStaffId: '',
    currentOrderNo: ''
  },

  onLoad(options) {
    console.log("页面加载了");
    if (options.type === 'pending') {
      this.setData({ activeStatus: 'pending' });
    }
    this.loadOrderList();
  },

  onShow() {
    wx.hideTabBar({});
    const identity = wx.getStorageSync('identity') || 'owner';
    const tabBar = this.getTabBar && this.getTabBar();
    if (tabBar) {
      tabBar.setData({ identity });
    }

    const { activeStatus, orderList } = this.data;
    if (activeStatus === 'processing') {
      const processingOrder = orderList.find(o => o.status === 'processing');
      if (processingOrder) {
        this.startLocationUpload(processingOrder.orderNo);
      }
    }
  },

  onUnload() {
    this.stopLocationUpload();
  },

  // 实时上传位置
  startLocationUpload(orderNo: string) {
    console.log("🚀 开始上传位置了！订单号：", orderNo);
    this.stopLocationUpload();

    const timer = setInterval(() => {
      wx.getLocation({
        type: 'gcj02',
        success: async (res) => {
          try {
            await request({
              url: `/track/upload?orderNo=${orderNo}&supplierLat=${res.latitude}&supplierLng=${res.longitude}`,
              method: "POST"
            });
          } catch (err) {
            console.log("位置上传失败", err);
          }
        },
        fail: (err) => {
          console.log("获取位置失败", err);
        }
      });
    }, 5000);

    this.setData({ locationTimer: timer });
  },

  stopLocationUpload() {
    if (this.data.locationTimer) {
      clearInterval(this.data.locationTimer);
      this.setData({ locationTimer: null });
    }
  },

  // 加载订单列表
  async loadOrderList() {
    const { activeStatus } = this.data;

    try {
      wx.showLoading({ title: '加载订单中...' });
      const res = await request({
        url: '/supplier/order/list',
        method: 'GET',
        data: {
          pageNum: 1,
          pageSize: 10,
          status: activeStatus === 'all' ? '' : activeStatus,
        }
      });
      this.setData({
        orderList: res.records || [],
        total: res.total
      });
    } catch (err) {
      console.error('加载订单列表失败:', err);
    } finally {
      wx.hideLoading({});
    }
  },

  // 切换状态
  switchOrderStatus(e) {
    const status = e.currentTarget.dataset.status;
    if (status === this.data.activeStatus) return;

    this.setData({ activeStatus: status }, () => {
      this.loadOrderList();
    });
  },

  // 点击接单 → 打开选择师傅弹窗
  async handleAcceptOrder(e) {
    const orderNo = e.currentTarget.dataset.orderNo;
    if (!orderNo) {
      wx.showToast({ title: '订单号异常', icon: 'none' });
      return;
    }

    try {
      // 加载师傅列表
      const res = await request({
        url: "/supplier/staff/list",
        method: "GET",
        data: { pageNum: 1, pageSize: 100, name: "" }
      });

      this.setData({
        currentOrderNo: orderNo,
        staffList: res.records || [],
        showStaffModal: true,
        selectedStaffId: ""
      });
    } catch (err) {
      wx.showToast({ title: '加载师傅列表失败', icon: 'none' });
    }
  },

  // 选择师傅
  selectStaff(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ selectedStaffId: id });
  },

  // 关闭弹窗
  closeStaffModal() {
    this.setData({ showStaffModal: false });
  },

  // 确认接单 + 分配师傅
  async confirmAssignStaff() {
    const { currentOrderNo, selectedStaffId } = this.data;

    if (!selectedStaffId) {
      wx.showToast({ title: "请选择接单师傅", icon: "none" });
      return;
    }

    // 加载中
    const orderList = this.data.orderList.map(item => {
      if (item.orderNo === currentOrderNo) item.loading = true;
      return item;
    });
    this.setData({ orderList });

    try {
      const res = await request({
        url: `/supplier/order/accept/${currentOrderNo}`,
        method: "POST",
        data: { staffId: selectedStaffId }
      });

      if (res.code === 200) {
        wx.showToast({ title: "接单成功", icon: "success" });
        this.closeStaffModal();
        this.startLocationUpload(currentOrderNo);
        this.loadOrderList();
      } else {
        wx.showToast({ title: res.msg || "接单失败", icon: "none" });
      }
    } catch (err: any) {
      console.error("接单失败", err);
      wx.showToast({ title: err.msg || "请求失败", icon: "none" });
    } finally {
      const orderList = this.data.orderList.map(item => {
        if (item.orderNo === currentOrderNo) item.loading = false;
        return item;
      });
      this.setData({ orderList });
    }
  },

  // 完成订单
  async handleCompleteOrder(e) {
    const { orderNo } = e.currentTarget.dataset;
    try {
      const confirmRes = await wx.showModal({
        title: '确认完成',
        content: '是否确认该订单已完成救援？',
        confirmColor: '#67c23a'
      });
      if (!confirmRes.confirm) return;

      await request({
        url: `/supplier/order/complete/${orderNo}`,
        method: 'POST'
      });
      wx.showToast({ title: '标记完成成功', icon: 'success' });
      this.loadOrderList();
    } catch (err) {
      console.error('标记订单完成失败:', err);
    }
  },

  // 供应商取消已接单订单
  async handleCancelOrder(e: WechatMiniprogram.BaseEvent<{ orderNo: string }>) {
      const orderNo = e.currentTarget.dataset.orderNo;

      const modalRes = await wx.showModal({
        title: '取消订单',
        content: '请先与车主沟通确认后再取消订单，确定要取消该订单吗？',
        confirmColor: '#F53F3F',
        cancelColor: '#999'
      });

      if (!modalRes.confirm) return;

      try {
        wx.showLoading({ title: '取消中...' });

        const res = await request({
          url: `/supplier/order/cancel/${orderNo}`,
          method: 'POST'
        });

        wx.showToast({ title: '取消成功', icon: 'success' });
        this.loadOrderList(); // 刷新订单列表

      } catch (err) {
        console.error('取消订单失败', err);
        wx.showToast({ title: '取消失败', icon: 'none' });
      } finally {
        wx.hideLoading();
      }
  },

  goBack() {
    wx.navigateBack({
      delta: 1,
      fail: () => {
        wx.switchTab({ url: '/pages/index/index' });
      }
    });
  },

  getStatusText(status: string) {
    const statusMap: Record<string, string> = {
      pending: '待接单',
      processing: '已接单',
      completed: '已完成',
      cancelled: '已取消'
    };
    return statusMap[status] || '未知状态';
  },

  getStatusColor(status: string) {
    const colorMap: Record<string, string> = {
      pending: '#f59e0b',
      processing: '#409eff',
      completed: '#67c23a',
      cancelled: '#999'
    };
    return colorMap[status] || '#333';
  },

  getActiveStatusText() {
    const item = this.data.filterList.find(item => item.key === this.data.activeStatus);
    return item ? item.name : '全部';
  }
});