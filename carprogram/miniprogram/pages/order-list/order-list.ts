import { request } from '../../utils/request';

// 1. 定义类型接口（核心：对齐后端返回结构）
// 订单详情DTO类型（补充后端实际返回的字段）
interface OrderDetailDTO {
  id?: number;
  orderNo: string;
  status: string; // pending/processing/completed/cancelled
  statusClass?: string; // 状态样式类
  faultTypeId?: number; // 故障类型ID
  faultTypeName: string; // 后端返回的故障类型名称（关键：替换原faultType）
  createTime: string; // 救援时间
  createTimeText?: string; // 格式化后的时间
  address: string; // 救援地址
  statusText?: string; // 中文状态文本
  username?: string;
  phone?: string;
  latitude?: number;
  longitude?: number;
  remark?: string;
  updateTime?: string;
  cancelTime?: string;
  supplierId?: string;
  userId?: number;
  isEvaluated?: number;
}

// 分页接口类型
interface IPage<T> {
  records: T[]; // 数据列表
  total: number; // 总条数
  size: number; // 每页条数
  current: number; // 当前页码
  pages?: number; // 总页数（可选）
}

// 分页参数类型
interface Pagination {
  currentPage: number;
  pageSize: number;
  total: number;
}

// 筛选条件类型
interface SearchForm {
  status: string; // 订单状态
  dateRange: string[]; // 时间范围 ["YYYY-MM-DD", "YYYY-MM-DD"]
}

// 页面数据类型
interface PageData {
  orderList: OrderDetailDTO[];
  pagination: Pagination;
  searchForm: SearchForm;
  loading: boolean;
  hasMore: boolean;
  activeStatus: string; // 当前选中的状态筛选
}

// 页面方法类型
interface PageMethods {
  loadOrderList: () => Promise<void>;
  handleStatusChange: (e: WechatMiniprogram.TouchEvent) => void;
  handleDateChange: (e: WechatMiniprogram.TouchEvent) => void;
  handleSearch: () => void;
  goToOrderDetail: (e: WechatMiniprogram.TouchEvent) => void;
  cancelOrder: (e: WechatMiniprogram.TouchEvent) => void;
  getStatusText: (status: string) => string; // 状态转换方法
  formatTime: (timeStr: string) => string; // 时间格式化方法
}

