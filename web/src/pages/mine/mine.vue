<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { confirm } from '@/utils/toast'

const auth = useAuthStore()
const sysInfo = uni.getSystemInfoSync()
const statusBarHeight = sysInfo.statusBarHeight || 20

// 订单状态入口（售后单独走列表页，非订单状态筛选）
const orderTabs = [
  { status: 0, label: '待付款', icon: '💳' },
  { status: 1, label: '待发货', icon: '📦' },
  { status: 2, label: '待收货', icon: '🚚' },
  { status: 3, label: '已完成', icon: '✅' },
  { status: 'aftersale', label: '退款/售后', icon: '🔧' },
]

const services = [
  { label: '我的优惠券', icon: '🎫', url: '/pages/coupon/mine' },
  { label: '收货地址', icon: '📍', url: '/pages/address/manage' },
  { label: '退款/售后', icon: '🔧', url: '/pages/aftersale/list' },
]

onShow(() => auth.restore())

function ensureLogin() {
  if (!auth.isLogin) {
    uni.navigateTo({ url: '/pages/login/login' })
    return false
  }
  return true
}

function toOrders(status) {
  if (!ensureLogin()) return
  if (status === 'aftersale') {
    uni.navigateTo({ url: '/pages/aftersale/list' })
    return
  }
  const q = status != null ? `?status=${status}` : ''
  uni.navigateTo({ url: `/pages/order/list${q}` })
}

function toPage(url) {
  if (!ensureLogin()) return
  uni.navigateTo({ url })
}

async function logout() {
  const ok = await confirm('确定退出登录？')
  if (ok) auth.logout()
}
</script>

<template>
  <view class="mine">
    <view class="profile" :style="{ paddingTop: statusBarHeight + 30 + 'px' }">
      <template v-if="auth.isLogin">
        <image class="avatar" :src="auth.avatarUrl || '/static/tabbar/mine.png'" mode="aspectFill" />
        <text class="nickname">{{ auth.nickname || '微信用户' }}</text>
      </template>
      <view v-else class="login-entry" @tap="uni.navigateTo({ url: '/pages/login/login' })">
        <image class="avatar" src="/static/tabbar/mine.png" mode="aspectFill" />
        <text class="nickname">点击登录</text>
      </view>
    </view>

    <!-- 我的订单 -->
    <view class="card">
      <view class="card-head">
        <text class="card-title">我的订单</text>
        <text class="card-more" @tap="toOrders()">全部 ›</text>
      </view>
      <view class="order-tabs">
        <view v-for="t in orderTabs" :key="t.status" class="ot" hover-class="hover-press" :hover-stay-time="0" @tap="toOrders(t.status)">
          <text class="ot-ico">{{ t.icon }}</text>
          <text class="ot-label">{{ t.label }}</text>
        </view>
      </view>
    </view>

    <!-- 服务 -->
    <view class="card">
      <view v-for="s in services" :key="s.url" class="svc-row" hover-class="hover-press" :hover-stay-time="0" @tap="toPage(s.url)">
        <text class="svc-ico">{{ s.icon }}</text>
        <text class="svc-label">{{ s.label }}</text>
        <text class="svc-arrow">›</text>
      </view>
    </view>

    <view v-if="auth.isLogin" class="logout" @tap="logout">退出登录</view>
  </view>
</template>

<style lang="scss" scoped>
.mine {
  min-height: 100vh;
  background: $page-bg;
}
.profile {
  background: linear-gradient(135deg, #ffd9df 0%, #f8f8f8 100%);
  padding-bottom: 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}
.login-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}
.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #fff;
  border: 4rpx solid #fff;
}
.nickname {
  font-size: $fs-title;
  font-weight: 600;
  color: $text-title;
}
.card {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 24rpx;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.card-title {
  font-size: $fs-title;
  font-weight: 600;
  color: $text-title;
}
.card-more {
  font-size: $fs-aux;
  color: $text-weak;
}
.order-tabs {
  display: flex;
  justify-content: space-between;
}
.ot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.ot-ico {
  font-size: 44rpx;
}
.ot-label {
  font-size: $fs-aux;
  color: $text-body;
}
.svc-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 0;
  border-bottom: 2rpx solid $divider;
}
.svc-row:last-child {
  border-bottom: none;
}
.svc-ico {
  font-size: 36rpx;
}
.svc-label {
  flex: 1;
  font-size: $fs-body;
  color: $text-title;
}
.svc-arrow {
  font-size: 36rpx;
  color: $text-placeholder;
}
.logout {
  margin: 40rpx $safe-x;
  height: 88rpx;
  background: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-body;
  color: $text-weak;
}
</style>
