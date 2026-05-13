<template>
  <div class="admin-order">
    <h2 style="margin-bottom: 20px; color: #333">订单管理</h2>

    <!-- 查询条件 -->
    <el-form :inline="true" :model="searchForm" style="margin-bottom: 20px">
      <el-form-item label="订单编号">
        <el-input
          v-model="searchForm.orderNo"
          placeholder="请输入订单编号"
          style="width: 200px"
        />
      </el-form-item>

      <el-form-item label="订单状态">
        <el-select
          v-model="searchForm.status"
          placeholder="请选择"
          style="width: 160px"
        >
          <el-option label="全部" value="" />
          <el-option label="待接单" value="pending" />
          <el-option label="救援中" value="processing" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
      </el-form-item>

      <el-form-item label="时间范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="searchOrder">查询</el-button>
      </el-form-item>
    </el-form>

    <!-- 订单表格（只展示6个关键字段） -->
    <el-card shadow="hover" style="margin-bottom: 15px">
      <el-table :data="orderList" border style="width: 100%" fit>
        <el-table-column prop="orderNo" label="订单编号" min-width="180" />
        <el-table-column prop="nickname" label="车主姓名" min-width="120" />
        <el-table-column prop="phone" label="车主电话" min-width="160" />

        <!-- 订单状态 -->
        <el-table-column label="订单状态" min-width="130">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'pending'" type="warning"
              >待接单</el-tag
            >
            <el-tag v-else-if="scope.row.status === 'processing'" type="primary"
              >救援中</el-tag
            >
            <el-tag v-else-if="scope.row.status === 'completed'" type="success"
              >已完成</el-tag
            >
            <el-tag v-else-if="scope.row.status === 'cancelled'" type="danger"
              >已取消</el-tag
            >
          </template>
        </el-table-column>

        <!-- 下单时间 -->
        <el-table-column label="下单时间" min-width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" min-width="120" align="center">
          <template #default="scope">
            <el-button type="text" size="small" @click="openDetail(scope.row)"
              >查看详情</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="pagination.currentPage"
      :page-sizes="[5, 10, 20]"
      :page-size="pagination.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      style="text-align: right"
    />

    <!-- 订单详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="订单详情"
      size="65%"
      direction="rtl"
    >
      <div v-if="currentRow" class="detail-box">
        <el-descriptions :column="2" border title="订单完整信息">
          <el-descriptions-item label="订单号">{{
            currentRow.orderNo
          }}</el-descriptions-item>
          <el-descriptions-item label="故障类型">{{
            currentRow.faultTypeName
          }}</el-descriptions-item>
          <el-descriptions-item label="车主姓名">{{
            currentRow.nickname
          }}</el-descriptions-item>
          <el-descriptions-item label="车主电话">{{
            currentRow.phone
          }}</el-descriptions-item>
          <el-descriptions-item label="救援地址" :span="2">{{
            currentRow.address
          }}</el-descriptions-item>
          <el-descriptions-item label="服务商">{{
            currentRow.supplierName
          }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{
            currentRow.supplierContact
          }}</el-descriptions-item>
          <el-descriptions-item label="服务商电话">{{
            currentRow.supplierPhone
          }}</el-descriptions-item>
          <el-descriptions-item label="救援师傅">{{
            currentRow.staffName
          }}</el-descriptions-item>
          <el-descriptions-item label="师傅电话">{{
            currentRow.staffPhone
          }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{
            currentRow.statusDesc
          }}</el-descriptions-item>
          <el-descriptions-item label="评价状态">{{
            currentRow.evaluateStatusDesc
          }}</el-descriptions-item>
          <el-descriptions-item label="评价内容" :span="2">
            {{ currentRow.content || "暂无评价" }}
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{
            formatTime(currentRow.createTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{
            formatTime(currentRow.updateTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="订单备注" :span="2">{{
            currentRow.remark || "无"
          }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import dayjs from "dayjs";
import { getAdminOrderList } from "@/api/admin";

// 查询条件
const searchForm = reactive({
  orderNo: "",
  status: "",
  dateRange: [],
});

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

const orderList = ref([]);
const drawerVisible = ref(false);
const currentRow = ref(null);

// 获取订单列表
const getOrderList = async () => {
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      orderNo: searchForm.orderNo,
      status: searchForm.status,
      startDate: searchForm.dateRange?.[0] || "",
      endDate: searchForm.dateRange?.[1] || "",
    };
    const res = await getAdminOrderList(params);
    if (res.code === 200) {
      orderList.value = res.data.records;
      pagination.total = res.data.total;
    }
  } catch (e) {
    ElMessage.error("获取订单失败");
  }
};

// 时间格式化（修复时间显示）
const formatTime = (time) => {
  if (!time) return "-";
  return dayjs(time).format("YYYY-MM-DD HH:mm:ss");
};

// 打开详情
const openDetail = (row) => {
  currentRow.value = row;
  drawerVisible.value = true;
};

// 查询
const searchOrder = () => {
  pagination.currentPage = 1;
  getOrderList();
};

// 分页
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  getOrderList();
};
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  getOrderList();
};

onMounted(() => {
  getOrderList();
});
</script>

<style scoped>
.admin-order {
  padding: 20px;
}
.detail-box {
  padding: 10px 0;
}
</style>
