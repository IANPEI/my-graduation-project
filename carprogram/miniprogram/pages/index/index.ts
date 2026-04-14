import { request } from '../../utils/request';

// 1. 全局App类型定义
interface AppGlobalData {
  token: string;
  identity: string;
}
interface CustomApp extends WechatMiniprogram.App.Instance<AppGlobalData> {
  globalData: AppGlobalData;
}

// 2. 订单类型定义（服务商页面用）
interface OrderItem {
  orderNo: string;
  faultTypeId: string;
  faultTypeName: string;
  username: string;
  phone: string;
  address: string;
  status: string;
  createTime: string;
}

// 3. 接口返回类型（服务商首页数据）
interface HomeSimpleRes {
  pendingCount: number;
  orderList: OrderItem[];
}

// 4. 页面数据类型（整合车主+服务商所有字段）
interface PageData {
  identity: 'owner' | 'supplier' | 'admin'; // 用户身份
  showHelpModal: boolean; // 救援指南弹窗（车主）
  isLogin: boolean; // 登录状态
  userInfo: any; // 用户信息
  loading: boolean; // 通用加载状态
  accepting: boolean; // 服务商接单加载状态
  homeData: Record<string, any>; // 服务商首页原始数据
  orderList: OrderItem[]; // 服务商待处理订单列表
  pendingCount: number; // 服务商待处理订单数
  refreshTimer: number | null; // 自动刷新定时器
}

// 5. 页面方法类型（整合所有方法）
interface PageCustomMethods {
  // 通用方法
  checkLoginStatus: () => boolean;
  logout: () => Promise<void>;
  clearLoginState: () => void;

  // 车主方法
  goToCallRescue: () => void;
  goToOrderList: () => void;
  goToMine: () => void;
  showHelpModal: () => void;
  hideHelpModal: () => void;
  noop: () => void;

  // 服务商方法
  loadHomeData: () => Promise<void>;
  handleAcceptOrder: (e: WechatMiniprogram.TouchEvent) => Promise<void>;
  goToStaffManage: () => void;
  goToOrderStat: () => void;
  goToProfile: () => void;

  // 新增：自动刷新
  startAutoRefresh: () => void;
  stopAutoRefresh: () => void;
}

