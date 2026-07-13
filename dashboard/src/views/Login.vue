<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'
import { api } from '@/api'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const auth = useAuthStore()

const qrDataUrl = ref('')
const state = ref('')
const status = ref('loading') // loading | waiting | scanned | expired | rejected
let timer = null

const statusText = {
  loading: '二维码加载中…',
  waiting: '请使用微信扫码登录',
  scanned: '已扫码，请在手机上确认',
  expired: '二维码已过期',
  rejected: '未授权或账号未启用',
}

async function initQrcode() {
  stopPoll()
  status.value = 'loading'
  try {
    const res = await api.loginQrcode()
    state.value = res.state
    qrDataUrl.value = await QRCode.toDataURL(res.qrcodeUrl, { width: 220, margin: 1 })
    status.value = 'waiting'
    startPoll()
  } catch (e) {
    status.value = 'expired'
  }
}

function startPoll() {
  timer = setInterval(poll, 2000)
}
function stopPoll() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function poll() {
  try {
    const res = await api.loginCheck(state.value)
    if (res.status === 'scanned') {
      status.value = 'scanned'
    } else if (res.status === 'confirmed') {
      stopPoll()
      auth.setLogin(res.login)
      ElMessage.success('登录成功')
      router.replace('/dashboard')
    } else if (res.status === 'expired' || res.status === 'rejected') {
      stopPoll()
      status.value = res.status
    }
  } catch (e) {
    stopPoll()
    status.value = 'expired'
  }
}

onMounted(initQrcode)
onUnmounted(stopPoll)
</script>

<template>
  <div class="login">
    <div class="login-card">
      <div class="brand">
        <div class="logo">K</div>
        <div class="brand-name">KOALA 管理后台</div>
      </div>
      <div class="qr-box">
        <img v-if="qrDataUrl && status !== 'expired'" :src="qrDataUrl" class="qr" alt="登录二维码" />
        <div v-else class="qr qr-mask">
          <el-button type="primary" plain @click="initQrcode">刷新二维码</el-button>
        </div>
        <div v-if="status === 'scanned'" class="qr-overlay">已扫码，请确认</div>
      </div>
      <div class="status" :class="status">{{ statusText[status] }}</div>
      <div class="tip">仅限受邀管理员扫码登录</div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a2332 0%, #2b3a52 100%);
}
.login-card {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}
.logo {
  width: 40px;
  height: 40px;
  background: $brand;
  color: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}
.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: $text-title;
}
.qr-box {
  position: relative;
  width: 220px;
  height: 220px;
}
.qr {
  width: 220px;
  height: 220px;
  border: 1px solid $border;
  border-radius: 8px;
}
.qr-mask {
  display: flex;
  align-items: center;
  justify-content: center;
  background: $table-head-bg;
}
.qr-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $brand;
  font-weight: 600;
  border-radius: 8px;
}
.status {
  margin-top: 24px;
  font-size: 15px;
  color: $text-body;
  &.expired,
  &.rejected {
    color: $danger;
  }
}
.tip {
  margin-top: 8px;
  font-size: 12px;
  color: $text-weak;
}
</style>