// 2. 页面配置
Page<PageData, PageMethods>({
  /**
   * 页面的初始数据
   */
  data: {
    // 订单列表数据
    orderList: [] as OrderDetailDTO[], // 显式指定类型，消除never[]报错
    // 分页参数（对齐后端pageNum/pageSize）
    pagination: {
      currentPage: 1,
      pageSize: 10,
      total: 0,
    },
    // 筛选条件
    searchForm: {
      status: "", 
      dateRange: [] as string[], // 显式指定类型
    },
    // 加载状态
    loading: false,
    hasMore: true,
    activeStatus: "all" // 默认选中全部状态
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 页面加载时校验登录状态（必须有userId）
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.id) {
      wx.showToast({ title: "请先登录", icon: "none" });
      wx.redirectTo({ url: "/pages/login/login" });
      return;
    }
    // 加载订单列表
    this.loadOrderList();
  },

  /**
   * 页面显示时更新tabBar（核心修复：适配自定义tabBar）
   */
  onShow() {
    wx.hideTabBar({});
  
    const identity = wx.getStorageSync('identity') || 'owner';
  
    const tabBar = this.getTabBar && this.getTabBar();
    if (tabBar) {
      // ✅ 只传 identity
      tabBar.setData({ identity });
    }
  },

  /**
   * 状态英文转中文
   */
  getStatusText(status: string): string {
    const statusMap: Record<string, string> = {
      'pending': '待救援',
      'processing': '救援中',
      'completed': '已完成',
      'cancelled': '已取消'
    };
    return statusMap[status] || status;
  },

  /**
   * 时间格式化（适配后端返回的时间格式）
   */
  formatTime(timeStr: string): string {
    if (!timeStr) return '';
    // 处理后端返回的时间格式（如2026-03-09T07:30:04.000+00:00）
    const date = new Date(timeStr);
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
  },

  /**
   * 加载订单列表（核心方法：对齐后端接口参数）
   */
  async loadOrderList() {
    // 防止重复加载
    if (this.data.loading) return;

    this.setData({ loading: true });

    try {
      // 1. 获取用户ID（从缓存读取，后端必传）
      const userInfo = wx.getStorageSync('userInfo');
      const userId = userInfo && userInfo.id;
      if (!userId) {
        throw new Error("用户ID不存在，请重新登录");
      }

      // 2. 构造请求参数（严格对齐后端接口）
      const { pagination, searchForm } = this.data;
      const params = {
        pageNum: pagination.currentPage,
        pageSize: pagination.pageSize,
        userId: userId, 
        status: searchForm.status === "all" ? "" : searchForm.status,
        startDate: searchForm.dateRange[0] || "",
        endDate: searchForm.dateRange[1] || "",
      };

      // 3. 调用后端接口（指定泛型，明确返回类型）
      const orderPage = await request<IPage<OrderDetailDTO>>({
        url: "/owner/order/list",
        method: "GET",
        data: params
      });

      // 4. 处理后端返回：补充中文状态、格式化时间、故障类型
      const processedList = orderPage.records.map(item => ({
        ...item,
        statusText: this.getStatusText(item.status), // 中文状态
        createTimeText: this.formatTime(item.createTime), // 格式化时间
        statusClass: `status-${item.status}` // 状态样式类（可选）
      }));

      // 5. 合并分页数据
      const newOrderList = pagination.currentPage === 1 
        ? processedList 
        : [...this.data.orderList, ...processedList];

      // 6. 更新页面数据
      this.setData({
        orderList: newOrderList,
        "pagination.total": orderPage.total,
        hasMore: newOrderList.length < orderPage.total,
      });
    } catch (err: any) {
      console.error("加载订单列表失败：", err);
      wx.showToast({
        title: err.msg || "加载订单失败",
        icon: "none",
        duration: 2000,
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 筛选条件变更（订单状态）
   */
  handleStatusChange(e: WechatMiniprogram.TouchEvent) {
    const status = e.currentTarget.dataset.status;
    this.setData({
      "searchForm.status": status,
      "pagination.currentPage": 1, 
      activeStatus: status 
    }, () => {
      this.loadOrderList();
    });
  },

  /**
   * 时间范围筛选（适配小程序日期选择器返回格式）
   */
  handleDateChange(e: WechatMiniprogram.TouchEvent) {
    const dateRange = e.detail.value;
    this.setData({
      "searchForm.dateRange": dateRange,
      "pagination.currentPage": 1, 
    }, () => {
      this.loadOrderList();
    });
  },

  /**
   * 点击查询按钮
   */
  handleSearch() {
    this.setData({
      "pagination.currentPage": 1,
    }, () => {
      this.loadOrderList();
    });
  },

  /**
   * 点击进入订单详情
   */
  goToOrderDetail(e: WechatMiniprogram.TouchEvent) {
    const { orderNo } = e.currentTarget.dataset; 
    wx.navigateTo({
      url: `/pages/order-detail/order-detail?orderNo=${orderNo}`,
    });
  },

  /**
   * 取消订单
   */
  async cancelOrder(e: WechatMiniprogram.TouchEvent) {
    
    const { orderNo } = e.currentTarget.dataset; 
    try {
      // 确认取消
      const modalRes = await wx.showModal({
        title: "确认取消",
        content: "是否确认取消该订单？",
        confirmColor: "#f53f3f",
      });

      // 用户取消操作
      if (!modalRes.confirm) return;

      // 调用取消订单接口（需补充实现）
      // const cancelRes = await request({
      //   url: "/owner/order/cancel",
      //   method: "POST",
      //   data: { orderNo: orderNo, userId: wx.getStorageSync('userInfo').id }
      // });

      wx.showToast({
        title: "取消成功",
        icon: "success",
      });

      // 刷新列表
      this.setData({
        "pagination.currentPage": 1,
      }, () => {
        this.loadOrderList();
      });
    } catch (err: any) {
      console.error("取消订单失败：", err);
      wx.showToast({
        title: err.msg || "取消失败",
        icon: "none",
      });
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.setData({
      "pagination.currentPage": 1,
    }, () => {
      this.loadOrderList().finally(() => {
        wx.stopPullDownRefresh();
      });
    });
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({
      "pagination.currentPage": this.data.pagination.currentPage + 1,
    }, () => {
      this.loadOrderList();
    });
  }
});