// 6. 页面配置（整合所有逻辑）
Page<PageData, PageCustomMethods>({
  data: {
    identity: 'owner', // 默认车主身份
    showHelpModal: false,
    isLogin: false,
    userInfo: null,
    loading: false,
    accepting: false,
    homeData: {},
    orderList: [],
    pendingCount: 0,
    refreshTimer: null // 自动刷新定时器
  },

  onLoad() {
    // 校验登录状态
    this.checkLoginStatus();
    // 获取身份并初始化
    const identity = (wx.getStorageSync('identity') as 'owner' | 'supplier') || 'owner';
    this.setData({ identity });
    // 服务商身份加载首页数据
    if (identity === 'supplier') {
      this.loadHomeData();
      this.startAutoRefresh(); // 开启自动刷新
    }
  },

  onShow() {
    wx.hideTabBar({});
    const identity = wx.getStorageSync('identity') || 'owner';
  
    const tabBar = this.getTabBar && this.getTabBar();
    if (tabBar) {
      tabBar.setData({ identity });
      tabBar.refreshTabBar();
    }
  
    this.checkLoginStatus();
    this.setData({ identity });
  
    if (identity === 'supplier') {
      this.loadHomeData();
      this.startAutoRefresh(); // 页面显示时重启刷新
    }
  },

  onHide() {
    this.stopAutoRefresh(); // 页面隐藏时停止刷新
  },

  onUnload() {
    this.stopAutoRefresh(); // 页面销毁时停止刷新
  },

  /**
   * 新增：开启自动刷新（每8秒拉一次新订单）
   */
  startAutoRefresh() {
    const { identity, refreshTimer } = this.data;
    if (identity !== 'supplier') return;
    if (refreshTimer) return;

    const timer = setInterval(() => {
      this.loadHomeData();
    }, 8000); // 8秒刷新一次

    this.setData({ refreshTimer: timer });
    console.log("✅ 服务商首页自动刷新已开启");
  },

  /**
   * 新增：关闭自动刷新
   */
  stopAutoRefresh() {
    const { refreshTimer } = this.data;
    if (refreshTimer) {
      clearInterval(refreshTimer);
      this.setData({ refreshTimer: null });
      console.log("✅ 自动刷新已停止");
    }
  },

  /**
   * 通用方法：登录状态校验
   */
  checkLoginStatus(): boolean {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.redirectTo({ url: '/pages/login/login' });
      this.setData({ isLogin: false });
      return false;
    }
    this.setData({ isLogin: true });
    return true;
  },

  /**
   * 通用方法：清空登录状态
   */
  clearLoginState() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    wx.removeStorageSync('identity');
    wx.removeStorageSync('supplierName');
    const app = getApp<CustomApp>();
    app.globalData.token = '';
    app.globalData.identity = '';
    this.setData({ isLogin: false });
  },

  /**
   * 通用方法：退出登录
   */
  async logout() {
    try {
      const confirmRes = await wx.showModal({
        title: '确认退出',
        content: '是否确认退出登录？',
        confirmColor: '#f53f3f'
      });

      if (!confirmRes.confirm) return;

      this.clearLoginState();
      wx.showToast({ title: '已退出登录', icon: 'success' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/login/login' });
      }, 1000);
    } catch (err: any) {
      console.error('退出登录异常:', err);
      wx.showToast({ title: err.msg || '退出失败，请重试', icon: 'none' });
    }
  },

  /**
   * 车主方法：跳转到一键呼救
   */
  goToCallRescue() {
    if (!this.checkLoginStatus()) return;
    wx.switchTab({
      url: '/pages/call-rescue/call-rescue',
      fail: (err) => {
        console.warn('跳转到一键呼救页失败:', err);
        wx.showToast({ title: '页面跳转失败', icon: 'none' });
      }
    });
  },

  /**
   * 通用方法：跳转到订单列表（车主/服务商不同逻辑）
   */
  goToOrderList() {
    if (!this.checkLoginStatus()) return;
    const { identity } = this.data;
    if (identity === 'owner') {
      // 车主：跳转到车主订单列表（TabBar）
      wx.switchTab({
        url: '/pages/order-list/order-list',
        fail: (err) => {
          console.warn('跳转到我的订单页失败:', err);
          wx.showToast({ title: '页面跳转失败', icon: 'none' });
        }
      });
    } else {
      // 服务商：跳转到服务商订单列表（带待处理参数）
      wx.navigateTo({
        url: '/pages/supplier-order/supplier-order?type=pending',
        fail: (err) => {
          console.error('跳转订单列表失败:', err);
          wx.showToast({ title: '页面跳转失败', icon: 'none' });
        }
      });
    }
  },

  /**
   * 车主方法：跳转到个人中心
   */
  goToMine() {
    if (!this.checkLoginStatus()) return;
    wx.switchTab({
      url: '/pages/mine/mine',
      fail: (err) => {
        console.warn('跳转到个人中心页失败:', err);
        wx.showToast({ title: '页面跳转失败', icon: 'none' });
      }
    });
  },

  /**
   * 车主方法：显示救援指南弹窗
   */
  showHelpModal() {
    this.setData({ showHelpModal: true });
  },

  /**
   * 车主方法：隐藏救援指南弹窗
   */
  hideHelpModal() {
    this.setData({ showHelpModal: false });
  },

  /**
   * 车主方法：空方法（阻止事件冒泡）
   */
  noop() {},

  /**
   * 服务商方法：加载首页数据
   */
  async loadHomeData() {
    if (!this.checkLoginStatus()) return;
    try {
      this.setData({ loading: true });
      const res = await request<HomeSimpleRes>({
        url: '/supplier/home/simple',
        method: 'GET'
      });
      this.setData({
        homeData: res,
        pendingCount: res.pendingCount || 0,
        orderList: res.orderList || []
      });
    } catch (err: any) {
      console.error('加载服务商首页数据失败:', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 服务商方法：接单操作
   */
  async handleAcceptOrder(e: WechatMiniprogram.TouchEvent) {
    if (!this.checkLoginStatus()) return;
    const orderNo = e.currentTarget.dataset.orderNo as string;
    if (!orderNo) {
      wx.showToast({ title: '订单号异常', icon: 'none' });
      return;
    }
    if (this.data.accepting) return;

    try {
      this.setData({ accepting: true });
      await request({
        url: `/supplier/order/accept/${orderNo}`,
        method: 'POST'
      });
      wx.showToast({ title: '接单成功', icon: 'success' });
      this.loadHomeData();
    } catch (err: any) {
      console.error('接单失败:', err);
      wx.showToast({ title: err.msg || '接单失败，请重试', icon: 'none' });
    } finally {
      this.setData({ accepting: false });
    }
  },

  /**
   * 服务商方法：跳转到员工管理
   */
  goToStaffManage() {
    if (!this.checkLoginStatus()) return;
    wx.navigateTo({
      url: '/pages/supplier-staff/supplier-staff',
      fail: (err) => {
        console.error('跳转员工管理失败:', err);
        wx.showToast({ title: '页面跳转失败', icon: 'none' });
      }
    });
  },

  /**
   * 服务商方法：跳转到订单统计
   */
  goToOrderStat() {
    if (!this.checkLoginStatus()) return;
    wx.navigateTo({
      url: '/pages/supplier-order/supplier-order',
      fail: (err) => {
        console.error('跳转订单统计失败:', err);
        wx.reLaunch({
          url: '/pages/supplier-order/supplier-order',
          fail: () => {
            wx.showToast({ title: '页面跳转失败', icon: 'none' });
          }
        });
      }
    });
  },

  /**
   * 服务商方法：跳转到服务商资料
   */
  goToProfile() {
    if (!this.checkLoginStatus()) return;
    wx.navigateTo({
      url: '/pages/supplier-profile/supplier-profile',
      fail: (err) => {
        console.error('跳转资料修改失败:', err);
        wx.showToast({ title: '页面跳转失败', icon: 'none' });
      }
    });
  }
});