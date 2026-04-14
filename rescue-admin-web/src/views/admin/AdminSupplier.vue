<template>
  <div class="admin-supplier">
    <h2 style="margin-bottom: 20px; color: #333">用户管理</h2>

    <!-- 筛选+操作按钮 -->
    <div
      style="
        display: flex;
        justify-content: space-between;
        margin-bottom: 20px;
        flex-wrap: wrap;
        gap: 10px;
      "
    >
      <div style="display: flex; gap: 10px; flex-wrap: wrap">
        <el-input
          v-model="searchName"
          placeholder="请输入服务商名称"
          style="width: 200px"
        ></el-input>
        <el-select
          v-model="searchStatus"
          placeholder="请选择状态"
          style="width: 150px"
        >
          <el-option label="全部" value=""></el-option>
          <el-option label="已启用" value="enable"></el-option>
          <el-option label="已禁用" value="disable"></el-option>
          <el-option label="待审核" value="pending"></el-option>
        </el-select>
        <el-button type="primary" @click="searchSupplier">查询</el-button>
      </div>
      <el-button type="primary" @click="addSupplier">新增服务商</el-button>
    </div>

    <!-- 服务商列表 -->
    <el-card shadow="hover" style="margin-bottom: 20px">
      <template #header>
        <span>服务商列表</span>
      </template>
      <el-table :data="supplierList" border style="width: 100%">
        <el-table-column prop="id" label="服务商ID" width="100" />
        <el-table-column prop="name" label="服务商名称" />
        <el-table-column prop="contact" label="联系人" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="city" label="所在城市" />
        <el-table-column prop="createTime" label="入驻时间">
          <template #default="scope">
            {{ scope.row.createTime ? formatDate(scope.row.createTime) : "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'enable'" type="success">
              已启用
            </el-tag>
            <el-tag v-if="scope.row.status === 'disable'" type="danger">
              已禁用
            </el-tag>
            <el-tag v-if="scope.row.status === 'pending'" type="warning">
              待审核
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="text" size="small" @click="editSupplier(scope.row)"
              >编辑</el-button
            >
            <el-button
              v-if="scope.row.status === 'enable'"
              type="text"
              size="small"
              style="color: #f53f3f"
              @click="changeStatus(scope.row, 'disable')"
            >
              禁用
            </el-button>
            <el-button
              v-if="scope.row.status === 'disable'"
              type="primary"
              size="small"
              @click="changeStatus(scope.row, 'enable')"
            >
              启用
            </el-button>
            <el-button type="text" size="small" @click="viewDetail(scope.row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页：改为 5 / 10 / 20 -->
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.currentPage"
        :page-sizes="[5, 10, 20]"
        :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        style="margin-top: 20px; text-align: right"
      >
      </el-pagination>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <span>平台用户数据（车主/管理员）</span>
        <div style="display: flex; gap: 10px; margin-top: 10px">
          <el-input
            v-model="userSearchAccount"
            placeholder="账号/手机号"
            style="width: 200px"
          />
          <el-select
            v-model="userSearchIdentity"
            placeholder="身份类型"
            style="width: 130px"
          >
            <el-option label="全部" value="" />
            <el-option label="管理员" value="1" />
            <el-option label="车主" value="3" />
          </el-select>
          <el-button type="primary" @click="searchUser">查询</el-button>
        </div>
      </template>

      <el-table :data="userList" border style="width: 100%; margin-top: 10px">
        <el-table-column prop="account" label="登录账号" min-width="120" />
        <el-table-column prop="nickname" label="用户昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="identityType" label="身份类型">
          <template #default="scope">
            <el-tag
              :type="scope.row.identityType === 1 ? 'primary' : 'success'"
            >
              {{ scope.row.identityType === 1 ? "管理员" : "车主" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="账号状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? "启用" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ scope.row.createTime ? formatDate(scope.row.createTime) : "-" }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 用户分页：也改为 5 / 10 / 20 -->
      <el-pagination
        @size-change="handleUserSizeChange"
        @current-change="handleUserCurrentChange"
        :current-page="userPagination.currentPage"
        :page-sizes="[5, 10, 20]"
        :page-size="userPagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="userPagination.total"
        style="margin-top: 20px; text-align: right"
      />
    </el-card>
    <!-- ====================== 👆 用户表格结束 ====================== -->

    <!-- 新增/编辑服务商弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="supplierFormRef"
        :model="supplierForm"
        :rules="supplierRules"
        label-width="100px"
      >
        <el-form-item label="服务商ID" prop="id">
          <el-input
            v-model="supplierForm.id"
            placeholder="请输入服务商ID"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="服务商名称" prop="name">
          <el-input
            v-model="supplierForm.name"
            placeholder="请输入服务商名称"
          />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="supplierForm.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="supplierForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="所在城市" prop="city">
          <el-input v-model="supplierForm.city" placeholder="请输入所在城市" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input
            v-model="supplierForm.address"
            type="textarea"
            placeholder="请输入详细地址"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="isEdit">
          <el-select v-model="supplierForm.status" placeholder="请选择状态">
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已启用" value="enable"></el-option>
            <el-option label="已禁用" value="disable"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

    <!-- 服务商详情弹窗 -->
    <el-dialog v-model="detailVisible" title="服务商详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="服务商ID">{{
          detailForm.id || "-"
        }}</el-descriptions-item>
        <el-descriptions-item label="服务商名称">{{
          detailForm.name || "-"
        }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{
          detailForm.contact || "-"
        }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{
          detailForm.phone || "-"
        }}</el-descriptions-item>
        <el-descriptions-item label="所在城市">{{
          detailForm.city || "-"
        }}</el-descriptions-item>
        <el-descriptions-item label="详细地址">{{
          detailForm.address || "-"
        }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="detailForm.status === 'enable'" type="success"
            >已启用</el-tag
          >
          <el-tag v-if="detailForm.status === 'disable'" type="danger"
            >已禁用</el-tag
          >
          <el-tag v-if="detailForm.status === 'pending'" type="warning"
            >待审核</el-tag
          >
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="入驻时间">{{
          detailForm.createTime ? formatDate(detailForm.createTime) : "-"
        }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{
          detailForm.updateTime ? formatDate(detailForm.updateTime) : "-"
        }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatDate } from "@/utils/dateUtil";
import {
  getAdminSupplierList,
  changeSupplierStatus,
  getSupplierDetail,
  saveSupplier,
  updateSupplier,
  getUserList,
} from "@/api/admin";

const searchName = ref("");
const searchStatus = ref("");

const pagination = reactive({
  currentPage: 1,
  pageSize: 5, // 默认从5条开始
  total: 0,
});

const supplierList = ref([]);
const dialogVisible = ref(false);
const detailVisible = ref(false);
const dialogTitle = ref("");
const isEdit = ref(false);

const supplierFormRef = ref(null);
const supplierForm = reactive({
  id: "",
  name: "",
  contact: "",
  phone: "",
  city: "",
  address: "",
  status: "pending",
});

const detailForm = reactive({});

const validateIdUnique = (rule, value, callback) => {
  if (!value) return callback();
  if (isEdit.value) return callback();
  const isExist = supplierList.value.some((item) => item.id === value);
  if (isExist) callback(new Error("ID已存在"));
  else callback();
};

const supplierRules = reactive({
  id: [
    { required: true, message: "请输入服务商ID", trigger: "blur" },
    {
      pattern: /^[a-zA-Z0-9_-]{1,50}$/,
      message: "ID格式错误",
      trigger: "blur",
    },
    { validator: validateIdUnique, trigger: "blur" },
  ],
  name: [{ required: true, message: "请输入名称", trigger: "blur" }],
  contact: [{ required: true, message: "请输入联系人", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式错误", trigger: "blur" },
  ],
  city: [{ required: true, message: "请输入城市", trigger: "blur" }],
});

const getSupplierListData = async () => {
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      name: searchName.value,
      status: searchStatus.value,
    };
    const res = await getAdminSupplierList(params);
    if (res.code === 200) {
      supplierList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    }
  } catch (e) {
    ElMessage.error("获取服务商失败");
  }
};

const addSupplier = () => {
  dialogTitle.value = "新增";
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

const editSupplier = async (row) => {
  const res = await getSupplierDetail(row.id);
  Object.assign(supplierForm, res.data);
  isEdit.value = true;
  dialogTitle.value = "编辑";
  dialogVisible.value = true;
};

const viewDetail = async (row) => {
  const res = await getSupplierDetail(row.id);
  Object.assign(detailForm, res.data);
  detailVisible.value = true;
};

const submitForm = async () => {
  await supplierFormRef.value.validate();
  if (isEdit.value) await updateSupplier(supplierForm);
  else await saveSupplier(supplierForm);
  ElMessage.success("操作成功");
  dialogVisible.value = false;
  getSupplierListData();
};

const resetForm = () => {
  supplierFormRef.value?.resetFields();
  Object.assign(supplierForm, {
    id: "",
    name: "",
    contact: "",
    phone: "",
    city: "",
    address: "",
    status: "pending",
  });
};

const changeStatus = async (row, status) => {
  await ElMessageBox.confirm("确认操作？");
  await changeSupplierStatus(row.id, status);
  ElMessage.success("操作成功");
  getSupplierListData();
};

const handleSizeChange = (val) => {
  pagination.pageSize = val;
  getSupplierListData();
};
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  getSupplierListData();
};
const searchSupplier = () => {
  pagination.currentPage = 1;
  getSupplierListData();
};

const userList = ref([]);
const userPagination = reactive({ currentPage: 1, pageSize: 5, total: 0 });
const userSearchAccount = ref("");
const userSearchIdentity = ref("");

const getUserListData = async () => {
  try {
    const res = await getUserList({
      pageNum: userPagination.currentPage,
      pageSize: userPagination.pageSize,
      account: userSearchAccount.value,
      identityType: userSearchIdentity.value,
    });
    userList.value = res.data.records || [];
    userPagination.total = res.data.total || 0;
  } catch (e) {
    ElMessage.error("获取用户列表失败");
  }
};

const searchUser = () => {
  userPagination.currentPage = 1;
  getUserListData();
};

const handleUserSizeChange = (val) => {
  userPagination.pageSize = val;
  getUserListData();
};

const handleUserCurrentChange = (val) => {
  userPagination.currentPage = val;
  getUserListData();
};

onMounted(() => {
  getSupplierListData();
  getUserListData();
});
</script>

<style scoped>
.admin-supplier {
  width: 100%;
  padding: 0 20px;
}
</style>
