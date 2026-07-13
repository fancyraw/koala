<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { yuan, formatTime } from '@/utils/format'
import { ORDER_STATUS, ORDER_TABS, AFTER_SALE_STATUS, AFTER_SALE_TYPE } from '@/utils/dict'

const route = useRoute()

const tab = ref(route.query.tab === 'aftersale' ? 'aftersale' : 'order')

// ---- 订单列表 ----
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  keyword: '',
  status: route.query.status !== undefined ? Number(route.query.status) : undefined,
  page: 1,
  size: 20,
})

async function load() {
  loading.value = true
  try {
    const res = await api.orders({
      keyword: query.keyword || undefined,
      status: query.status,
      page: query.page,
      size: query.size,
    })
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}
function search() {
  query.page = 1
  load()
}
function onTabChange(status) {
  query.status = status
  search()
}

// ---- 详情抽屉 ----
const detail = reactive({ visible: false, loading: false, data: {} })
async function openDetail(no) {
  detail.visible = true
  detail.loading = true
  try {
    detail.data = await api.orderDetail(no)
  } finally {
    detail.loading = false
  }
}

// ---- 发货 ----
const ship = reactive({ visible: false, saving: false, no: '', logisticsCompany: '', logisticsNo: '' })
function openShip(row) {
  Object.assign(ship, { visible: true, saving: false, no: row.orderNo, logisticsCompany: '', logisticsNo: '' })
}
async function submitShip() {
  if (!ship.logisticsCompany.trim() || !ship.logisticsNo.trim())
    return ElMessage.warning('请填写物流公司和单号')
  ship.saving = true
  try {
    await api.orderShip({ no: ship.no, logisticsCompany: ship.logisticsCompany, logisticsNo: ship.logisticsNo })
    ElMessage.success('已发货')
    ship.visible = false
    load()
  } finally {
    ship.saving = false
  }
}

// ---- 退款 ----
const refund = reactive({ visible: false, saving: false, no: '', reason: '' })
function openRefund(row) {
  Object.assign(refund, { visible: true, saving: false, no: row.orderNo, reason: '' })
}
async function submitRefund() {
  refund.saving = true
  try {
    await api.orderRefund({ no: refund.no, reason: refund.reason })
    ElMessage.success('已发起退款')
    refund.visible = false
    load()
  } finally {
    refund.saving = false
  }
}

// ---- 售后列表 ----
const asLoading = ref(false)
const asList = ref([])
const asTotal = ref(0)
const asQuery = reactive({ keyword: '', status: undefined, page: 1, size: 20 })

async function loadAS() {
  asLoading.value = true
  try {
    const res = await api.afterSales({
      keyword: asQuery.keyword || undefined,
      status: asQuery.status,
      page: asQuery.page,
      size: asQuery.size,
    })
    asList.value = res.list || []
    asTotal.value = res.total || 0
  } finally {
    asLoading.value = false
  }
}
function searchAS() {
  asQuery.page = 1
  loadAS()
}

const asDetail = reactive({ visible: false, loading: false, data: {} })
async function openASDetail(no) {
  asDetail.visible = true
  asDetail.loading = true
  try {
    asDetail.data = await api.afterSaleDetail(no)
  } finally {
    asDetail.loading = false
  }
}

async function audit(row, approved) {
  let auditRemark = ''
  if (!approved) {
    const r = await ElMessageBox.prompt('请填写拒绝理由', '拒绝售后', {
      inputValidator: (v) => (v && v.trim() ? true : '理由不能为空'),
    })
    auditRemark = r.value
  } else {
    await ElMessageBox.confirm('确认同意该售后申请？', '提示', { type: 'warning' })
  }
  await api.afterSaleAudit({ afterSaleNo: row.afterSaleNo, approved, auditRemark })
  ElMessage.success('已处理')
  loadAS()
}

async function confirmReceive(row) {
  await ElMessageBox.confirm('确认已收到退货并退款？', '提示', { type: 'warning' })
  await api.afterSaleConfirmReceive(row.afterSaleNo)
  ElMessage.success('已退款')
  loadAS()
}

function onMainTab(name) {
  if (name === 'aftersale' && !asList.value.length) loadAS()
}

onMounted(() => {
  load()
  if (tab.value === 'aftersale') loadAS()
})
</script>

<template>
  <el-tabs v-model="tab" @tab-change="onMainTab">
    <!-- 订单 -->
    <el-tab-pane label="订单" name="order">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="订单号 / 买家昵称 / 收货人"
          clearable
          style="width: 260px"
          @keyup.enter="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <div class="card">
        <el-radio-group
          :model-value="query.status"
          class="status-filter"
          @change="onTabChange"
        >
          <el-radio-button v-for="t in ORDER_TABS" :key="String(t.value)" :value="t.value">
            {{ t.label }}
          </el-radio-button>
        </el-radio-group>

        <el-table v-loading="loading" :data="list" style="width: 100%" row-key="orderNo">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="items">
                <div v-for="(it, i) in row.items" :key="i" class="item">
                  <el-image :src="it.productImage" fit="cover" class="item-img" />
                  <div class="item-info">
                    <div>{{ it.productName }}</div>
                    <div class="item-sku">{{ it.skuName }}</div>
                  </div>
                  <div class="item-qty">{{ yuan(it.unitPrice) }} × {{ it.quantity }}</div>
                  <div class="item-sub">{{ yuan(it.subtotal) }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="订单号" prop="orderNo" width="180" />
          <el-table-column label="买家" prop="nickname" width="120" />
          <el-table-column label="收货人" width="140">
            <template #default="{ row }">
              <div>{{ row.receiverName }}</div>
              <div class="sub">{{ row.receiverPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="实付" width="110" align="right">
            <template #default="{ row }">{{ yuan(row.payAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="(ORDER_STATUS[row.status] || {}).type" size="small">
                {{ (ORDER_STATUS[row.status] || {}).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <div class="op-btns">
                <span class="link-text" @click="openDetail(row.orderNo)">详情</span>
                <span v-if="row.status === 1" class="link-text" @click="openShip(row)">发货</span>
                <span v-if="row.status === 3" class="link-text danger" @click="openRefund(row)">退款</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="load"
            @size-change="search"
          />
        </div>
      </div>
    </el-tab-pane>

    <!-- 售后 -->
    <el-tab-pane label="售后" name="aftersale">
      <div class="toolbar">
        <el-input
          v-model="asQuery.keyword"
          placeholder="售后单号 / 订单号 / 买家昵称"
          clearable
          style="width: 260px"
          @keyup.enter="searchAS"
        />
        <el-select v-model="asQuery.status" placeholder="全部状态" clearable style="width: 150px" @change="searchAS">
          <el-option v-for="(v, k) in AFTER_SALE_STATUS" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
        <el-button type="primary" @click="searchAS">查询</el-button>
      </div>

      <div class="card">
        <el-table v-loading="asLoading" :data="asList" style="width: 100%">
          <el-table-column label="售后单号" prop="afterSaleNo" width="180" />
          <el-table-column label="订单号" prop="orderNo" width="180" />
          <el-table-column label="买家" prop="nickname" width="110" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ AFTER_SALE_TYPE[row.type] }}</template>
          </el-table-column>
          <el-table-column label="退款额" width="110" align="right">
            <template #default="{ row }">{{ yuan(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="(AFTER_SALE_STATUS[row.status] || {}).type" size="small">
                {{ (AFTER_SALE_STATUS[row.status] || {}).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <div class="op-btns">
                <span class="link-text" @click="openASDetail(row.afterSaleNo)">详情</span>
                <template v-if="row.status === 0">
                  <span class="link-text" @click="audit(row, true)">同意</span>
                  <span class="link-text danger" @click="audit(row, false)">拒绝</span>
                </template>
                <span v-if="row.status === 2" class="link-text" @click="confirmReceive(row)">确认收货</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="asQuery.page"
            v-model:page-size="asQuery.size"
            :total="asTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadAS"
            @size-change="searchAS"
          />
        </div>
      </div>
    </el-tab-pane>
  </el-tabs>

  <!-- 订单详情 -->
  <el-drawer v-model="detail.visible" title="订单详情" size="520px">
    <div v-loading="detail.loading">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ detail.data.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(ORDER_STATUS[detail.data.status] || {}).type" size="small">
            {{ (ORDER_STATUS[detail.data.status] || {}).label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="买家">{{ detail.data.nickname }}</el-descriptions-item>
        <el-descriptions-item label="收货信息">
          {{ detail.data.receiverName }} {{ detail.data.receiverPhone }}<br />
          {{ detail.data.receiverAddress }}
        </el-descriptions-item>
        <el-descriptions-item label="商品金额">{{ yuan(detail.data.productAmount) }}</el-descriptions-item>
        <el-descriptions-item label="优惠">-{{ yuan(detail.data.couponDiscount) }}</el-descriptions-item>
        <el-descriptions-item label="运费">{{ yuan(detail.data.shippingFee) }}</el-descriptions-item>
        <el-descriptions-item label="实付">{{ yuan(detail.data.payAmount) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.data.logisticsNo" label="物流">
          {{ detail.data.logisticsCompany }} {{ detail.data.logisticsNo }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.data.remark" label="备注">{{ detail.data.remark }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(detail.data.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <div class="items detail-items">
        <div v-for="(it, i) in detail.data.items" :key="i" class="item">
          <el-image :src="it.productImage" fit="cover" class="item-img" />
          <div class="item-info">
            <div>{{ it.productName }}</div>
            <div class="item-sku">{{ it.skuName }}</div>
          </div>
          <div class="item-qty">{{ yuan(it.unitPrice) }} × {{ it.quantity }}</div>
          <div class="item-sub">{{ yuan(it.subtotal) }}</div>
        </div>
      </div>
    </div>
  </el-drawer>

  <!-- 售后详情 -->
  <el-drawer v-model="asDetail.visible" title="售后详情" size="520px">
    <div v-loading="asDetail.loading">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="售后单号">{{ asDetail.data.afterSaleNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ asDetail.data.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ asDetail.data.nickname }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ AFTER_SALE_TYPE[asDetail.data.type] }}</el-descriptions-item>
        <el-descriptions-item label="退款额">{{ yuan(asDetail.data.refundAmount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(AFTER_SALE_STATUS[asDetail.data.status] || {}).type" size="small">
            {{ (AFTER_SALE_STATUS[asDetail.data.status] || {}).label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="原因">{{ asDetail.data.reason }}</el-descriptions-item>
        <el-descriptions-item v-if="asDetail.data.remark" label="买家备注">{{ asDetail.data.remark }}</el-descriptions-item>
        <el-descriptions-item v-if="asDetail.data.returnTrackingNo" label="退货单号">{{ asDetail.data.returnTrackingNo }}</el-descriptions-item>
        <el-descriptions-item v-if="asDetail.data.auditRemark" label="审核备注">{{ asDetail.data.auditRemark }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="asDetail.data.evidenceImages && asDetail.data.evidenceImages.length" class="evidence">
        <el-image
          v-for="(img, i) in asDetail.data.evidenceImages"
          :key="i"
          :src="img"
          fit="cover"
          class="evidence-img"
          :preview-src-list="asDetail.data.evidenceImages"
          :initial-index="i"
        />
      </div>
    </div>
  </el-drawer>

  <!-- 发货弹窗 -->
  <el-dialog v-model="ship.visible" title="发货" width="440px">
    <el-form label-width="88px">
      <el-form-item label="订单号">{{ ship.no }}</el-form-item>
      <el-form-item label="物流公司" required>
        <el-input v-model="ship.logisticsCompany" placeholder="如 顺丰速运" />
      </el-form-item>
      <el-form-item label="物流单号" required>
        <el-input v-model="ship.logisticsNo" placeholder="快递单号" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="ship.visible = false">取消</el-button>
      <el-button type="primary" :loading="ship.saving" @click="submitShip">确认发货</el-button>
    </template>
  </el-dialog>

  <!-- 退款弹窗 -->
  <el-dialog v-model="refund.visible" title="发起退款" width="440px">
    <el-form label-width="88px">
      <el-form-item label="订单号">{{ refund.no }}</el-form-item>
      <el-form-item label="退款原因">
        <el-input v-model="refund.reason" type="textarea" :rows="3" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="refund.visible = false">取消</el-button>
      <el-button type="primary" :loading="refund.saving" @click="submitRefund">确认退款</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.card {
  background: #fff;
  border-radius: $radius-card;
  padding: 16px 20px;
  border: 1px solid $border;
}
.status-filter {
  margin-bottom: 16px;
}
.sub {
  font-size: 12px;
  color: $text-weak;
}
.link-text.danger {
  color: $danger;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.items {
  padding: 8px 16px;
}
.detail-items {
  margin-top: 16px;
  border: 1px solid $border;
  border-radius: $radius-control;
}
.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  & + .item {
    border-top: 1px solid $border;
  }
}
.item-img {
  width: 44px;
  height: 44px;
  border-radius: $radius-control;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
  font-size: 13px;
}
.item-sku {
  color: $text-weak;
  font-size: 12px;
}
.item-qty {
  color: $text-body;
  font-size: 13px;
}
.item-sub {
  width: 90px;
  text-align: right;
  color: $text-title;
}
.evidence {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.evidence-img {
  width: 88px;
  height: 88px;
  border-radius: $radius-control;
}
</style>
