<template>
  <div class="admin-home">
    <h2 style="margin-bottom: 20px; color: #333">管理员工作台</h2>

    <!-- 平台数据统计 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-title">已入驻服务商</div>
          <div class="stat-value">{{ homeData.supplierCount }}</div>
          <div class="stat-desc">
            本月新增 {{ homeData.monthNewSupplierCount }} 家
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-title">全平台订单数</div>
          <div class="stat-value">{{ homeData.totalOrderCount }}</div>
          <div class="stat-desc">
            今日新增 {{ homeData.todayNewOrderCount }} 单
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-title">救援人员总数</div>
          <div class="stat-value">{{ homeData.staffTotalCount }}</div>
          <div class="stat-desc">在线 {{ homeData.onlineStaffCount }} 人</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-title">注册车主用户</div>
          <div class="stat-value">{{ homeData.userCount }}</div>
          <div class="stat-desc">
            本月新增 {{ homeData.monthNewUserCount }} 人
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 服务商入驻申请 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>最新服务商入驻申请</span>
          <el-button type="text" @click="goToSupplier">查看全部</el-button>
        </div>
      </template>
      <el-table :data="homeData.supplierApplyList" border style="width: 100%">
        <el-table-column prop="name" label="服务商名称" />
        <el-table-column prop="contact" label="联系人" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="createTime" label="申请时间">
          <template #default="scope">
            {{ scope.row.createTime ? formatDate(scope.row.createTime) : "-" }}
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="scope">
            <!-- 待审核显示红色，已通过显示绿色，已拒绝显示灰色 -->
            <el-tag
              :type="
                scope.row.status === 'pending'
                  ? 'danger'
                  : scope.row.status === 'enable'
                    ? 'success'
                    : 'info'
              "
            >
              {{
                scope.row.status === "pending"
                  ? "待审核"
                  : scope.row.status === "enable"
                    ? "已通过"
                    : "已拒绝"
              }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 新增：审核操作列 -->
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <!-- 只有待审核才显示审核按钮 -->
            <el-button
              type="success"
              size="small"
              plain
              @click="handleApproveSupplier(scope.row.id, 'enable')"
              v-if="scope.row.status === 'pending'"
            >
              通过
            </el-button>
            <el-button
              type="info"
              size="small"
              plain
              @click="handleApproveSupplier(scope.row.id, 'disable')"
              v-if="scope.row.status === 'pending'"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 订单趋势 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <span>近7天订单趋势</span>
      </template>
      <!-- ECharts 容器 -->
      <div id="orderTrendChart" style="width: 100%; height: 300px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
// 引入 ECharts
import * as echarts from "echarts";
import { getAdminHomeData, approveSupplier } from "@/api/admin";

const router = useRouter();

// 工作台数据（从后端获取）
const homeData = reactive({
  supplierCount: 0,
  monthNewSupplierCount: 0,
  totalOrderCount: 0,
  todayNewOrderCount: 0,
  staffTotalCount: 0,
  onlineStaffCount: 0,
  userCount: 0,
  monthNewUserCount: 0,
  supplierApplyList: [],
  // 新增订单趋势数据（默认模拟数据，可替换为后端返回数据）
  orderTrendData: {
    dates: ["7天前", "6天前", "5天前", "4天前", "3天前", "2天前", "1天前"],
    counts: [120, 200, 150, 80, 70, 110, 130],
  },
});

// 格式化日期（YYYY-MM-DD → MM-DD）
const formatChartDate = (dateStr) => {
  if (!dateStr) return "";
  return dateStr.split("-").slice(1).join("-"); // 截取后两位：2026-03-11 → 03-11
};

// 格式化时间（用于申请时间显示）
const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  // 兼容不同时间格式，保留时分秒
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, "0")}-${date.getDate().toString().padStart(2, "0")} ${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
};

// 服务商入驻审核（通过/拒绝）
const handleApproveSupplier = async (id, status) => {
  try {
    const res = await approveSupplier(id, status);
    if (res.code === 200) {
      ElMessage.success(
        status === "enable" ? "审核通过成功" : "已拒绝该服务商申请",
      );

      // 👇 加固：先获取数据，再渲染图表
      const newRes = await getAdminHomeData();
      if (newRes.code === 200) {
        Object.assign(homeData, newRes.data);
      }

      // 延迟一点确保数据更新
      setTimeout(() => {
        initOrderChart();
      }, 200);
    } else {
      ElMessage.error(res.msg);
    }
  } catch (error) {
    ElMessage.error("审核操作失败");
    console.error(error);
  }
};

// 初始化 ECharts 图表
const initOrderChart = () => {
  // 获取图表容器
  const chartDom = document.getElementById("orderTrendChart");
  if (!chartDom) return;

  // 初始化图表实例
  const myChart = echarts.init(chartDom);

  // 处理后端返回的日期，优化显示
  const chartDates =
    homeData.orderTrendData?.dates?.map((item) => formatChartDate(item)) ||
    homeData.orderTrendData.dates;
  // 兼容空数据
  const chartCounts = homeData.orderTrendData?.counts || [0, 0, 0, 0, 0, 0, 0];

  // 图表配置项
  const option = {
    // 提示框
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "shadow",
      },
      textStyle: {
        fontSize: 12,
      },
      // 提示框显示完整日期
      formatter: (params) => {
        const originDate = homeData.orderTrendData.dates[params[0].dataIndex];
        return `${originDate}<br/>订单数：${params[0].value}`;
      },
    },
    // 图例
    legend: {
      data: ["订单数"],
      top: 0,
    },
    // 网格
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      containLabel: true,
    },
    // x轴
    xAxis: {
      type: "category",
      data: chartDates, // 使用格式化后的日期
      axisLabel: {
        fontSize: 12,
      },
    },
    // y轴
    yAxis: {
      type: "value",
      name: "订单数",
      min: 0,
      axisLabel: {
        fontSize: 12,
      },
    },
    // 系列数据
    series: [
      {
        name: "订单数",
        type: "bar",
        data: chartCounts, // 兼容空数据
        // 柱子样式
        itemStyle: {
          color: "#409EFF",
        },
        // 柱子宽度
        barWidth: "60%",
        // 显示数值
        label: {
          show: true,
          position: "top",
          fontSize: 11,
        },
      },
    ],
  };

  // 设置配置项并渲染
  myChart.setOption(option);

  // 监听窗口大小变化，自适应图表
  window.addEventListener("resize", () => {
    myChart.resize();
  });
};

// 页面加载时获取数据并初始化图表
onMounted(async () => {
  try {
    const res = await getAdminHomeData();
    if (res.code === 200) {
      // 合并后端返回数据（自动覆盖模拟数据）
      Object.assign(homeData, res.data);
    } else {
      ElMessage.error("获取工作台数据失败：" + res.msg);
    }
  } catch (error) {
    ElMessage.error("网络错误，获取工作台数据失败");
    console.error("获取管理员工作台数据失败：", error);
  } finally {
    // 无论是否获取数据成功，都初始化图表
    initOrderChart();
  }
});

// 跳转到服务商管理页
const goToSupplier = () => {
  router.push("/admin/supplier");
};
</script>

<style scoped>
/* 补充统计卡片样式，让数据展示更美观 */
.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-desc {
  font-size: 12px;
  color: #999;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-home {
  padding: 0;
}

/* 解决 ECharts 容器样式问题 */
:deep(.el-card__body) {
  padding: 20px;
}
</style>
