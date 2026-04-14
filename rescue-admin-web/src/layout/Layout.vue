<template>
  <div class="layout-container">
    <!-- 侧边导航栏 -->
    <div class="sidebar">
      <!-- 左侧顶部 LOGO 区域 -->
      <div class="sidebar-logo">
        <img src="@/assets/logo.png" alt="LOGO" />
        <span class="logo-text">救援管理平台</span>
      </div>

      <el-menu
        default-active="1"
        class="el-menu-vertical-demo"
        background-color="transparent"
        text-color="#fff"
        active-text-color="#ffd04b"
        router
      >
        <template v-if="identity === 'supplier'">
          <el-menu-item index="/supplier/home">
            <el-icon><House /></el-icon>
            <template #title>服务商工作台</template>
          </el-menu-item>
          <el-menu-item index="/supplier/order">
            <el-icon><ShoppingCart /></el-icon>
            <template #title>订单管理</template>
          </el-menu-item>
          <el-menu-item index="/supplier/staff">
            <el-icon><User /></el-icon>
            <template #title>救援人员</template>
          </el-menu-item>
        </template>

        <template v-if="identity === 'admin'">
          <el-menu-item index="/admin/home">
            <el-icon><House /></el-icon>
            <template #title>管理员工作台</template>
          </el-menu-item>
          <el-menu-item index="/admin/supplier">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/system">
            <el-icon><Setting /></el-icon>
            <template #title>系统设置</template>
          </el-menu-item>
        </template>

        <el-menu-item @click="logout">
          <el-icon><SwitchButton /></el-icon>
          <template #title>退出登录</template>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 顶部导航：左边小图片 + 系统名 + 右边用户信息 -->
      <div class="navbar">
        <div class="navbar-left">
          <!-- 顶部欢迎小图标 -->
          <img src="@/assets/welcome.png" alt="欢迎" class="welcome-img" />
          <span class="navbar-title">救援管理系统</span>
        </div>
        <div class="user-info">
          <template v-if="identity === 'supplier'">
            欢迎，{{ supplierName || "未知" }}服务商
          </template>
          <template v-else-if="identity === 'admin'"> 欢迎，管理员 </template>
        </div>
      </div>

      <div class="page-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
  House,
  ShoppingCart,
  User,
  OfficeBuilding,
  Setting,
  SwitchButton,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";

const router = useRouter();
const identity = ref(localStorage.getItem("identity") || "");
const supplierName = ref(localStorage.getItem("supplierName") || "");

onMounted(() => {
  identity.value = localStorage.getItem("identity") || "";
  supplierName.value = localStorage.getItem("supplierName") || "未知";
});

const logout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("identity");
  localStorage.removeItem("supplierId");
  localStorage.removeItem("supplierName");
  ElMessage.success("退出登录成功");
  router.push("/login");
};
</script>

<style scoped>
/* 整体布局 */
.layout-container {
  display: flex;
  width: 100%;
  height: 100vh;
}

/* 🔴 侧边栏：淡蓝色渐变（你要的风格） */
.sidebar {
  width: 200px;
  height: 100%;
  background: linear-gradient(180deg, #2c5282 0%, #4a83b9 100%);
}

/* 🔴 左侧LOGO放大 */
.sidebar-logo {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}
.sidebar-logo img {
  width: 40px;
  height: 40px;
  object-fit: contain;
}
.logo-text {
  color: #fff;
  font-size: 15px;
  font-weight: bold;
}

.el-menu-vertical-demo {
  height: calc(100% - 70px);
  border-right: none;
}

/* 主内容 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

/* 顶部导航 */
.navbar {
  height: 60px;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 左边：图片 + 系统名 */
.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.welcome-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}
.navbar-title {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
}

.user-info {
  font-size: 14px;
  color: #333;
}

.page-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>
