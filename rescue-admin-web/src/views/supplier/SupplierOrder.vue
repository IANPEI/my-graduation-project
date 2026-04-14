<template>
  <div class="supplier-order">
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
          <el-option label="待处理" value="pending" />
          <el-option label="处理中" value="processing" />
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
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="searchOrder">查询</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-card shadow="hover">
      <el-table :data="orderList" border style="width: 100%">
        <el-table-column prop="orderNo" label="订单编号" min-width="220" />
        <el-table-column prop="faultTypeName" label="故障类型" width="120" />
        <el-table-column prop="nickname" label="车主姓名" width="120" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column
          prop="address"
          label="救援地址"
          min-width="260"
          show-overflow-tooltip
        />

        <!-- 状态 -->
        <el-table-column label="订单状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'pending'" type="warning">
              待处理
            </el-tag>
            <el-tag v-if="scope.row.status === 'processing'" type="primary">
              处理中
            </el-tag>
            <el-tag v-if="scope.row.status === 'completed'" type="success">
              已完成
            </el-tag>
            <el-tag v-if="scope.row.status === 'cancelled'" type="danger">
              已取消
            </el-tag>
          </template>
        </el-table-column>

        <!-- 评价状态 -->
        <el-table-column label="评价状态" width="110">
          <template #default="scope">
            <el-tag v-if="scope.row.evaluateStatus === 1" type="success">
              已评价
            </el-tag>
            <el-tag v-else type="info"> 未评价 </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="提交时间" width="180" />

        <!-- 操作 -->
        <el-table-column label="操作" width="280">
          <template #default="scope">
            <el-button type="text" size="small" @click="viewDetail(scope.row)">
              详情
            </el-button>

            <!-- 查看评价按钮 -->
            <el-button
              type="text"
              size="small"
              @click="viewEvaluate(scope.row)"
            >
              查看评价
            </el-button>

            <el-button
              v-if="canAccept(scope.row)"
              type="primary"
              size="small"
              @click="handleAcceptOrder(scope.row)"
            >
              接单
            </el-button>
            <el-button
              v-if="canComplete(scope.row)"
              type="success"
              size="small"
              @click="handleCompleteOrder(scope.row)"
            >
              完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.currentPage"
        :page-sizes="[5, 10, 20, 30]"
        :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        style="margin-top: 20px; text-align: right"
      />
    </el-card>

    <!-- 订单详情抽屉 -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="订单详情"
      size="70%"
      direction="rtl"
      :with-header="true"
    >
      <div v-if="currentOrderDetail" class="order-detail-container">
        <el-descriptions
          title="订单核心信息"
          :column="2"
          border
          style="margin-bottom: 20px"
        >
          <el-descriptions-item label="故障类型">
            {{ currentOrderDetail.faultTypeName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="订单编号">
            {{ currentOrderDetail.orderNo || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="车主姓名">
            {{ currentOrderDetail.nickname || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ currentOrderDetail.phone || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="救援地址" :span="2">
            {{ currentOrderDetail.address || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ currentOrderDetail.createTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <span v-if="currentOrderDetail.status === 'pending'">待处理</span>
            <span v-else-if="currentOrderDetail.status === 'processing'"
              >处理中</span
            >
            <span v-else-if="currentOrderDetail.status === 'completed'"
              >已完成</span
            >
            <span v-else-if="currentOrderDetail.status === 'cancelled'"
              >已取消</span
            >
            <span v-else>{{ currentOrderDetail.status || "-" }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="评价状态">
            <span
              v-if="currentOrderDetail.evaluateStatus === 1"
              style="color: green"
              >已评价</span
            >
            <span v-else style="color: #999">未评价</span>
          </el-descriptions-item>
          <el-descriptions-item label="订单备注" :span="2">
            {{ currentOrderDetail.remark || "无" }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else class="empty-tip">
        <el-empty description="暂无订单详情数据" />
      </div>
    </el-drawer>

    <!-- 评价详情抽屉 -->
    <el-drawer
      v-model="evaluateDrawerVisible"
      title="订单评价详情"
      size="50%"
      direction="rtl"
    >
      <div v-if="currentEvaluate" class="evaluate-container">
        <el-descriptions title="评价信息" :column="1" border>
          <el-descriptions-item label="订单编号">
            {{ currentEvaluate.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="车主评分">
            <el-rate v-model="currentEvaluate.score" disabled />
            <span style="margin-left: 10px"
              >{{ currentEvaluate.score }} 星</span
            >
          </el-descriptions-item>
          <el-descriptions-item label="评价内容">
            {{ currentEvaluate.content || "无评价内容" }}
          </el-descriptions-item>
          <el-descriptions-item label="评价时间">
            {{ currentEvaluate.createTime }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else class="empty-tip">
        <el-empty description="暂无评价数据" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox, ElEmpty } from "element-plus";
import dayjs from "dayjs";

import {
  getSupplierOrderList,
  getOrderDetail,
  acceptOrder,
  completeOrder,
  // 新增评价接口
  getOrderEvaluate,
} from "@/api/supplier";

/* 当前供应商ID */
const currentSupplierId = localStorage.getItem("supplierId");

/* 搜索表单 */
const searchForm = reactive({
  orderNo: "",
  status: "",
  dateRange: [],
});

/* 分页 */
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

/* 订单列表 */
const orderList = ref([]);

/* 详情抽屉 */
const detailDrawerVisible = ref(false);
const currentOrderDetail = ref(null);

/* 评价抽屉 */
const evaluateDrawerVisible = ref(false);
const currentEvaluate = ref(null);

/* 获取订单列表 */
const getOrderList = async () => {
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      orderNo: searchForm.orderNo,
      status: searchForm.status,
      startDate: searchForm.dateRange?.[0]
        ? dayjs(searchForm.dateRange[0]).format("YYYY-MM-DD")
        : "",
      endDate: searchForm.dateRange?.[1]
        ? dayjs(searchForm.dateRange[1]).format("YYYY-MM-DD")
        : "",
    };

    const res = await getSupplierOrderList(params);
    if (res.code === 200) {
      orderList.value = res.data.records;
      pagination.total = res.data.total;
    } else {
      ElMessage.error("获取订单列表失败：" + res.msg);
    }
  } catch (error) {
    ElMessage.error("网络错误");
    console.error(error);
  }
};

onMounted(() => {
  getOrderList();
});

/* 查询 */
const searchOrder = () => {
  pagination.currentPage = 1;
  getOrderList();
};

/* 查看订单详情 */
const viewDetail = async (row) => {
  try {
    detailDrawerVisible.value = true;
    currentOrderDetail.value = null;
    const res = await getOrderDetail(row.orderNo);
    if (res.code === 200) {
      currentOrderDetail.value = res.data;
    } else {
      ElMessage.error("获取详情失败：" + res.msg);
    }
  } catch (error) {
    ElMessage.error("获取详情出错");
  }
};

/* ====================== 查看评价 ====================== */
const viewEvaluate = async (row) => {
  // 未评价直接提示
  if (row.evaluateStatus !== 1) {
    ElMessage.info("该订单尚未评价！");
    return;
  }

  try {
    evaluateDrawerVisible.value = true;
    currentEvaluate.value = null;

    const res = await getOrderEvaluate(row.orderNo);
    if (res.code === 200) {
      currentEvaluate.value = res.data;
      // 格式化时间
      if (currentEvaluate.value.createTime) {
        currentEvaluate.value.createTime = dayjs(
          currentEvaluate.value.createTime,
        ).format("YYYY-MM-DD HH:mm:ss");
      }
    } else {
      ElMessage.error("获取评价失败：" + res.msg);
    }
  } catch (error) {
    ElMessage.error("获取评价出错");
    console.error(error);
  }
};

/* 是否可接单 */
const canAccept = (row) => {
  return (!row.supplierId || row.supplierId === "") && row.status === "pending";
};

/* 是否可完成 */
const canComplete = (row) => {
  return row.supplierId == currentSupplierId && row.status === "processing";
};

/* 接单 */
const handleAcceptOrder = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认接单订单【${row.orderNo}】吗？`,
      "接单确认",
      { type: "warning" },
    );
    await acceptOrder(row.orderNo, currentSupplierId);
    ElMessage.success("接单成功");
    getOrderList();
  } catch (error) {
    if (error !== "cancel") ElMessage.error("接单失败");
  }
};

/* 完成订单 */
const handleCompleteOrder = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认完成订单【${row.orderNo}】吗？`,
      "完成确认",
      { type: "warning" },
    );
    await completeOrder(row.orderNo);
    ElMessage.success("订单已完成");
    getOrderList();
  } catch (error) {
    if (error !== "cancel") ElMessage.error("完成失败");
  }
};

/* 分页 */
const handleSizeChange = (val) => {
  pagination.pageSize = val;
  getOrderList();
};
const handleCurrentChange = (val) => {
  pagination.currentPage = val;
  getOrderList();
};
</script>

<style scoped>
.supplier-order {
  padding: 20px;
}
.order-detail-container,
.evaluate-container {
  padding: 10px 0;
}
.empty-tip {
  padding: 40px 0;
  text-align: center;
}
.el-descriptions {
  --el-descriptions-item-label-width: 120px;
}
</style>
