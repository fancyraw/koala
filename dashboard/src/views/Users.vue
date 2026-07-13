<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { yuan, formatTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: undefined, page: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    const res = await api.users({
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

async function toggleStatus(row) {
  const next = row.isValid === 1 ? 0 : 1
  const word = next === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${word}用户「${row.nickname}」？`, '提示', { type: 'warning' })
  await api.userStatus(row.id, next)
  row.isValid = next
  ElMessage.success(`已${word}`)
}

const detail = reactive({ visible: false, loading: false, data: {} })
async function openDetail(row) {
  detail.visible = true
  detail.loading = true
  try {
    detail.data = await api.userDetail(row.id)
  } finally {
    detail.loading = false
  }
}
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="昵称"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
        <el-option label="正常" :value="1" />
        <el-option label="已禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="row.avatarUrl">{{ (row.nickname || '?').slice(0, 1) }}</el-avatar>
              <span class="user-name">{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="ID" prop="id" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.isValid === 1 ? 'success' : 'danger'" size="small">
              {{ row.isValid === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span class="link-text" @click="openDetail(row)">详情</span>
              <span class="link-text" :class="{ danger: row.isValid === 1 }" @click="toggleStatus(row)">
                {{ row.isValid === 1 ? '禁用' : '启用' }}
              </span>
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

    <el-dialog v-model="detail.visible" title="用户详情" width="460px">
      <div v-loading="detail.loading" class="detail">
        <div class="detail-head">
          <el-avatar :size="56" :src="detail.data.avatarUrl">
            {{ (detail.data.nickname || '?').slice(0, 1) }}
          </el-avatar>
          <div>
            <div class="detail-name">{{ detail.data.nickname }}</div>
            <el-tag :type="detail.data.isValid === 1 ? 'success' : 'danger'" size="small">
              {{ detail.data.isValid === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </div>
        </div>
        <div class="stat-row">
          <div class="stat">
            <div class="stat-num">{{ detail.data.paidOrderCount || 0 }}</div>
            <div class="stat-label">已支付订单</div>
          </div>
          <div class="stat">
            <div class="stat-num">{{ yuan(detail.data.totalPaidAmount) }}</div>
            <div class="stat-label">累计实付</div>
          </div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户 ID">{{ detail.data.id }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatTime(detail.data.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
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
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  color: $text-title;
}
.link-text.danger {
  color: $danger;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.detail-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: $text-title;
  margin-bottom: 6px;
}
.stat-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}
.stat {
  flex: 1;
  background: $table-head-bg;
  border-radius: $radius-control;
  padding: 16px;
  text-align: center;
}
.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: $brand;
}
.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: $text-weak;
}
</style>
