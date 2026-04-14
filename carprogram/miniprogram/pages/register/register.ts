import { request } from '../../utils/request';

// 1. 页面数据类型
interface RegisterPageData {
  role: 'owner' | 'supplier';
  phone: string;
  nickname: string;
  password: string;
  supplierName: string;
  contact: string;
  city: string;
  address: string;
  loading: boolean;
}

// 2. 解决报错：直接使用官方标准事件类型 (不依赖低版本的 Tap)
type InputEvent = WechatMiniprogram.Input;
type TapEvent = WechatMiniprogram.CustomEvent;

// 3. 页面自定义方法类型
interface RegisterPageMethods {
  selectRole(e: TapEvent): void;
  onPhoneInput(e: InputEvent): void;
  onNicknameInput(e: InputEvent): void;
  onPwdInput(e: InputEvent): void;
  onSupplierNameInput(e: InputEvent): void;
  onContactInput(e: InputEvent): void;
  onCityInput(e: InputEvent): void;
  onAddressInput(e: InputEvent): void;
  doRegister(): Promise<void>;
}

// 🌟 核心修复：Page 泛型必须传入 2 个参数 (Data + Methods)
Page<RegisterPageData, RegisterPageMethods>({
  data: {
    role: 'owner',
    phone: '',
    nickname: '',
    password: '',
    supplierName: '',
    contact: '',
    city: '',
    address: '',
    loading: false
  },

  // 选择身份
  selectRole(e: TapEvent) {
    // 安全类型断言
    this.setData({
      role: e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.role as 'owner' | 'supplier'
    });
  },

  // 输入事件
  onPhoneInput(e: InputEvent) {
    this.setData({ phone: e.detail.value.trim() });
  },
  onNicknameInput(e: InputEvent) {
    this.setData({ nickname: e.detail.value.trim() });
  },
  onPwdInput(e: InputEvent) {
    this.setData({ password: e.detail.value.trim() });
  },
  onSupplierNameInput(e: InputEvent) {
    this.setData({ supplierName: e.detail.value.trim() });
  },
  onContactInput(e: InputEvent) {
    this.setData({ contact: e.detail.value.trim() });
  },
  onCityInput(e: InputEvent) {
    this.setData({ city: e.detail.value.trim() });
  },
  onAddressInput(e: InputEvent) {
    this.setData({ address: e.detail.value.trim() });
  },

  // 注册提交
  async doRegister() {
    const { role, phone, nickname, password, supplierName, contact, city, address, loading } = this.data;

    if (loading) return;

    // 非空校验
    if (!phone) { wx.showToast({ title: '请输入手机号', icon: 'none' }); return; }
    if (!nickname) { wx.showToast({ title: '请输入昵称', icon: 'none' }); return; }
    if (!password) { wx.showToast({ title: '请设置密码', icon: 'none' }); return; }

    if (role === 'supplier') {
      if (!supplierName) { wx.showToast({ title: '请输入服务商名称', icon: 'none' }); return; }
      if (!contact) { wx.showToast({ title: '请输入联系人', icon: 'none' }); return; }
      if (!city) { wx.showToast({ title: '请输入城市', icon: 'none' }); return; }
      if (!address) { wx.showToast({ title: '请输入详细地址', icon: 'none' }); return; }
    }

    this.setData({ loading: true });

    try {
      await request({
        url: '/user/register',
        method: 'POST',
        data: {
          account: phone,
          phone,
          nickname,
          password,
          identityType: role === 'owner' ? 3 : 2,
          supplierName: role === 'supplier' ? supplierName : '',
          contact,
          city,
          address
        }
      });

      wx.showToast({ title: '注册成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1500);

    } catch (err: any) {
      wx.showToast({ title: err.msg || '注册失败', icon: 'none' });
      console.error('注册异常：', err);
    } finally {
      this.setData({ loading: false });
    }
  }
});