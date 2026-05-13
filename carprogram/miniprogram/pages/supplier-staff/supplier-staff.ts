import { request } from '../../utils/request';

// 员工信息类型定义
interface StaffItem {
  id?: string;           // 员工ID
  name: string;          // 姓名
  phone: string;         // 电话
  skill: string;         // 技能
  status: 'online'|'busy'|'offline';        // 状态：online/offline
}

// 表单数据类型（移除status字段）
interface StaffForm {
  id?: string;
  name: string;
  phone: string;
  skill: string;
}

// 核心修复：定义所有自定义方法的类型接口
interface PageCustomMethods {
  loadStaffList(): Promise<void>;
  openStaffDialog(e?: WechatMiniprogram.BaseEvent<{ staff?: StaffItem }>): void;
  closeStaffDialog(): void;
  onFormInput(e: WechatMiniprogram.Input): void;
  changeStaffStatus(e: WechatMiniprogram.BaseEvent<{ id: string; status: string }>): Promise<void>;
  saveStaff(): Promise<void>;
  handleDeleteStaff(e: WechatMiniprogram.BaseEvent<{ id: string }>): Promise<void>;
}

// 修复：将PageCustomMethods传入第二个泛型参数
Page<{
  staffList: StaffItem[];
  dialogVisible: boolean; // 弹窗显隐
  dialogType: string;     // 弹窗类型：add/edit
  staffForm: StaffForm;   // 表单数据（无status）
  loading: boolean;       // 加载状态
}, PageCustomMethods>({
  data: {
    staffList: [],
    dialogVisible: false,
    dialogType: 'add',
    staffForm: {
      name: '',
      phone: '',
      skill: ''
    },
    loading: false
  },

  onLoad() {
    // 身份校验：仅服务商可访问
    const identity = wx.getStorageSync('identity');
    if (identity !== 'supplier') {
      wx.redirectTo({ url: '/pages/login/login' });
      return;
    }
    
    this.loadStaffList();
  },

  // 页面显示时刷新列表
  onShow() {
    wx.hideTabBar({});
  
    const identity = wx.getStorageSync('identity') || 'owner';
  
    const tabBar = this.getTabBar && this.getTabBar();
    if (tabBar) {
      // ✅ 只传 identity
      tabBar.setData({ identity });
    }
  },

  // 加载员工列表
  async loadStaffList() {
    try {
      wx.showLoading({ title: '加载人员中...' });
      // 获取当前登录服务商ID
      const supplierId = wx.getStorageSync('supplierId');
      const res = await request({
        url: '/supplier/staff/list',
        method: 'GET',
        data: {
          pageNum: 1,
          pageSize: 100, // 小程序端默认加载全部
          name: '',      // 姓名搜索，默认空
          supplierId     // 传递服务商ID（后端需要）
        }
      });
      // 解析分页数据
      this.setData({
        staffList: res.records || []
      });
    } catch (err) {
      console.error('加载员工列表失败:', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      wx.hideLoading({}); // 修复：传入空对象满足类型要求
    }
  },

  // 打开新增/编辑员工弹窗（移除status相关逻辑）
  openStaffDialog(e?) {
    const defaultForm = {
      id: '',
      name: '',
      phone: '',
      skill: ''
    };
  
    let dialogType = 'add';
    const staff = e && e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.staff;
  
    if (staff) {
      dialogType = 'edit';
      defaultForm.id = staff.id;
      defaultForm.name = staff.name;
      defaultForm.phone = staff.phone;
      defaultForm.skill = staff.skill || '';
    }
  
    this.setData({
      dialogVisible: true,
      dialogType,
      staffForm: defaultForm
    });
  },

  // 关闭员工弹窗（重置表单）
  closeStaffDialog() {
    this.setData({
      dialogVisible: false,
      staffForm: {
        id: '',
        name: '',
        phone: '',
        skill: ''
      }
    });
  },

  // 表单输入变化
  onFormInput(e) {
    const { key } = e.currentTarget.dataset;
    if (!key) return;
    this.setData({
      [`staffForm.${key}`]: e.detail.value.trim()
    });
  },

  // 新增：切换员工在线/离线状态
  async changeStaffStatus(e) {
    const { id, status } = e.currentTarget.dataset;
    if (!id) {
      wx.showToast({ title: '人员ID不能为空', icon: 'none' });
      return;
    }

    // 确认切换
    const targetStatus = status === 'online' ? 'offline' : 'online';
    const confirmRes = await wx.showModal({
      title: '确认切换状态',
      content: `是否将该员工设为${targetStatus === 'online' ? '在线' : '离线'}？`,
      confirmColor: '#409eff'
    });
    if (!confirmRes.confirm) return;

    try {
      // 调用状态切换接口
      await request({
        url: `/supplier/staff/status/${id}`, // 后端接口地址
        method: 'POST',
        data: { status: targetStatus } // 传递目标状态
      });
      wx.showToast({ 
        title: `已设为${targetStatus === 'online' ? '在线' : '离线'}`, 
        icon: 'success' 
      });
      // 刷新列表
      this.loadStaffList();
    } catch (err) {
      console.error('切换状态失败:', err);
      const errMsg = (err && err.response && err.response.data && err.response.data.msg) || '切换状态失败';
      wx.showToast({ title: errMsg, icon: 'none' });
    }
  },

  // 保存员工信息（仅修改姓名、电话、技能）
  async saveStaff() {
    const { dialogType, staffForm, loading } = this.data;
    
    // 基础校验
    if (!staffForm.name) {
      wx.showToast({ title: '请输入员工姓名', icon: 'none' });
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(staffForm.phone)) {
      wx.showToast({ title: '请输入正确的11位手机号', icon: 'none' });
      return;
    }
    if (!staffForm.skill) {
      wx.showToast({ title: '请输入擅长救援类型', icon: 'none' });
      return;
    }
    if (loading) return;

    try {
      this.setData({ loading: true });
      // 获取当前服务商ID
      const supplierId = wx.getStorageSync('supplierId');
      const requestData = { 
        ...staffForm, 
        supplierId,
        id: staffForm.id ? Number(staffForm.id) : 0 // ID转数字
      };

      if (dialogType === 'add') {
        // 新增员工
        await request({
          url: '/supplier/staff/add',
          method: 'POST',
          data: requestData
        });
        wx.showToast({ title: '添加成功', icon: 'success' });
      } else {
        // 编辑员工（仅修改姓名、电话、技能）
        await request({
          url: '/supplier/staff/edit',
          method: 'POST',
          data: requestData
        });
        wx.showToast({ title: '编辑成功', icon: 'success' });
      }
      this.closeStaffDialog(); // 关闭弹窗
      this.loadStaffList();    // 刷新列表
    } catch (err) {
      console.error('保存员工信息失败:', err);
      const errMsg = (err && err.response && err.response.data && err.response.data.msg) || '操作失败';
      wx.showToast({ title: errMsg, icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 删除员工
  async handleDeleteStaff(e) {
    const { id } = e.currentTarget.dataset;
    if (!id) return;

    try {
      const confirmRes = await wx.showModal({
        title: '确认删除',
        content: '是否确认删除该救援人员？',
        confirmColor: '#f53f3f'
      });
      if (!confirmRes.confirm) return;
  
      // 获取当前服务商ID（后端校验归属）
      const supplierId = wx.getStorageSync('supplierId');
      await request({
        url: `/supplier/staff/delete/${id}`,
        method: 'DELETE',
        data: { supplierId }
      });
      wx.showToast({ title: '删除成功', icon: 'success' });
      this.loadStaffList();
    } catch (err) {
      console.error('删除员工失败:', err);
      wx.showToast({ title: '删除失败', icon: 'none' });
    }
  }
});