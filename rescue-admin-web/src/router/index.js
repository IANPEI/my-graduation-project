import { createRouter, createWebHistory } from "vue-router";

// 导入布局组件
const Layout = () => import("@/layout/Layout.vue");

// 登录页
const Login = () => import("@/views/Login.vue");

// 服务商后台页面
const SupplierHome = () => import("@/views/supplier/SupplierHome.vue");
const SupplierOrder = () => import("@/views/supplier/SupplierOrder.vue");
const SupplierStaff = () => import("@/views/supplier/SupplierStaff.vue");

// 管理员后台页面
const AdminHome = () => import("@/views/admin/AdminHome.vue");
const AdminSupplier = () => import("@/views/admin/AdminSupplier.vue");
const AdminSystem = () => import("@/views/admin/AdminSystem.vue");

const routes = [
  {
    path: "/",
    redirect: "/login",
  },
  {
    path: "/login",
    name: "Login",
    component: Login,
  },
  // 服务商后台路由（绑定 Layout 布局）
  {
    path: "/supplier",
    component: Layout, // 核心：添加布局组件
    redirect: "/supplier/home",
    children: [
      { path: "home", name: "SupplierHome", component: SupplierHome },
      { path: "order", name: "SupplierOrder", component: SupplierOrder },
      { path: "staff", name: "SupplierStaff", component: SupplierStaff },
    ],
  },
  // 管理员后台路由（绑定 Layout 布局）
  {
    path: "/admin",
    component: Layout, // 核心：添加布局组件
    redirect: "/admin/home",
    children: [
      { path: "home", name: "AdminHome", component: AdminHome },
      { path: "supplier", name: "AdminSupplier", component: AdminSupplier },
      { path: "system", name: "AdminSystem", component: AdminSystem },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由守卫（保留原有逻辑）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");
  const identity = localStorage.getItem("identity"); // supplier / admin

  if (to.path === "/login") {
    next();
    return;
  }

  if (!token) {
    next("/login");
    return;
  }

  if (to.path.startsWith("/admin")) {
    // 访问管理员后台 → 必须是 admin
    if (identity !== "admin") {
      next("/login");
      return;
    }
  }

  if (to.path.startsWith("/supplier")) {
    // 访问服务商后台 → 必须是 supplier
    if (identity !== "supplier") {
      next("/login");
      return;
    }
  }

  next();
});

export default router;
