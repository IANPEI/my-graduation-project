<template>
  <div class="supplier-home">
    <!-- 双图表容器（饼图+柱状图） -->
    <div class="charts-wrapper">
      <div class="chart-card">
        <h3 style="margin: 0 0 15px 0; color: #333">故障类型分布</h3>
        <div id="faultTypeChart" style="width: 100%; height: 300px"></div>
      </div>
      <div class="chart-card">
        <h3 style="margin: 0 0 15px 0; color: #333">近7日订单数量</h3>
        <div id="orderCountChart" style="width: 100%; height: 300px"></div>
      </div>
    </div>

    <!-- 查询栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchOrderNo"
        placeholder="请输入订单编号"
        style="width: 260px"
        clearable
      />
      <el-button type="primary" @click="handleSearch"> 查询 </el-button>
      <el-button @click="handleReset"> 重置 </el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="orderList"
      border
      v-loading="loading"
      empty-text="暂无订单数据"
      style="width: 100%"
    >
      <el-table-column prop="orderNo" label="订单编号" min-width="200" />
      <el-table-column prop="faultTypeName" label="故障类型" width="120" />
      <el-table-column prop="nickname" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column
        prop="address"
        label="救援地址"
        min-width="280"
        show-overflow-tooltip
      />
      <el-table-column label="订单状态" width="120">
        <template #default="{ row }">
          <el-tag type="success">
            {{ row.statusDesc }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否已评价" width="120">
        <template #default="{ row }">
          <el-tag :type="row.evaluateStatus === 1 ? 'success' : 'info'">
            {{ row.evaluateStatus === 1 ? "已评价" : "未评价" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            type="warning"
            size="small"
            @click="exportOrderDetail(row)"
          >
            下载订单
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      :current-page="pageNum"
      :page-size="pageSize"
      :page-sizes="[5, 10, 20, 30]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      style="margin-top: 20px; text-align: right"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getSupplierHomeData,
  exportOrder,
  getOrderStatistics, // 新增：引入统计接口
} from "@/api/supplier";
import dayjs from "dayjs";
// 引入 ECharts
import * as echarts from "echarts";

const orderList = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchOrderNo = ref("");
// 定义两个图表实例
let faultTypeChart = null;
let orderCountChart = null;

/**
 * 获取订单列表（仅负责表格数据）
 */
const getOrderList = async () => {
  loading.value = true;

  try {
    // 补充分页参数传递（修复原代码未传参问题）
    const res = await getSupplierHomeData({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });

    if (res.code === 200) {
      // 只保留已完成订单
      let completedOrders = (res.data.records || []).filter(
        (item) => item.status === "completed",
      );

      // 如果输入订单号则过滤
      if (searchOrderNo.value) {
        completedOrders = completedOrders.filter((item) =>
          item.orderNo.includes(searchOrderNo.value),
        );
      }

      orderList.value = completedOrders;
      total.value = completedOrders.length;
    } else {
      ElMessage.error(res.msg || "获取订单失败");
    }
  } catch (error) {
    console.error(error);
    ElMessage.error("网络错误");
  } finally {
    loading.value = false;
  }
};

/**
 * 新增：获取订单统计数据（对接后端接口）
 */
const fetchOrderStatistics = async () => {
  try {
    const res = await getOrderStatistics();
    if (res.code === 200) {
      // 渲染两个图表
      renderFaultTypeChart(res.data.faultTypeDistribution);
      renderOrderCountChart(res.data.last7DaysOrderCount);
    } else {
      ElMessage.error(res.msg || "获取统计数据失败");
    }
  } catch (error) {
    console.error("统计数据获取失败：", error);
    ElMessage.error("获取统计数据失败，请刷新重试");
  }
};

/**
 * 渲染故障类型分布饼图（接收后端统计数据）
 */
const renderFaultTypeChart = (faultTypeMap) => {
  // 初始化图表（确保只初始化一次）
  if (!faultTypeChart) {
    faultTypeChart = echarts.init(document.getElementById("faultTypeChart"));
    window.addEventListener("resize", () => {
      faultTypeChart?.resize();
    });
  }

  // 直接使用后端返回的统计数据
  const xAxisData = Object.keys(faultTypeMap || {});
  const seriesData = Object.values(faultTypeMap || {});

  // 图表配置项
  const option = {
    tooltip: {
      trigger: "item",
      formatter: "{b}: {c} 单 ({d}%)", // 显示名称、数量、占比
    },
    legend: {
      orient: "horizontal",
      bottom: 0,
      textStyle: {
        fontSize: 12,
      },
    },
    series: [
      {
        name: "故障类型",
        type: "pie", // 饼图
        radius: ["40%", "70%"], // 饼图大小
        center: ["50%", "40%"], // 饼图位置
        data: xAxisData.map((name, index) => ({
          name,
          value: seriesData[index],
        })),
        label: {
          show: true,
          formatter: "{b}: {c}", // 饼图标签显示名称和数量
        },
        // 自定义颜色
        color: [
          "#409EFF",
          "#67C23A",
          "#E6A23C",
          "#F56C6C",
          "#909399",
          "#722ED1",
          "#13C2C2",
          "#FA541C",
        ],
      },
    ],
  };

  // 设置配置项并渲染
  faultTypeChart.setOption(option);
};

/**
 * 渲染近7日订单数量柱状图（接收后端统计数据）
 */
const renderOrderCountChart = (last7DaysList) => {
  // 初始化图表（确保只初始化一次）
  if (!orderCountChart) {
    orderCountChart = echarts.init(document.getElementById("orderCountChart"));
    window.addEventListener("resize", () => {
      orderCountChart?.resize();
    });
  }

  // 解析后端返回的结构化数据
  const xAxisData = (last7DaysList || []).map((item) => item.date);
  const seriesData = (last7DaysList || []).map((item) => item.count);

  // 柱状图配置项
  const option = {
    tooltip: {
      trigger: "axis",
      formatter: "{b}：{c} 单", // 显示日期+订单数
      axisPointer: { type: "shadow" },
    },
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      top: "10%",
      containLabel: true,
    },
    xAxis: [
      {
        type: "category",
        data: xAxisData,
        axisLabel: { fontSize: 12 },
      },
    ],
    yAxis: [
      {
        type: "value",
        name: "订单数",
        min: 0,
        axisLabel: { formatter: "{value}" },
      },
    ],
    series: [
      {
        name: "订单数量",
        type: "bar",
        barWidth: "60%", // 柱子宽度
        data: seriesData,
        itemStyle: {
          color: "#409EFF", // 主色调，和Element Plus统一
          borderRadius: [4, 4, 0, 0], // 柱子顶部圆角
        },
        label: {
          show: true, // 柱子上显示数值
          position: "top",
          fontSize: 12,
        },
      },
    ],
  };

  // 设置配置项并渲染
  orderCountChart.setOption(option);
};

/**
 * 查询（同步刷新表格+统计图表）
 */
const handleSearch = () => {
  pageNum.value = 1;
  getOrderList(); // 刷新表格
  fetchOrderStatistics(); // 刷新图表
};

/**
 * 重置（同步刷新表格+统计图表）
 */
const handleReset = () => {
  searchOrderNo.value = "";
  getOrderList(); // 刷新表格
  fetchOrderStatistics(); // 刷新图表
};

/**
 * 分页大小变化
 */
const handleSizeChange = (val) => {
  pageSize.value = val;
  getOrderList();
};

/**
 * 页码变化
 */
const handleCurrentChange = (val) => {
  pageNum.value = val;
  getOrderList();
};

/**
 * 下载订单
 */
const exportOrderDetail = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要下载订单【${row.orderNo}】的详情吗？`,
      "下载确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    const res = await exportOrder(row.orderNo);

    const blob = new Blob([res], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });

    const url = window.URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = `订单详情_${row.orderNo}_${dayjs().format("YYYYMMDDHHmmss")}.xlsx`;

    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    window.URL.revokeObjectURL(url);

    ElMessage.success("下载成功");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("下载失败");
    }
  }
};

// 页面挂载时初始化（同时加载表格+统计数据）
onMounted(() => {
  getOrderList();
  fetchOrderStatistics(); // 新增：加载统计数据
});

// 组件卸载时销毁两个图表（防止内存泄漏）
onUnmounted(() => {
  if (faultTypeChart) {
    faultTypeChart.dispose();
    faultTypeChart = null;
  }
  if (orderCountChart) {
    orderCountChart.dispose();
    orderCountChart = null;
  }
});
</script>

<style scoped>
.supplier-home {
  padding: 20px;
  width: 100%;
}

.search-bar {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
}

/* 双图表容器样式（并列布局） */
.charts-wrapper {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap; /* 响应式：屏幕小时自动换行 */
}

.chart-card {
  flex: 1;
  min-width: 400px; /* 最小宽度，保证小屏幕下图表不挤 */
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}
</style>
