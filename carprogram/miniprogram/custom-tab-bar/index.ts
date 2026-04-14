// 1. 定义 TabBar 单个选项的类型
interface TabBarItem {
  pagePath: string;
  text: string;
  iconPath: string;
  selectedIconPath: string;
}

// 2. 定义组件数据类型
interface TabBarData {
  selected: string;
  identity: string;
  list: TabBarItem[];
  ownerList: TabBarItem[];
  supplierList: TabBarItem[];
}

// 3. 定义组件方法类型
interface TabBarMethods {
  switchTab: (e: WechatMiniprogram.TouchEvent) => void;
  refreshTabBar: () => void;
}

Component<TabBarData, TabBarMethods>({
  data: {
    selected: '',
    identity: '',
    list: [],

    // 车主 TabBar
    ownerList: [
      { pagePath: "pages/index/index", text: "首页", iconPath: "/images/tab/home.png", selectedIconPath: "/images/tab/home.png" },
      { pagePath: "pages/call-rescue/call-rescue", text: "一键呼救", iconPath: "/images/tab/rescue.png", selectedIconPath: "/images/tab/rescue.png" },
      { pagePath: "pages/order-list/order-list", text: "我的订单", iconPath: "/images/tab/order.png", selectedIconPath: "/images/tab/order.png" },
      { pagePath: "pages/mine/mine", text: "我的", iconPath: "/images/tab/mine.png", selectedIconPath: "/images/tab/mine.png" }
    ],

    // 服务商 TabBar
    supplierList: [
      { pagePath: "pages/index/index", text: "首页", iconPath: "/images/tab/home.png", selectedIconPath: "/images/tab/home.png" },
      { pagePath: "pages/supplier-order/supplier-order", text: "救援订单", iconPath: "/images/tab/order.png", selectedIconPath: "/images/tab/order.png" },
      { pagePath: "pages/supplier-staff/supplier-staff", text: "员工管理", iconPath: "/images/tab/management.png", selectedIconPath: "/images/tab/management.png" }
    ]
  },

  attached() {
    setTimeout(() => {
      wx.hideTabBar({});
    }, 30); 
    this.refreshTabBar();
  },

  methods: {
    refreshTabBar() {
      const identity = wx.getStorageSync('identity') as string || 'owner';
      setTimeout(() => {
        const pages = getCurrentPages();
        let path = "pages/index/index";
        if (pages.length > 0) {
          const currentPage = pages[pages.length - 1];
          path = currentPage.route as string;
        }

        const list = identity === 'supplier' ? this.data.supplierList : this.data.ownerList;
        this.setData({ identity, selected: path, list });
      }, 100);
    },

    switchTab(e: WechatMiniprogram.TouchEvent) {
      const path = e.currentTarget.dataset.path as string;
      if (!path) return;

      // 👇 只保留 app.json 中真正声明的 4 个页面
      const tabBarPaths = [
        "pages/index/index",
        "pages/call-rescue/call-rescue",
        "pages/order-list/order-list",
        "pages/mine/mine"
      ];

      // ✅ 真正的 TabBar 页面 → switchTab
      if (tabBarPaths.includes(path)) {
        wx.switchTab({ url: `/${path}` });
      }
      // ✅ 服务商页面 → navigateTo
      else {
        wx.navigateTo({ url: `/${path}` });
      }
    }
  }
});