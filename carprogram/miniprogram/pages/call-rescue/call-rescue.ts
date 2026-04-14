import { request } from '../../utils/request';

// 1. 定义故障类型数据结构
interface FaultType {
  id: number;
  name: string;
  sort?: number;
  status?: number;
  createTime?: string;
}

// 2. 定义 Page 数据类型
interface PageData {
  locationDesc: string;
  latitude: number;
  longitude: number;
  faultTypes: FaultType[];
  faultTypeIndex: number;
  faultTypeNames: string[];
  remark: string;
  submitting: boolean;
}

// 3. 声明 Page 方法类型
interface PageMethods {
  loadFaultTypes: () => Promise<void>;
  chooseLocation: () => void;
  onFaultTypeChange: (e: any) => void;
  onRemarkInput: (e: any) => void;
  callRescue: () => Promise<void>;
  validatePhone: (phone: string | null | undefined) => boolean;
}

// 4. Page 配置
Page<PageData, PageMethods>({
  data: {
    locationDesc: '',
    latitude: 0,
    longitude: 0,
    faultTypes: [] as FaultType[],
    faultTypeIndex: -1,
    faultTypeNames: [] as string[],
    remark: '',
    submitting: false
  },

  onShow() {
    wx.hideTabBar({});

    const identity = wx.getStorageSync('identity') || 'owner';

    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      const tabBar = this.getTabBar();
      tabBar.refreshTabBar();
    }
  },

  onLoad() {
    this.loadFaultTypes();
  },

  // 加载故障类型列表
  async loadFaultTypes() {
    try {
      console.log('开始请求故障类型接口');
      const res = await request({
        url: '/owner/order/fault/type/list',
        method: 'GET'
      });
      let list: FaultType[] = [];
      const resAny = res as Record<string, any>;

      if (resAny && resAny.code === 200 && Array.isArray(resAny.data)) {
        list = resAny.data as FaultType[];
      }
      else if (Array.isArray(resAny)) {
        list = resAny as FaultType[];
      } else if (resAny && resAny.data && resAny.data.data && Array.isArray(resAny.data.data)) {
        list = resAny.data.data as FaultType[];
      }

      if (list.length === 0) {
        list = [{ id: 1, name: '其他故障' }];
      }

      const validList = list.map(item => ({
        id: item.id || 0,
        name: item.name || '未知故障'
      })).filter(item => item.id > 0 && item.name.trim() !== '');

      this.setData({
        faultTypes: validList,
        faultTypeNames: validList.map(item => item.name),
        faultTypeIndex: -1
      }, () => {
        console.log('故障类型加载完成:', this.data.faultTypeNames);
      });
    } catch (err: any) {
      console.error('加载故障类型失败:', err.message || err);
      this.setData({
        faultTypes: [{ id: 1, name: '其他故障' }],
        faultTypeNames: ['其他故障'],
        faultTypeIndex: -1
      });
      wx.showToast({ title: '加载故障类型失败，使用默认类型', icon: 'none' });
    }
  },

  // 手动选择救援地址（唯一保留的位置方法）
  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        this.setData({
          locationDesc: res.name + res.address,
          latitude: Number(res.latitude),
          longitude: parseFloat(res.longitude)
        });
        wx.showToast({ title: '地址选择成功', icon: 'success' });
      },
      fail: (err) => {
        if (err.errMsg !== 'chooseLocation:fail cancel') {
          wx.showToast({ title: '地址选择失败，请允许位置权限', icon: 'none' });
        }
      }
    });
  },

  // 故障类型选择变更
  onFaultTypeChange(e: any) {
    const index = parseInt(e.detail.value);
    this.setData({ faultTypeIndex: index });
    console.log('选择的故障类型:', this.data.faultTypes[index]);
  },

  // 备注输入
  onRemarkInput(e: any) {
    this.setData({ remark: (e.detail.value || '').trim() });
  },

  // 手机号校验
  validatePhone(phone: string | null | undefined): boolean {
    if (!phone) return false;
    const phoneStr = String(phone).trim();
    const reg = /^1[3-9]\d{9}$/;
    return reg.test(phoneStr);
  },

  // 提交救援订单
  async callRescue() {
    console.log('===== 开始执行callRescue =====');
    const { locationDesc, faultTypeIndex, faultTypes, submitting } = this.data;

    if (submitting) {
      console.log('重复点击，直接返回');
      return;
    }

    if (!locationDesc) {
      console.log('校验失败：地址为空');
      wx.showToast({ title: '请选择救援地址', icon: 'none' });
      return;
    }
    if (faultTypeIndex === -1 || faultTypes.length === 0) {
      console.log('校验失败：故障类型未选择');
      wx.showToast({ title: '请选择故障类型', icon: 'none' });
      return;
    }

    const userInfo = wx.getStorageSync('userInfo') || {};
    console.log('当前缓存 userInfo:', userInfo);

    const phone = userInfo.phone || userInfo.mobile || userInfo.telephone;

    if (!userInfo.id) {
      console.log('校验失败：用户ID不存在，跳转登录页');
      wx.showToast({ title: '请先登录', icon: 'none' });
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }

    if (!this.validatePhone(phone)) {
      console.log('校验失败：手机号无效，phone=', phone);
      wx.showToast({ title: '请先完善手机号信息', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    try {
      const selectedFault = faultTypes[faultTypeIndex];
      const submitData = {
        userId: userInfo.id,
        faultTypeId: selectedFault.id,
        address: locationDesc,
        latitude: this.data.latitude,
        longitude: this.data.longitude,
        remark: this.data.remark,
        username: userInfo.nickname || userInfo.name || '未知用户',
        phone: String(phone).trim()
      };

      console.log('开始提交订单，参数：', submitData);

      const res = await request({
        url: '/rescue/create',
        method: 'POST',
        data: submitData,
        header: {
          'token': wx.getStorageSync('token') || '',
          'Content-Type': 'application/json'
        },
        ignoreCodeCheck: true,
        showErrorToast: false
      });

      console.log('后端返回结果完整:', res);

      const orderNo = (res && res.orderNo)
        || (res && res.data && res.data.orderNo)
        || (res && res.result && res.result.orderNo)
        || '';
      console.log('解析出的 orderNo:', orderNo);

      if (orderNo) {
        wx.showToast({ title: '救援订单提交成功', icon: 'success' });
        setTimeout(() => {
          wx.redirectTo({
            url: `/pages/order-detail/order-detail?orderNo=${orderNo}`
          });
        }, 1500);
      } else {
        throw new Error('订单创建失败，未返回订单号');
      }

    } catch (err: any) {
      console.error('提交订单异常：', err);
      const orderNo = (err && err.orderNo) || (err && err.data && err.data.orderNo) || '';
      if (orderNo) {
        wx.showToast({ title: '救援订单提交成功', icon: 'success' });
        setTimeout(() => {
          wx.redirectTo({
            url: `/pages/order-detail/order-detail?orderNo=${orderNo}`
          });
        }, 1500);
      } else {
        wx.showToast({
          title: err.msg || err.message || '提交失败，请稍后重试',
          icon: 'none',
          duration: 2000
        });
      }
    } finally {
      this.setData({ submitting: false });
      console.log('===== callRescue执行结束 =====');
    }
  }
});