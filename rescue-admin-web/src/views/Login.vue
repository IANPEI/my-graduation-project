<template>
  <div class="login-wrapper">
    <el-card class="login-card" shadow="hover">
      <div class="logo">🚗 汽车救援服务管理平台</div>

      <el-form
        :model="loginForm"
        ref="loginFormRef"
        label-width="80px"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="登录账号" prop="account">
          <el-input
            v-model="loginForm.account"
            placeholder="请输入账号"
          ></el-input>
        </el-form-item>

        <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="login-btn" @click="handleLogin"
            >登录</el-button
          >
        </el-form-item>
      </el-form>

      <p class="aux-tip">请输入正确的账号密码，系统将自动识别您的身份</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { login } from "@/api/login";

const loginFormRef = ref(null);
const router = useRouter();

const loginForm = reactive({
  account: "",
  password: "",
});

const handleLogin = async () => {
  if (!loginForm.account || !loginForm.password) {
    ElMessage.warning("账号/密码不能为空");
    return;
  }

  try {
    const res = await login({
      account: loginForm.account,
      password: loginForm.password,
    });

    console.log("登录返回数据：", res);

    localStorage.setItem("token", res.data.token || "");
    localStorage.setItem("identity", res.data.identity || "");
    localStorage.setItem("account", res.data.account || "");

    if (res.data.supplierId) {
      localStorage.setItem("supplierId", res.data.supplierId);
    }
    if (res.data.supplierName) {
      localStorage.setItem("supplierName", res.data.supplierName);
    }

    const identity = res.data.identity;
    if (identity === "admin") {
      await router.push("/admin/home");
    } else if (identity === "supplier") {
      await router.push("/supplier/home");
    } else {
      ElMessage.error("身份识别失败，请联系管理员");
      return;
    }

    ElMessage.success("登录成功");
  } catch (error) {
    console.error("登录失败详情：", error);
    ElMessage.error(error.msg || "登录失败，请检查账号密码");
  }
};
</script>

<style scoped>
.login-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: url("@/assets/rescue-bg.png") no-repeat center center;
  background-size: cover;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.1), rgba(0, 0, 0, 0.4)),
    url("@/assets/rescue-bg.png");
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  position: relative;
}

.login-wrapper::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(245, 248, 250, 0.6);
  z-index: 0;
}

.login-card {
  width: 100%;
  max-width: 450px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.9) !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12) !important;
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  position: relative;
  z-index: 1;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.logo {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  text-align: center;
  margin-bottom: 30px;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.login-btn {
  width: 100%;
  background-color: #2185d0 !important;
  border: none !important;
  border-radius: 6px !important;
}

.aux-tip {
  text-align: center;
  font-size: 12px;
  color: #666;
  margin-top: 20px;
}
</style>
