import { request } from '../../utils/request';

interface RegisterPageData {
  role: 'owner' | 'supplier';
  phone: string;
  nickname: string;
  password: string;
  supplierName: string;
  contact: string;
  city: string;
  address: string;
  // 👇 新增两个字段
  businessLicenseNo: string;
  rescueQualificationNo: string;
  loading: boolean;
}

type InputEvent = WechatMiniprogram.Input;
type TapEvent = WechatMiniprogram.CustomEvent;

interface RegisterPageMethods {
  selectRole(e: TapEvent): void;
  onPhoneInput(e: InputEvent): void;
  onNicknameInput(e: InputEvent): void;
  onPwdInput(e: InputEvent): void;
  onSupplierNameInput(e: InputEvent): void;
  onContactInput(e: InputEvent): void;
  onCityInput(e: InputEvent): void;
  onAddressInput(e: InputEvent): void;
  // 👇 新增输入方法
  onBusinessLicenseNoInput(e: InputEvent): void;
  onRescueQualificationNoInput(e: InputEvent): void;
  doRegister(): Promise<void>;
}

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
    // 👇 初始化
    businessLicenseNo: '',
    rescueQualificationNo: '',
    loading: false
  },

  selectRole(e: TapEvent) {
    this.setData({
      role: e.currentTarget.dataset.role as 'owner' | 'supplier'
    });
  },

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

  // ==============================================
  // ✅ 新增：营业执照编号输入
  // ==============================================
  onBusinessLicenseNoInput(e: InputEvent) {
    this.setData({ businessLicenseNo: e.detail.value.trim() });
  },

  // ==============================================
  // ✅ 新增：救援资质编号输入
  // ==============================================
  onRescueQualificationNoInput(e: InputEvent) {
    this.setData({ rescueQualificationNo: e.detail.value.trim() });
  },

  // ==============================================
  // ✅ 注册提交（完整修复）
  // ==============================================
  async doRegister() {
    const {
      role, phone, nickname, password,
      supplierName, contact, city, address,
      businessLicenseNo, rescueQualificationNo,
      loading
    } = this.data;

    if (loading) return;

    // 基础校验
    if (!phone) { wx.showToast({ title: '请输入手机号', icon: 'none' }); return; }
    if (!nickname) { wx.showToast({ title: '请输入昵称', icon: 'none' }); return; }
    if (!password) { wx.showToast({ title: '请设置密码', icon: 'none' }); return; }

    // 服务商额外校验
    if (role === 'supplier') {
      if (!supplierName) { wx.showToast({ title: '请输入服务商名称', icon: 'none' }); return; }
      if (!contact) { wx.showToast({ title: '请输入联系人', icon: 'none' }); return; }
      if (!city) { wx.showToast({ title: '请输入城市', icon: 'none' }); return; }
      if (!address) { wx.showToast({ title: '请输入详细地址', icon: 'none' }); return; }
      if (!businessLicenseNo) { wx.showToast({ title: '请输入营业执照编号', icon: 'none' }); return; }
      if (!rescueQualificationNo) { wx.showToast({ title: '请输入救援资质编号', icon: 'none' }); return; }
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
          address,
          // 👇 新增提交
          businessLicenseNo,
          rescueQualificationNo
        }
      });
      // ✅ 服务商：注册成功后弹出审核提示
      if (role === 'supplier') {
        wx.showModal({
          title: '注册成功',
          content: '您的账号已提交，需线下联系管理员审核资质，审核通过后才可登录使用',
          showCancel: false,
          confirmText: '我知道了',
          success: () => {
            wx.navigateBack();
          }
        });
      } else {
        wx.showToast({ title: '注册成功', icon: 'success' });
        setTimeout(() => wx.navigateBack(), 1500);
      }

    } catch (err: any) {
      wx.showToast({ title: err.msg || '注册失败', icon: 'none' });
      console.error('注册异常：', err);
    } finally {
      this.setData({ loading: false });
    }
  }
});