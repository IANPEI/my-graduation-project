<template>
  <div class="admin-system">
    <h2 style="margin-bottom: 20px; color: #333">系统配置</h2>

    <!-- 基础配置 -->
    <el-card shadow="hover" style="margin-bottom: 20px">
      <template #header>
        <span>基础配置</span>
      </template>
      <el-form :model="baseConfig" label-width="120px" style="max-width: 800px">
        <el-form-item label="平台名称">
          <el-input
            v-model="baseConfig.platformName"
            placeholder="请输入平台名称"
          ></el-input>
        </el-form-item>
        <el-form-item label="客服电话">
          <el-input
            v-model="baseConfig.servicePhone"
            placeholder="请输入客服电话"
          ></el-input>
        </el-form-item>
        <el-form-item label="救援响应时限（分钟）">
          <el-input-number
            v-model="baseConfig.responseTime"
            :min="5"
            :max="60"
          ></el-input-number>
        </el-form-item>
        <el-form-item label="平台公告">
          <el-input
            v-model="baseConfig.notice"
            type="textarea"
            :rows="5"
            placeholder="请输入平台公告"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveBaseConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 故障类型配置 -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>故障类型配置</span>
          <el-button type="primary" size="small" @click="openFaultTypeDialog()">
            添加故障类型
          </el-button>
        </div>
      </template>
      <el-table :data="faultTypeList" border style="width: 100%">
        <el-table-column prop="id" label="类型ID" width="80" />
        <el-table-column prop="name" label="故障类型名称" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button
              type="text"
              size="small"
              @click="openFaultTypeDialog(scope.row)"
              >编辑</el-button
            >
            <el-button
              type="text"
              size="small"
              style="color: #f53f3f"
              @click="handleDeleteFaultType(scope.row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑故障类型弹窗 -->
    <el-dialog
      v-model="faultTypeDialogVisible"
      title="故障类型配置"
      width="500px"
      @close="resetFaultTypeForm"
    >
      <el-form
        :model="faultTypeForm"
        :rules="faultTypeRules"
        ref="faultTypeFormRef"
        label-width="120px"
      >
        <el-form-item label="故障类型名称" prop="name">
          <el-input
            v-model="faultTypeForm.name"
            placeholder="请输入故障类型名称"
          ></el-input>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="faultTypeForm.sort"
            :min="1"
            :max="999"
            placeholder="请输入排序值"
          ></el-input-number>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="faultTypeForm.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faultTypeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFaultTypeForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
// 导入所有需要的接口
import {
  getSystemConfig,
  saveSystemConfig,
  getFaultTypeList,
  deleteFaultType,
  saveFaultType,
  updateFaultType,
  getFaultTypeDetail,
} from "@/api/admin";

// 基础配置（从后端获取）
const baseConfig = reactive({
  platformName: "",
  servicePhone: "",
  responseTime: 0,
  notice: "",
});

// 故障类型列表
const faultTypeList = ref([]);

// 故障类型弹窗相关
const faultTypeDialogVisible = ref(false);
const faultTypeFormRef = ref(null); 
const faultTypeForm = reactive({
  id: "", 
  name: "", 
  sort: 1, 
  status: "1", 
});
// 表单校验规则
const faultTypeRules = {
  name: [
    { required: true, message: "请输入故障类型名称", trigger: "blur" },
    { min: 2, max: 20, message: "名称长度在 2 到 20 个字符", trigger: "blur" },
  ],
  sort: [
    { required: true, message: "请输入排序值", trigger: "blur" },
  ],
  status: [
    { required: true, message: "请选择状态", trigger: "change" },
  ],
};

// 获取系统配置和故障类型列表
const getConfigData = async () => {
  try {
    // 获取基础配置
    const configRes = await getSystemConfig();
    if (configRes.code === 200) {
      Object.assign(baseConfig, configRes.data);
    } else {
      ElMessage.error("获取基础配置失败：" + configRes.msg);
    }

    // 获取故障类型列表
    await fetchFaultTypeList();
  } catch (error) {
    ElMessage.error("网络错误，获取系统配置失败");
    console.error("获取系统配置失败：", error);
  }
};

