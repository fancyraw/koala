<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api'
import { yuan, formatTime } from '@/utils/format'
import { COUPON_STATE, COUPON_STATE_TABS, USER_COUPON_STATUS } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ state: undefined, page: 1, size: 12 })

async function load() {
  loading.value = true
  try {
    const res = await api.coupons({ state: query.state, page: query.page, size: query.size })
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}
function onStateChange(state) {
  query.state = state
  query.page = 1
  load()
}

function typeText(c) {
  return c.type === 2 ? '无门槛' : `满${yuan(c.minSpend)}可用`
}
function validityText(c) {
  if (c.validityType === 2) return `领取后 ${c.validDays} 天有效`
  return `${formatTime(c.validStartAt)} ~ ${formatTime(c.validEndAt)}`
}

async function stop(c) {
  await ElMessageBox.confirm(`停发「${c.name}」后不可恢复，确认？`, '提示', { type: 'warning' })
  await api.couponStop(c.id)
  ElMessage.success('已停发')
  load()
}
async function remove(c) {
  await ElMessageBox.confirm(`确认删除「${c.name}」？`, '提示', { type: 'warning' })
  await api.couponDelete(c.id)
  ElMessage.success('已删除')
  load()
}

// ---- 编辑弹窗 ----
const dialog = reactive({ visible: false, saving: false })
const formRef = ref(null)
const form = reactive({
  id: undefined,
  name: '',
  type: 1,
  discountAmount: 0,
  minSpend: 0,
  totalCount: 100,
  validityType: 1,
  range: [],
  validDays: 7,
})
const isEdit = computed(() => !!form.id)

const rules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  discountAmount: [{ required: true, message: '请输入优惠金额', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发行总量', trigger: 'blur' }],
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    name: '',
    type: 1,
    discountAmount: 0,
    minSpend: 0,
    totalCount: 100,
    validityType: 1,
    range: [],
    validDays: 7,
  })
  dialog.visible = true
}
async function openEdit(c) {
  const d = await api.couponDetail(c.id)
  Object.assign(form, {
    id: d.id,
    name: d.name,
    type: d.type,
    discountAmount: Number(d.discountAmount),
    minSpend: Number(d.minSpend || 0),
    totalCount: d.totalCount,
    validityType: d.validityType,
    range: d.validStartAt ? [d.validStartAt, d.validEndAt] : [],
    validDays: d.validDays || 7,
  })
  dialog.visible = true
}

async function submit() {
  await formRef.value.validate()
  const payload = {
    id: form.id,
    name: form.name,
    type: form.type,
    discountAmount: form.discountAmount,
    minSpend: form.type === 2 ? 0 : form.minSpend,
    totalCount: form.totalCount,
    validityType: form.validityType,
    validStartAt: form.validityType === 1 ? form.range[0] : null,
    validEndAt: form.validityType === 1 ? form.range[1] : null,
    validDays: form.validityType === 2 ? form.validDays : null,
  }
  if (form.validityType === 1 && (!form.range || form.range.length < 2))
    return ElMessage.warning('请选择有效期区间')
  dialog.saving = true
  try {
    await api.couponSave(payload)
    ElMessage.success('已保存')
    dialog.visible = false
    load()
  } finally {
    dialog.saving = false
  }
}

// ---- 发放明细抽屉 ----
const grant = reactive({ visible: false, loading: false, list: [], name: '' })
async function openGrants(c) {
  grant.visible = true
  grant.loading = true
  grant.name = c.name
  try {
    grant.list = await api.couponGrants({ id: c.id })
  } finally {
    grant.loading = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-radio-group :model-value="query.state" @change="onStateChange">
        <el-radio-button v-for="t in COUPON_STATE_TABS" :key="String(t.value)" :value="t.value">
          {{ t.label }}
        </el-radio-button>
      </el-radio-group>
      <div class="toolbar-spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新建优惠券</el-button>
    </div>

    <div v-loading="loading" class="coupon-grid">
      <div v-for="c in list" :key="c.id" class="coupon-card">
        <div class="coupon-left">
          <div class="coupon-amount">
            <span class="unit">¥</span>{{ Number(c.discountAmount) }}
          </div>
          <div class="coupon-cond">{{ typeText(c) }}</div>
        </div>
        <div class="coupon-right">
          <div class="coupon-head">
            <span class="coupon-name">{{ c.name }}</span>
            <el-tag :type="(COUPON_STATE[c.state] || {}).type" size="small">
              {{ (COUPON_STATE[c.state] || {}).label }}
            </el-tag>
          </div>
          <div class="coupon-validity">{{ validityText(c) }}</div>
          <div class="coupon-stats">
            <span>发行 {{ c.totalCount }}</span>
            <span>已领 {{ c.issuedCount }}</span>
            <span>已用 {{ c.usedCount }}</span>
          </div>
          <div class="coupon-ops">
            <span class="link-text" @click="openGrants(c)">发放明细</span>
            <span v-if="c.state === 'NOT_STARTED'" class="link-text" @click="openEdit(c)">编辑</span>
            <span v-if="c.state !== 'STOPPED' && c.state !== 'ENDED'" class="link-text danger" @click="stop(c)">停发</span>
            <span v-if="c.deletable" class="link-text danger" @click="remove(c)">删除</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="暂无优惠券" />
    </div>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="isEdit ? '编辑优惠券' : '新建优惠券'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="券类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">无门槛券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="使用门槛">
          <el-input-number v-model="form.minSpend" :min="0" :precision="2" :step="1" />
          <span class="hint">满多少元可用</span>
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" />
        </el-form-item>
        <el-form-item label="有效期类型">
          <el-radio-group v-model="form.validityType">
            <el-radio :value="1">固定区间</el-radio>
            <el-radio :value="2">领取后 N 天</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.validityType === 1" label="有效期">
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item v-else label="有效天数">
          <el-input-number v-model="form.validDays" :min="1" />
          <span class="hint">天</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发放明细 -->
    <el-drawer v-model="grant.visible" :title="`发放明细 - ${grant.name}`" size="560px">
      <el-table v-loading="grant.loading" :data="grant.list" style="width: 100%">
        <el-table-column label="用户" prop="nickname" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="(USER_COUPON_STATUS[row.status] || {}).type" size="small">
              {{ (USER_COUPON_STATUS[row.status] || {}).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="领取时间" width="160">
          <template #default="{ row }">{{ formatTime(row.grantedAt) }}</template>
        </el-table-column>
        <el-table-column label="过期时间" width="160">
          <template #default="{ row }">{{ formatTime(row.expireAt) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar-spacer {
  flex: 1;
}
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
  min-height: 120px;
}
.coupon-card {
  display: flex;
  background: #fff;
  border: 1px solid $border;
  border-radius: $radius-card;
  overflow: hidden;
}
.coupon-left {
  width: 130px;
  flex-shrink: 0;
  background: $brand-light;
  color: $brand;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.coupon-amount {
  font-size: 32px;
  font-weight: 700;
  .unit {
    font-size: 16px;
  }
}
.coupon-cond {
  font-size: 12px;
}
.coupon-right {
  flex: 1;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.coupon-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.coupon-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-title;
}
.coupon-validity {
  font-size: 12px;
  color: $text-weak;
}
.coupon-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: $text-body;
}
.coupon-ops {
  display: flex;
  gap: 14px;
  margin-top: auto;
}
.link-text.danger {
  color: $danger;
}
.hint {
  margin-left: 12px;
  color: $text-weak;
  font-size: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
