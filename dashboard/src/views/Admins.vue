<script setup>
import { ref, reactive, onMounted } from 'vue'
import QRCode from 'qrcode'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/api'
import { formatTime } from '@/utils/format'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()

const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try {
    list.value = await api.admins()
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const next = row.isValid !== 1
  const word = next ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${word}管理员「${row.nickname}」？`, '提示', { type: 'warning' })
  await api.adminStatus(row.id, next)
  row.isValid = next ? 1 : 0
  ElMessage.success(`已${word}`)
}

// ---- 邀请弹窗 ----
const invite = reactive({ visible: false, loading: false, qrDataUrl: '', expireSeconds: 0 })
async function openInvite() {
  invite.visible = true
  invite.loading = true
  invite.qrDataUrl = ''
  try {
    const res = await api.adminInvite()
    invite.expireSeconds = res.expireSeconds
    invite.qrDataUrl = await QRCode.toDataURL(res.qrcodeUrl, { width: 220, margin: 1 })
  } finally {
    invite.loading = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="page-hint">超级管理员可邀请新管理员、启用或禁用账号</div>
      <div class="toolbar-spacer" />
      <el-button type="primary" :icon="Plus" @click="openInvite">邀请管理员</el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column label="管理员" min-width="220">
          <template #default="{ row }">
            <div class="admin-cell">
              <el-avatar :size="36" :src="row.avatarUrl">{{ (row.nickname || '?').slice(0, 1) }}</el-avatar>
              <span class="admin-name">{{ row.nickname }}</span>
              <el-tag v-if="row.superAdmin" type="warning" size="small">超管</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.isValid === 1" type="success" size="small">已启用</el-tag>
            <el-tag v-else-if="row.isValid === 0" type="danger" size="small">已禁用</el-tag>
            <el-tag v-else type="info" size="small">待审核</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="180">
          <template #default="{ row }">{{ formatTime(row.lastLoginAt) || '—' }}</template>
        </el-table-column>
        <el-table-column label="加入时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <div v-if="!row.superAdmin && row.id !== auth.profile?.id" class="op-btns">
              <span class="link-text" :class="{ danger: row.isValid === 1 }" @click="toggleStatus(row)">
                {{ row.isValid === 1 ? '禁用' : '启用' }}
              </span>
            </div>
            <span v-else class="text-weak">—</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="invite.visible" title="邀请管理员" width="380px">
      <div v-loading="invite.loading" class="invite-body">
        <img v-if="invite.qrDataUrl" :src="invite.qrDataUrl" class="invite-qr" alt="邀请二维码" />
        <div class="invite-tip">让对方用微信扫码，扫码后进入待审核状态</div>
        <div v-if="invite.expireSeconds" class="invite-expire">
          二维码 {{ Math.round(invite.expireSeconds / 60) }} 分钟内有效
        </div>
      </div>
      <template #footer>
        <el-button @click="openInvite">刷新二维码</el-button>
        <el-button type="primary" @click="invite.visible = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.page-hint {
  color: $text-weak;
  font-size: 13px;
}
.toolbar-spacer {
  flex: 1;
}
.card {
  background: #fff;
  border-radius: $radius-card;
  padding: 16px 20px;
  border: 1px solid $border;
}
.admin-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.admin-name {
  color: $text-title;
}
.link-text.danger {
  color: $danger;
}
.text-weak {
  color: $text-weak;
}
.invite-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  min-height: 220px;
}
.invite-qr {
  width: 220px;
  height: 220px;
  border: 1px solid $border;
  border-radius: $radius-control;
}
.invite-tip {
  font-size: 13px;
  color: $text-body;
}
.invite-expire {
  font-size: 12px;
  color: $text-weak;
}
</style>
