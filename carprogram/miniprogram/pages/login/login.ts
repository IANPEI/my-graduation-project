import { request } from '../../utils/request';

// 1. 页面数据类型定义
interface PageData {
  account: string;       
  password: string;      
  loading: boolean;  
}

interface AppGlobalData {
  token: string;
  identity: string;
  redirectUrl: string;
}

// 2. 全局App实例类型定义
interface CustomApp extends WechatMiniprogram.App.Instance<{
  globalData: AppGlobalData;
  token: string;
  identity: string;
  redirectUrl: string;
}> {
  getHomeUrlByIdentity: (identity: string) => string;
}

// 3. 页面自定义方法类型定义（核心修复：包含所有自定义方法）
interface PageCustomMethods {
  onAccountInput(e: WechatMiniprogram.Input): void; // 修正：使用正确的Input事件类型
  onPwdInput(e: WechatMiniprogram.Input): void;     // 修正：使用正确的Input事件类型
  goRegister(): void;
  login(): Promise<void>;
  jumpByIdentity(identity: string, isLoginSuccess?: boolean): void;
}

// 获取App实例并指定类型
const app = getApp<CustomApp>();

// 核心修复：把PageCustomMethods传给Page的第二个泛型参数
Page<PageData, PageCustomMethods>({
  data: {
    account: '',
    password: '',
    loading: false
  },

  onLoad() {
    // 页面加载时：如果已登录则直接跳首页
    const token = wx.getStorageSync('token');
    if (token) {
      const identity = wx.getStorageSync('identity');
      this.jumpByIdentity(identity);
    }
  },
  
  // 账号输入事件（修复：事件类型改为WechatMiniprogram.Input）
  onAccountInput(e: WechatMiniprogram.Input) {
    this.setData({
      account: e.detail.value.trim()
    });
  },

  // 密码输入事件（修复：事件类型改为WechatMiniprogram.Input）
  onPwdInput(e: WechatMiniprogram.Input) {
    this.setData({
      password: e.detail.value.trim()
    });
  },

  // 跳转注册页
  goRegister() {
    wx.navigateTo({
      url: '/pages/register/register'
    });
  },

  // 核心登录逻辑：账号+密码登录 + 真实接口调用 + 登录态存储 + 分角色跳转
  async login() {
    const { account, password, loading } = this.data;

    // 1. 基础校验
    if (loading) return; // 防止重复点击
    if (!account) {
      wx.showToast({ title: '请输入登录账号', icon: 'none' });
      return;
    }
    if (!password) {
      wx.showToast({ title: '请输入登录密码', icon: 'none' });
      return;
    }

    // 2. 标记加载状态
    this.setData({ loading: true });

    try {
      // 3. 调用真实登录接口（所有角色统一接口）
      const res = await request({
        url: '/login',
        method: 'POST',
        data: {
          account,
          password
        }
      });

      // 4. 解析后端返回的身份信息 + 存储登录态（核心）
      const { token, userInfo, identityType } = res;
      // identityType：1=管理员 2=服务商 3=车主（和t_user表一致）
      const identity = identityType === 1 ? 'admin' : (identityType === 2 ? 'supplier' : 'owner');
      
      // 存储到本地缓存，供后续页面使用
      wx.setStorageSync('token', token);         // 接口鉴权token
      wx.setStorageSync('userInfo', userInfo);   // 用户基本信息（昵称/手机号等）
      wx.setStorageSync('identity', identity);   // 身份标识
      // 新增：存储服务商名称（如果是服务商）
      if (identity === 'supplier' && res.supplierName) {
        wx.setStorageSync('supplierName', res.supplierName);
      }

      // 更新全局数据
      app.globalData.token = token;
      app.globalData.identity = identity;

      // 5. 根据身份跳转对应页面（优先回跳原页面）
      this.jumpByIdentity(identity, true);

    } catch (err: any) {
      // 错误提示（优先用后端返回的msg，兜底用通用提示）
      wx.showToast({ title: err.msg || '登录失败，请检查账号密码', icon: 'none' });
      console.error('登录异常:', err);
    } finally {
      // 6. 取消加载状态
      this.setData({ loading: false });
    }
  },

  

  /**
   * 分角色跳转方法（修复供应商跳转 + 完善tabBar判定）
   * @param identity 身份标识
   * @param isLoginSuccess 是否是登录成功跳转（区分回跳逻辑）
   */
  jumpByIdentity(identity: string, isLoginSuccess = false) {
    let redirectUrl = '';
    
    if (isLoginSuccess && app.globalData.redirectUrl) {
      redirectUrl = app.globalData.redirectUrl;
      app.globalData.redirectUrl = '';
    } else {
      switch (identity) {
        case 'owner':
        case 'supplier':
          // ✅ 所有人统一进 index
          redirectUrl = '/pages/index/index';
          break;
        default:
          wx.showToast({ title: '身份识别失败', icon: 'none' });
          return;
      }
    }
  
    // ✅ tabBar 页面
    const tabBarPages = [
      '/pages/index/index',
      '/pages/call-rescue/call-rescue',
      '/pages/order-list/order-list',
      '/pages/mine/mine'
    ];
  
    const pureUrl = redirectUrl.split('?')[0];
  
    if (tabBarPages.includes(pureUrl)) {
      wx.switchTab({ url: pureUrl });
      console.log('跳转方式:', tabBarPages.includes(pureUrl) ? 'switchTab' : 'redirectTo');
      console.log('最终路径:', pureUrl);
    } else {
      wx.redirectTo({ url: pureUrl });
    }
  
    if (isLoginSuccess) {
      wx.showToast({ title: '登录成功', icon: 'success' });
    }
  }
});