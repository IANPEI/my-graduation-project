// 个人中心页数据类型定义
interface PageData {
  isLogin: boolean;                // 是否登录
  userInfo: {                      // 用户信息（从本地缓存/后端获取）
    nickName: string;
    avatarUrl: string;
    phone: string;
    id: number;                    // 新增：用户ID
  } | null;
  loading: boolean;                // 加载状态
  showHelpModal: boolean;          // 补充：添加缺失的字段声明
}

// 页面自定义方法类型定义（核心修复：包含所有自定义方法）
interface PageCustomMethods {
  checkLoginStatus(): Promise<void>; // 注意：方法是 async，返回值为 Promise<void>
  goToLogin(): void;
  goToOrderList(): void;
  goToFeedback(): void;
  goToAbout(): void;
  logout(): Promise<void>;          // async 方法，返回 Promise<void>
  clearLoginState(): void;
}

Page<PageData, PageCustomMethods>({
  data: {
    isLogin: false,
    userInfo: null,
    loading: false,
    showHelpModal: false           // 补充：初始化缺失的字段
  },

  /**
   * 页面加载/显示时校验登录态 + 加载用户信息
   */
  onLoad() {
    this.checkLoginStatus();
  },

  onShow() {
    wx.hideTabBar({});
  
    const identity = wx.getStorageSync('identity') || 'owner';
  
    const tabBar = this.getTabBar && this.getTabBar();
    if (tabBar) {
      // ✅ 只传 identity
      tabBar.setData({ identity });
    }
  
    this.checkLoginStatus();
  },

  /**
   * 核心：校验登录态 + 加载用户信息（优化身份校验逻辑）
   */
  async checkLoginStatus() {
    // 1. 从本地缓存获取token和用户信息
    const token = wx.getStorageSync('token');
    const localUserInfo = wx.getStorageSync('userInfo');
    const identity = wx.getStorageSync('identity');

    // 2. 校验登录态（放宽身份限制：兼容车主/服务商/管理员，个人中心通用）
    if (!token || !identity) {
      this.setData({
        isLogin: false,
        userInfo: null
      });
      return;
    }

    // 3. 标记加载状态
    this.setData({ loading: true });

    try {
      // 4. 优先用本地缓存，再调用后端接口刷新用户信息（可选）
      if (localUserInfo) {
        // 区分身份显示昵称（车主/服务商/管理员）
        let nickName = '';
        if (identity === 'owner') {
          nickName = localUserInfo.nickname || `车主${localUserInfo.phone ? localUserInfo.phone.slice(-4) : ''}`;
        } else if (identity === 'supplier') {
          nickName = wx.getStorageSync('supplierName') || `服务商${localUserInfo.phone ? localUserInfo.phone.slice(-4) : ''}`;
        } else {
          nickName = localUserInfo.nickname || '管理员';
        }

        // 本地有缓存，直接渲染
        this.setData({
          isLogin: true,
          userInfo: {
            nickName: nickName,
            avatarUrl: localUserInfo.avatar || '/images/default-avatar.png', // 头像兜底
            phone: localUserInfo.phone || '',
            id: localUserInfo.id || 0
          }
        });
      } else {
        // 本地无缓存，调用后端获取用户信息
        // const res = await request({
        //   url: '/user/info',
        //   method: 'GET'
        // });
        // this.setData({
        //   isLogin: true,
        //   userInfo: {
        //     nickName: res.nickname || `车主${res.phone?.slice(-4)}`,
        //     avatarUrl: res.avatar || '/images/default-avatar.png',
        //     phone: res.phone || '',
        //     id: res.id || 0
        //   }
        // });
        // // 缓存用户信息
        // wx.setStorageSync('userInfo', res);
      }
    } catch (err: any) {
      console.error('加载用户信息失败:', err);
      wx.showToast({ title: '加载个人信息失败', icon: 'none' });
      // 加载失败，清空登录态
      this.clearLoginState();
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 跳转到登录页
   */
  goToLogin() {
    wx.redirectTo({
      url: '/pages/login/login',
      fail: () => {
        wx.navigateTo({ url: '/pages/login/login' });
      }
    });
  },

  /**
   * 1. 跳转到我的订单列表页
   */
  goToOrderList() {
    // 未登录时先跳登录
    if (!this.data.isLogin) {
      this.goToLogin();
      return;
    }

    // 订单列表页是 tabBar 页面，必须用 switchTab
    wx.switchTab({
      url: '/pages/order-list/order-list',
      fail: (err) => {
        console.error('跳转订单列表失败:', err);
        wx.showToast({ title: '跳转失败，请重试', icon: 'none' });
      }
    });
  },

  /**
   * 3. 意见反馈：弹出邮箱反馈的弹窗
   */
  goToFeedback() {
    // 未登录也可以看反馈方式
    wx.showModal({
      title: '意见反馈',
      content: '您可以通过以下邮箱向我们反馈问题或建议：\nfeedback@jiuyuan.com',
      showCancel: false, // 只显示确定按钮
      confirmText: '知道了',
      confirmColor: '#1989fa',
      success: (res) => {
        if (res.confirm) {
          // 可选：点击确定后复制邮箱到剪贴板
          wx.setClipboardData({
            data: 'feedback@jiuyuan.com',
            success: () => {
              wx.showToast({ title: '邮箱已复制', icon: 'success' });
            }
          });
        }
      }
    });
  },

  /**
   * 4. 跳转到关于我们介绍页
   */
  goToAbout() {
    wx.navigateTo({ 
      url: '/pages/about/about',
      fail: (err) => {
        console.error('跳转关于我们失败:', err);
        wx.showToast({ title: '跳转失败，请重试', icon: 'none' });
      }
    });
  },

  /**
   * 退出登录（清空登录态 + 跳转登录页）
   */
  async logout() {
    try {
      // 1. 确认退出
      const confirmRes = await wx.showModal({
        title: '确认退出',
        content: '是否确认退出登录？',
        confirmColor: '#f53f3f'
      });

      if (!confirmRes.confirm) return;

      // 2. 清空登录态（核心）
      this.clearLoginState();

      // 3. 提示 + 跳转登录页
      wx.showToast({ title: '已退出登录', icon: 'success' });
      setTimeout(() => {
        this.goToLogin();
      }, 1000);

    } catch (err) {
      console.error('退出登录异常:', err);
      wx.showToast({ title: '退出失败，请重试', icon: 'none' });
    }
  },

  /**
   * 清空登录态（封装复用）
   */
  clearLoginState() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    wx.removeStorageSync('identity');
    this.setData({
      isLogin: false,
      userInfo: null
    });
  }
});