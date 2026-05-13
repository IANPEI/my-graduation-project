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

    <!-- 服务商好评率统计 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>服务商好评率统计</span>
        </div>
      </template>
      <el-table :data="supplierEvaluateList" border style="width: 100%">
        <el-table-column prop="supplierId" label="服务商ID" width="120" />
        <el-table-column prop="name" label="服务商名称" />
        <el-table-column prop="totalCount" label="总评价数" width="100" />
        <el-table-column prop="goodCount" label="好评数" width="100" />
        <el-table-column prop="goodRate" label="好评率" width="130">
          <template #default="scope">
            <el-tag type="success" v-if="scope.row.goodRate >= 80">
              {{ scope.row.goodRate.toFixed(2) }}%
            </el-tag>
            <el-tag type="warning" v-else-if="scope.row.goodRate >= 60">
              {{ scope.row.goodRate.toFixed(2) }}%
            </el-tag>
            <el-tag type="danger" v-else>
              {{ scope.row.goodRate.toFixed(2) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="avgScore" label="平均评分" width="120">
          <template #default="scope">
            {{ scope.row.avgScore ? scope.row.avgScore.toFixed(1) : "0.0" }} ⭐
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="evaluatePage.currentPage"
        v-model:page-size="evaluatePage.pageSize"
        :total="evaluatePage.total"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; text-align: right"
        @size-change="loadSupplierEvaluate"
        @current-change="loadSupplierEvaluate"
      />
    </el-card>

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
        <el-table-column
          prop="businessLicenseNo"
          label="营业执照编号"
          min-width="160"
        />
        <el-table-column
          prop="rescueQualificationNo"
          label="救援资质编号"
          min-width="160"
        />
        <el-table-column prop="createTime" label="申请时间">
          <template #default="scope">
            {{ scope.row.createTime ? formatDate(scope.row.createTime) : "-" }}
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="scope">
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

        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              plain
              @click="openEditDialog(scope.row)"
              v-if="scope.row.status === 'pending'"
            >
              编辑审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 👇 完全统一风格的审核弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="服务商入驻审核"
      width="750px"
      @close="closeDialog"
    >
      <el-form v-if="currentSupplier">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="服务商ID">{{
            currentSupplier.id
          }}</el-descriptions-item>
          <el-descriptions-item label="服务商名称">{{
            currentSupplier.name
          }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{
            currentSupplier.contact
          }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{
            currentSupplier.phone
          }}</el-descriptions-item>
          <el-descriptions-item label="所在城市">{{
            currentSupplier.city
          }}</el-descriptions-item>
          <el-descriptions-item label="详细地址">{{
            currentSupplier.address
          }}</el-descriptions-item>
          <el-descriptions-item label="营业执照编号">
            {{ currentSupplier.businessLicenseNo }}
          </el-descriptions-item>
          <el-descriptions-item label="救援资质编号">
            {{ currentSupplier.rescueQualificationNo }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">
            {{ formatDate(currentSupplier.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px">
          <el-radio-group v-model="auditResult" style="margin-bottom: 15px">
            <el-radio label="enable">审核通过</el-radio>
            <el-radio label="disable">审核拒绝</el-radio>
          </el-radio-group>

          <el-input
            v-if="auditResult === 'disable'"
            v-model="refuseReason"
            type="textarea"
            rows="3"
            placeholder="请输入拒绝理由，将短信通知服务商"
          />
        </div>
      </el-form>

      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submitAudit">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- 订单趋势 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <span>近7天订单趋势</span>
      </template>
      <div id="orderTrendChart" style="width: 100%; height: 300px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import * as echarts from "echarts";
import {
  getAdminHomeData,
  approveSupplier,
  getSupplierEvaluateList,
} from "@/api/admin";

const router = useRouter();

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
  orderTrendData: {
    dates: ["7天前", "6天前", "5天前", "4天前", "3天前", "2天前", "1天前"],
    counts: [120, 200, 150, 80, 70, 110, 130],
  },
});

const formatChartDate = (dateStr) => {
  if (!dateStr) return "";
  return dateStr.split("-").slice(1).join("-");
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, "0")}-${date.getDate().toString().padStart(2, "0")} ${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`;
};

// ====================== 审核弹窗逻辑 ======================
const dialogVisible = ref(false);
const currentSupplier = ref(null);
const auditResult = ref("enable");
const refuseReason = ref("");

const openEditDialog = (row) => {
  currentSupplier.value = row;
  auditResult.value = "enable";
  refuseReason.value = "";
  dialogVisible.value = true;
};

const closeDialog = () => {
  dialogVisible.value = false;
  currentSupplier.value = null;
  refuseReason.value = "";
};

const submitAudit = async () => {
  if (!currentSupplier.value) return;
  const id = currentSupplier.value.id;
  const status = auditResult.value;

  try {
    if (status === "enable") {
      await ElMessageBox.confirm("确认通过该服务商入驻申请？", "提示");
    } else {
      if (!refuseReason.value) {
        ElMessage.warning("请填写拒绝理由");
        return;
      }
      await ElMessageBox.confirm("确认拒绝？", "提示");
    }

    const res = await approveSupplier(id, status, refuseReason.value);
    if (res.code === 200) {
      ElMessage.success("操作成功");
      closeDialog();

      // 延迟刷新，保证后台修改完成
      setTimeout(() => {
        loadHomeData();
      }, 300);

      loadSupplierEvaluate();
    } else {
      ElMessage.error(res.msg);
    }
  } catch (e) {
    ElMessage.info("已取消");
  }
};

const supplierEvaluateList = ref([]);
const evaluatePage = reactive({
  currentPage: 1,
  pageSize: 5,
  total: 0,
});

const loadSupplierEvaluate = async () => {
  try {
    const res = await getSupplierEvaluateList({
      pageNum: evaluatePage.currentPage,
      pageSize: evaluatePage.pageSize,
    });
    if (res.code === 200) {
      supplierEvaluateList.value = res.data.records || [];
      evaluatePage.total = res.data.total || 0;
    }
  } catch (e) {}
};

const loadHomeData = async () => {
  const res = await getAdminHomeData();
  if (res.code === 200) {
    Object.assign(homeData, res.data);
  }
};

const initOrderChart = () => {
  const chartDom = document.getElementById("orderTrendChart");
  if (!chartDom) return;
  const myChart = echarts.init(chartDom);
  const option = {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: homeData.orderTrendData.dates },
    yAxis: { type: "value" },
    series: [
      { name: "订单", type: "bar", data: homeData.orderTrendData.counts },
    ],
  };
  myChart.setOption(option);
  window.addEventListener("resize", () => myChart.resize());
};

onMounted(async () => {
  await loadHomeData();
  initOrderChart();
  loadSupplierEvaluate();
});

const goToSupplier = () => {
  router.push("/admin/supplier");
};
</script>

<style scoped>
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
:deep(.el-card__body) {
  padding: 20px;
}
</style>