// 单独封装获取故障类型列表的方法（方便复用）
const fetchFaultTypeList = async () => {
  try {
    const faultRes = await getFaultTypeList();
    if (faultRes.code === 200) {
      faultTypeList.value = faultRes.data || [];
    } else {
      ElMessage.error("获取故障类型列表失败：" + faultRes.msg);
    }
  } catch (error) {
    ElMessage.error("获取故障类型列表失败");
    console.error(error);
  }
};

// 页面加载时获取数据
onMounted(() => {
  getConfigData();
});

// 保存基础配置
const saveBaseConfig = async () => {
  try {
    const res = await saveSystemConfig(baseConfig);
    if (res.code === 200) {
      ElMessage.success("基础配置保存成功");
    } else {
      ElMessage.error("保存失败：" + res.msg);
    }
  } catch (error) {
    ElMessage.error("保存配置失败，请重试");
    console.error("保存配置失败：", error);
  }
};

// 打开新增/编辑故障类型弹窗
const openFaultTypeDialog = async (row = null) => {
  // 重置表单
  resetFaultTypeForm();
  if (row) {
    // 编辑模式：回显数据
    faultTypeDialogVisible.value = true;
    try {
      const res = await getFaultTypeDetail(row.id);
      if (res.code === 200) {
        const data = res.data;
        faultTypeForm.id = data.id;
        faultTypeForm.name = data.name;
        faultTypeForm.sort = data.sort;
        faultTypeForm.status = data.status.toString(); // 转字符串匹配radio值
      } else {
        ElMessage.error("获取故障类型详情失败：" + res.msg);
      }
    } catch (error) {
      ElMessage.error("获取故障类型详情失败");
      console.error(error);
    }
  } else {
    // 新增模式：直接打开弹窗
    faultTypeDialogVisible.value = true;
  }
};

// 重置故障类型表单
const resetFaultTypeForm = () => {
  if (faultTypeFormRef.value) {
    faultTypeFormRef.value.resetFields();
  }
  faultTypeForm.id = "";
  faultTypeForm.name = "";
  faultTypeForm.sort = 1;
  faultTypeForm.status = "1";
};

// 提交故障类型表单（新增/编辑）
const submitFaultTypeForm = async () => {
  try {
    // 表单校验
    await faultTypeFormRef.value.validate();
    // 构造提交数据
    const submitData = {
      name: faultTypeForm.name,
      sort: Number(faultTypeForm.sort), // 转数字
      status: Number(faultTypeForm.status), // 转数字
    };
    if (faultTypeForm.id) {
      // 编辑模式：补充id
      submitData.id = faultTypeForm.id;
      const res = await updateFaultType(submitData);
      if (res.code === 200) {
        ElMessage.success("编辑故障类型成功");
      } else {
        ElMessage.error("编辑失败：" + res.msg);
        return;
      }
    } else {
      // 新增模式
      const res = await saveFaultType(submitData);
      if (res.code === 200) {
        ElMessage.success("新增故障类型成功");
      } else {
        ElMessage.error("新增失败：" + res.msg);
        return;
      }
    }
    // 关闭弹窗 + 刷新列表
    faultTypeDialogVisible.value = false;
    await fetchFaultTypeList();
  } catch (error) {
    if (error.name !== "ValidationError") {
      ElMessage.error("提交失败，请重试");
      console.error("提交故障类型失败：", error);
    }
  }
};

// 删除故障类型
const handleDeleteFaultType = async (row) => {
  try {
    await ElMessageBox.confirm(
      "此操作将永久删除该故障类型, 是否继续?",
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    const res = await deleteFaultType(row.id);
    if (res.code === 200) {
      ElMessage.success("删除成功!");
      await fetchFaultTypeList(); // 刷新列表
    } else {
      ElMessage.error("删除失败：" + res.msg);
    }
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除故障类型失败，请重试");
      console.error("删除故障类型失败：", error);
    } else {
      ElMessage.info("已取消删除");
    }
  }
};
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-system {
  width: 100%;
  height: 100%;
  padding: 0 20px;
  box-sizing: border-box;
}
</style>