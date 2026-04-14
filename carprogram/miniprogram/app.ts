interface IAppOption {
  globalData: {
    token: string;
    identity: string;
    redirectUrl: string; // 登录后回跳地址
  };
  // 核心修复：声明所有自定义方法
  initLoginStatus(options: WechatMiniprogram.App.LaunchShowOption): void;
  checkLoginStatus(): void;
  getHomeUrlByIdentity(identity: string): string;
  // 新增：通用页面跳转方法（处理TabBar/非TabBar）
  navigateToPage(url: string): void;
}

App<IAppOption>({
  globalData: {
    token: "",
    identity: "",
    redirectUrl: ""
  },

  onLaunch(options) {
    // 原有日志存储逻辑保留
    const logs = wx.getStorageSync('logs') || [];
    logs.unshift(Date.now());
    wx.setStorageSync('logs', logs);

    // 原有wx.login逻辑保留（用于小程序自身登录，和业务登录区分）
    wx.login({
      success: res => {
        console.log(res.code);
        // 发送 res.code 到后台换取 openId, sessionKey, unionId
      },
    });

    // ========== 新增：全局登录状态初始化（仅初始化数据，不校验） ==========
    this.initLoginStatus(options);
    
    // 修复：延迟执行登录校验（等待页面栈初始化）
    setTimeout(() => {
      this.checkLoginStatus();
    }, 100);
  },

  onShow(options) {
    // 小程序切前台时重新校验登录状态
    this.checkLoginStatus();
  },

  /**
   * 初始化登录状态（启动时执行）
   * @param options 启动参数
   */
  initLoginStatus(options: WechatMiniprogram.App.LaunchShowOption) {
    // 1. 读取本地缓存的登录态
    this.globalData.token = wx.getStorageSync('token') || "";
    this.globalData.identity = wx.getStorageSync('identity') || "";

    // 2. 记录启动时的目标页面（用于登录后回跳）
    if (options.path && options.path !== "pages/login/login") {
      // 修复：路径拼接错误 + 处理带参数的场景
      let redirectUrl = `/${options.path}`;
      // 如果有参数，拼接参数
      if (options.query && Object.keys(options.query).length > 0) {
        const queryStr = Object.entries(options.query)
          .map(([key, val]) => `${key}=${val}`)
          .join('&');
        redirectUrl += `?${queryStr}`;
      }
      this.globalData.redirectUrl = redirectUrl;
    }
  },

  /**
   * 登录状态校验核心方法
   */
  checkLoginStatus() {
    const { token, redirectUrl } = this.globalData;
    // 获取当前页面栈（增加空值保护）
    const pages = getCurrentPages();
    if (pages.length === 0) return; // 页面栈未初始化，不处理
    
    const currentPage = pages[pages.length - 1].route;
    console.log('当前页面:', currentPage, '登录态:', !!token);

    // 未登录 + 不在登录页 → 强制跳登录页
    if (!token && currentPage !== "pages/login/login") {
      this.globalData.redirectUrl = `/${currentPage}`; // 记录当前页面
      // 修复：统一用redirectTo跳登录页（登录页不是TabBar）
      wx.redirectTo({
        url: "/pages/login/login",
        fail: (err) => {
          console.warn("跳登录页失败:", err);
          // 极端情况降级（如当前页是TabBar，redirectTo失败）
          wx.switchTab({ 
            url: "/pages/index/index", // 先跳首页，首页再拦截
            fail: () => {}
          });
        }
      });
      return;
    }

    // 已登录 + 在登录页 → 跳回原页面/对应身份首页
    if (token && currentPage === "pages/login/login") {
      const targetUrl = redirectUrl || this.getHomeUrlByIdentity(this.globalData.identity);
      // 修复：使用通用跳转方法，自动区分TabBar/非TabBar
      this.navigateToPage(targetUrl);
    }
  },

  /**
   * 根据身份获取默认首页
   * @param identity 身份标识
   */
  getHomeUrlByIdentity(identity: string): string {
    // 修复：处理identity为空的情况
    if (!identity) return "/pages/index/index";
    
    switch (identity) {
      case 'supplier':
      case 'owner':
      default: 
        return "/pages/index/index";
    }
  },

  /**
   * 通用页面跳转方法（自动区分TabBar/非TabBar）
   * @param url 目标页面路径
   */
  navigateToPage(url: string) {
    if (!url) return;
    
    // 定义TabBar页面列表（需和app.json中的tabBar配置一致）
    const tabBarPages = [
      'pages/index/index',
      'pages/call-rescue/call-rescue',
      'pages/order-list/order-list',
      'pages/mine/mine',
      'pages/supplier-order/supplier-order'
    ];

    // 提取纯路径（去掉参数）
    const pureUrl = url.split('?')[0];

    // 区分跳转方式
    if (tabBarPages.includes(pureUrl)) {
      wx.switchTab({
        url: pureUrl, // TabBar页面不能带参数
        fail: (err) => {
          console.warn("switchTab失败:", err);
        }
      });
    } else {
      wx.redirectTo({
        url: url,
        fail: (err) => {
          console.warn("redirectTo失败，尝试navigateTo:", err);
          wx.navigateTo({
            url: url,
            fail: () => {
              // 最终兜底到首页
              wx.switchTab({ url: "/pages/index/index" });
            }
          });
        }
      });
    }
  }
});