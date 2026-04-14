<template>
  <!-- 移除手动嵌套的 <Layout> -->
  <div class="supplier-staff">
    <h2 style="margin-bottom: 20px; color: #333">救援人员管理</h2>

    <!-- 操作按钮 -->
    <div
      style="margin-bottom: 20px; display: flex; justify-content: space-between"
    >
      <el-button type="primary" @click="openAddDialog">新增救援人员</el-button>
      <el-input
        v-model="searchName"
        placeholder="请输入人员姓名"
        style="width: 200px"
      ></el-input>
    </div>

    <!-- 人员列表 -->
    <el-card shadow="hover">
      <el-table :data="staffList" border style="width: 100%">
        <el-table-column prop="id" label="人员ID" width="80" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="skill" label="擅长救援类型" />
        <el-table-column prop="status" label="在线状态">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'online'" type="success">
              在线
            </el-tag>
            <el-tag v-if="scope.row.status === 'offline'" type="danger">
              离线
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="入职时间" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button
              type="text"
              size="small"
              @click="openEditDialog(scope.row)"
              >编辑</el-button
            >
            <el-button
              type="text"
              size="small"
              @click="changeStatus(scope.row)"
            >
              {{ scope.row.status === "online" ? "下线" : "上线" }}
            </el-button>
            <el-button
              type="text"
              size="small"
              style="color: #f53f3f"
              @click="handleDeleteStaff(scope.row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.currentPage"
        :page-sizes="[5, 10, 20]"
        :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        style="margin-top: 20px; text-align: right"
      ></el-pagination>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="staffFormRef"
        :model="staffForm"
        :rules="staffRules"
        label-width="100px"
      >
        <el-form-item label="姓名" prop="name">
          <el-input
            v-model="staffForm.name"
            placeholder="请输入救援人员姓名"
          ></el-input>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input
            v-model="staffForm.phone"
            placeholder="请输入11位手机号"
          ></el-input>
        </el-form-item>
        <el-form-item label="擅长救援类型" prop="skill">
          <el-input
            v-model="staffForm.skill"
            placeholder="请输入擅长救援类型，如：电瓶搭电、换胎"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
// 导入新增/编辑接口
import {
  getStaffList,
  changeStaffStatus,
  deleteStaff,
  addStaff,
  editStaff,
} from "@/api/supplier";

// 搜索名称
const searchName = ref("");

// 分页参数
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// 人员列表
const staffList = ref([]);

// 弹窗相关
const dialogVisible = ref(false); // 弹窗显隐
const dialogType = ref("add"); // add-新增 edit-编辑
const staffFormRef = ref(null); // 表单ref

// 表单数据
const staffForm = reactive({
  id: "", // 编辑时存储人员ID
  name: "",
  phone: "",
  skill: "",
});

// 表单校验规则
const staffRules = reactive({
  name: [
    { required: true, message: "请输入救援人员姓名", trigger: "blur" },
    { min: 2, max: 10, message: "姓名长度在 2 到 10 个字符", trigger: "blur" },
  ],
  phone: [
    { required: true, message: "请输入联系电话", trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的11位手机号",
      trigger: "blur",
    },
  ],
  skill: [
    { required: true, message: "请输入擅长救援类型", trigger: "blur" },
    { min: 2, message: "救援类型不能少于2个字符", trigger: "blur" },
  ],
});

// 获取人员列表（增加错误提示+返回码校验）
const getStaffListData = async () => {
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      name: searchName.value,
    };
    const res = await getStaffList(params);
    if (res.code === 200) {
      staffList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    } else {
      ElMessage.error("获取人员列表失败：" + res.msg);
    }
  } catch (error) {
    ElMessage.error("网络错误，获取人员列表失败");
    console.error("获取人员列表失败：", error);
  }
};

// 页面加载时获取数据
onMounted(() => {
  getStaffListData();
});

// 监听搜索名称变化
watch(searchName, () => {
  pagination.currentPage = 1;
  getStaffListData();
});

// 打开新增弹窗
const openAddDialog = () => {
  dialogType.value = "add";
  dialogVisible.value = true;
  // 重置表单
  resetForm();
};

// 打开编辑弹窗
const openEditDialog = (row) => {
  dialogType.value = "edit";
  dialogVisible.value = true;
  // 填充表单数据
  staffForm.id = row.id;
  staffForm.name = row.name;
  staffForm.phone = row.phone;
  staffForm.skill = row.skill;
};

// 重置表单
const resetForm = () => {
  if (staffFormRef.value) {
    staffFormRef.value.resetFields();
  }
  // 清空表单数据
  staffForm.id = "";
  staffForm.name = "";
  staffForm.phone = "";
  staffForm.skill = "";
};

// 获取弹窗标题
const dialogTitle = computed(() => {
  return dialogType.value === "add" ? "新增救援人员" : "编辑救援人员";
});

// 提交表单
const submitForm = async () => {
  try {
    // 表单校验
    const valid = await staffFormRef.value.validate();
    if (!valid) return;

    // 调用接口
    let res;
    if (dialogType.value === "add") {
      // 新增
      res = await addStaff(staffForm);
    } else {
      // 编辑
      res = await editStaff(staffForm);
    }

    // 处理结果
    if (res.code === 200) {
      ElMessage.success(dialogType.value === "add" ? "新增成功" : "编辑成功");
      dialogVisible.value = false;
      getStaffListData(); // 刷新列表
    } else {
      ElMessage.error(
        (dialogType.value === "add" ? "新增" : "编辑") + "失败：" + res.msg,
      );
    }
  } catch (error) {
    ElMessage.error(
      (dialogType.value === "add" ? "新增" : "编辑") + "失败，请重试",
    );
    console.error(
      dialogType.value === "add" ? "新增人员失败：" : "编辑人员失败：",
      error,
    );
  }
};

// 切换在线状态（调用后端接口）
const changeStatus = async (row) => {
  try {
    const newStatus = row.status === "online" ? "offline" : "online";
    const res = await changeStaffStatus(row.id, newStatus);
    if (res.code === 200) {
      ElMessage.success(
        `${row.name}已${newStatus === "online" ? "上线" : "下线"}`,
      );
      getStaffListData(); // 刷新列表
    } else {
      ElMessage.error("切换状态失败：" + res.msg);
    }
  } catch (error) {
    ElMessage.error("切换状态失败，请重试");
    console.error("切换状态失败：", error);
  }
};

// 修复命名冲突：删除人员函数改名
const handleDeleteStaff = async (row) => {
  try {
    await ElMessageBox.confirm("此操作将永久删除该人员, 是否继续?", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });
    const res = await deleteStaff(row.id);
    if (res.code === 200) {
      ElMessage.success("删除成功!");
      getStaffListData(); // 刷新列表
    } else {
      ElMessage.error("删除失败：" + res.msg);
    }
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除失败，请重试");
      console.error("删除失败：", error);
    } else {
      ElMessage.info("已取消删除");
    }
  }
};

// 分页事件
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  getStaffListData();
};

const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  getStaffListData();
};
</script>

<style scoped>
.el-pagination {
  margin-top: 20px;
  text-align: right;
}

.supplier-staff {
  padding: 20px;
}
</style>
