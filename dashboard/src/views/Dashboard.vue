<script setup>
import { ref, onMounted, onActivated, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { api } from '@/api'
import { yuan, toYuan } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const range = ref(7)
const today = ref({})
const pending = ref({})
const salesTrend = ref([])
const hotProducts = ref([])

const chartRef = ref(null)
let chart = null

async function load() {
  loading.value = true
  try {
    const res = await api.dashboard(range.value)
    today.value = res.today || {}
    pending.value = res.pending || {}
    salesTrend.value = res.salesTrend || []
    hotProducts.value = res.hotProducts || []
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const dates = salesTrend.value.map((p) => p.date)
  const values = salesTrend.value.map((p) => Number(p.amount || 0))
  chart.setOption({
    grid: { top: 30, right: 20, bottom: 30, left: 60 },
    tooltip: {
      trigger: 'axis',
      formatter: (ps) => `${ps[0].axisValue}<br/>销售额：${yuan(ps[0].data)}`,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisLabel: { color: '#8c8c8c' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#8c8c8c' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [
      {
        type: 'bar',
        data: values,
        barMaxWidth: 32,
        itemStyle: { color: '#1677ff', borderRadius: [4, 4, 0, 0] },
        label: {
          show: true,
          position: 'top',
          color: '#8c8c8c',
          fontSize: 11,
          formatter: (p) => (p.data > 0 ? toYuan(p.data) : ''),
        },
      },
    ],
  })
  chart.resize()
}

function onResize() {
  chart && chart.resize()
}

function growth(rate) {
  if (rate === null || rate === undefined) return { text: '—', cls: 'flat' }
  const n = Number(rate)
  if (n > 0) return { text: `↑ ${n}%`, cls: 'up' }
  if (n < 0) return { text: `↓ ${Math.abs(n)}%`, cls: 'down' }
  return { text: '0%', cls: 'flat' }
}

function goShip() {
  router.push({ path: '/orders', query: { status: 1 } })
}
function goAfterSale() {
  router.push({ path: '/orders', query: { tab: 'aftersale' } })
}
function goProduct(name) {
  router.push({ path: '/products', query: { keyword: name } })
}

watch(range, load)
onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})
onActivated(() => {
  chart && chart.resize()
})
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <!-- 今日概览 -->
    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">今日销售额</div>
        <div class="kpi-value">{{ yuan(today.salesAmount) }}</div>
        <div class="kpi-growth" :class="growth(today.salesGrowthRate).cls">
          较昨日 {{ growth(today.salesGrowthRate).text }}
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">今日订单</div>
        <div class="kpi-value">{{ today.orderCount || 0 }}</div>
        <div class="kpi-growth" :class="growth(today.orderGrowthRate).cls">
          较昨日 {{ growth(today.orderGrowthRate).text }}
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">今日新增用户</div>
        <div class="kpi-value">{{ today.newUserCount || 0 }}</div>
        <div class="kpi-growth" :class="growth(today.userGrowthRate).cls">
          较昨日 {{ growth(today.userGrowthRate).text }}
        </div>
      </div>
    </div>

    <!-- 待处理事项 -->
    <div class="card pending-card">
      <div class="card-title">待处理事项</div>
      <div class="pending-row">
        <div class="pending-item" @click="goShip">
          <span class="pending-label">待发货订单</span>
          <span class="pending-num" :class="{ hot: pending.toShip > 0 }">{{ pending.toShip || 0 }}</span>
        </div>
        <div class="pending-item" @click="goAfterSale">
          <span class="pending-label">退款 / 售后</span>
          <span class="pending-num" :class="{ hot: pending.afterSale > 0 }">{{ pending.afterSale || 0 }}</span>
        </div>
      </div>
    </div>

    <!-- 销售趋势 -->
    <div class="card">
      <div class="card-head">
        <div class="card-title">销售趋势</div>
        <el-radio-group v-model="range" size="small">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="chartRef" class="chart"></div>
    </div>

    <!-- 热销 Top5 -->
    <div class="card">
      <div class="card-title">热销商品 Top5</div>
      <el-table :data="hotProducts" style="width: 100%">
        <el-table-column label="排名" width="80">
          <template #default="{ $index }">
            <span class="rank" :class="{ top: $index < 3 }">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品名称" min-width="240">
          <template #default="{ row }">
            <span class="link-text" @click="goProduct(row.productName)">{{ row.productName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销量" prop="quantity" width="120" align="right" />
        <el-table-column label="销售额" width="160" align="right">
          <template #default="{ row }">{{ yuan(row.amount) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kpi-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.kpi-card {
  background: #fff;
  border-radius: $radius-card;
  padding: 20px 24px;
  border: 1px solid $border;
}
.kpi-label {
  font-size: 14px;
  color: $text-weak;
}
.kpi-value {
  margin: 10px 0 8px;
  font-size: 30px;
  font-weight: 700;
  color: $text-title;
}
.kpi-growth {
  font-size: 13px;
  &.up {
    color: $success;
  }
  &.down {
    color: $danger;
  }
  &.flat {
    color: $text-weak;
  }
}
.card {
  background: #fff;
  border-radius: $radius-card;
  padding: 20px 24px;
  border: 1px solid $border;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-title;
  margin-bottom: 16px;
}
.card-head .card-title {
  margin-bottom: 0;
}
.pending-row {
  display: flex;
  gap: 40px;
}
.pending-item {
  display: flex;
  align-items: baseline;
  gap: 12px;
  cursor: pointer;
  &:hover .pending-label {
    color: $brand;
  }
}
.pending-label {
  font-size: 14px;
  color: $text-body;
}
.pending-num {
  font-size: 24px;
  font-weight: 700;
  color: $text-title;
  &.hot {
    color: $danger;
  }
}
.chart {
  height: 320px;
}
.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: $table-head-bg;
  color: $text-body;
  font-size: 13px;
  &.top {
    background: $brand;
    color: #fff;
  }
}
</style>
