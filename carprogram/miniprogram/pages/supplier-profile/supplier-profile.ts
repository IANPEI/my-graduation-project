// pages/supplier-profile/supplier-profile.ts
import { request } from '../../utils/request';

// 服务商资料类型定义
interface SupplierInfo {
  id?: string;          // 服务商ID
  name: string;         // 服务商名称
  contact: string;      // 联系人
  phone: string;        // 联系电话
  address: string;      // 服务地址
  license: string;      // 营业执照编号
  scope: string;        // 救援范围
}

// 表单校验规则类型
interface FormRules {
  name: Array<{ required: boolean; message: string; trigger: string }>;
  contact: Array<{ required: boolean; message: string; trigger: string }>;
  phone: Array<{ required: boolean; message: string; trigger: string }>;
  address: Array<{ required: boolean; message: string; trigger: string }>;
}

// 页面自定义方法类型
interface PageCustomMethods {
  loadSupplierProfile(): Promise<void>;
  switchToEditMode(): void;
  cancelEdit(): void;
  onInputChange(e: WechatMiniprogram.Input): void;
  saveProfile(): Promise<void>;
}

Page<{
  supplierInfo: SupplierInfo;
  isEditMode: boolean;    // 是否编辑模式
  loading: boolean;       // 加载状态
  originInfo: SupplierInfo; // 原始数据（用于取消编辑）
}, PageCustomMethods>({
  data: {
    // 服务商资料默认值
    supplierInfo: {
      name: '',
      contact: '',
      phone: '',
      address: '',
      license: '',
      scope: ''
    },
    isEditMode: false,    // 默认查看模式
    loading: false,
    originInfo: {} as SupplierInfo // 原始数据备份
  },

  // 页面加载时获取服务商资料
  onLoad() {
    this.loadSupplierProfile();
  },

  // 加载服务商资料
  async loadSupplierProfile() {
    try {
      wx.showLoading({ title: '加载资料中...' });
      const res = await request({
        url: '/supplier/profile',
        method: 'GET'
      });
      // ✅ 修复：res 已经是后端的 data 对象，直接赋值
      this.setData({
        supplierInfo: res || {},
        originInfo: { ...res || {} }
      });
    } catch (err) {
      console.error('加载服务商资料失败:', err);
    } finally {
      wx.hideLoading({}); // ✅ 修复：传入空对象满足类型要求
    }
  },

  // 切换到编辑模式
  switchToEditMode() {
    this.setData({
      isEditMode: true
    });
    // 滚动到顶部，方便编辑
    wx.pageScrollTo({
      scrollTop: 0,
      duration: 300
    });
  },

  // 取消编辑（恢复原始数据）
  cancelEdit() {
    this.setData({
      isEditMode: false,
      supplierInfo: { ...this.data.originInfo } // 恢复原始数据
    });
  },

  // 输入框内容变化（通用处理）
  onInputChange(e: WechatMiniprogram.Input) { // ✅ 修复：使用正确的Input类型
    const { key } = e.currentTarget.dataset;
    if (!key) return;

    this.setData({
      [`supplierInfo.${key}`]: e.detail.value.trim()
    });
  },

  // 保存修改后的资料
  async saveProfile() {
    const { supplierInfo, loading } = this.data;
    if (loading) return;
  
    // 基础校验（省略）
    
    try {
      this.setData({ loading: true });
      await request({
        url: '/supplier/profile/update',
        method: 'POST',
        data: supplierInfo // 传递修改后的资料
      });
      wx.showToast({ title: '修改成功', icon: 'success' });
      this.setData({
        isEditMode: false,
        originInfo: { ...supplierInfo },
        loading: false
      });
      // 更新缓存的服务商名称
      wx.setStorageSync('supplierName', supplierInfo.name);
    } catch (err) {
      console.error('保存服务商资料失败:', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  // 返回上一页
  onUnload() {
    // 可在这里做一些清理工作
  }
});