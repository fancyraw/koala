<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toYuan } from '@/utils/format'

const orderNo = ref('')
const status = ref('success') // success | fail
const amount = ref(0)
const checking = ref(true)

onLoad(async (opts) => {
  orderNo.value = opts.no || ''
  amount.value = opts.amount || 0
  status.value = opts.status || 'success'
  // 复核真实订单状态（mock 支付回调可能异步）
  await verify()
})

async function verify() {
  checking.value = true
  try {
    const detail = await api.orderDetail(orderNo.value)
    // 状态 >0 且 !=4(取消) 视为已支付；0=待付款
    status.value = detail.status === 0 ? 'fail' : 'success'
    if (detail.payAmount) amount.value = detail.payAmount
  } catch (e) {
    /* 保持传入状态 */
  } finally {
    checking.value = false
  }
}

function toOrder() {
  uni.redirectTo({ url: `/pages/order/detail?no=${orderNo.value}` })
}
function toHome() {
  uni.switchTab({ url: '/pages/home/home' })
}
async function retry() {
  try {
    await api.orderPay(orderNo.value)
    await verify()
  } catch (e) {}
}
</script>

<template>
  <view class="result">
    <view class="hero">
      <view class="icon" :class="status">
        <text class="icon-mark">{{ status === 'success' ? '✓' : '!' }}</text>
      </view>
      <text class="title">{{ checking ? '确认支付结果…' : status === 'success' ? '支付成功' : '待支付' }}</text>
      <k-price v-if="status === 'success'" :value="amount" :size="56" />
    </view>

    <view class="actions">
      <template v-if="status === 'success'">
        <view class="btn primary" hover-class="hover-dim" :hover-stay-time="0" @tap="toOrder">查看订单</view>
        <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="toHome">返回首页</view>
      </template>
      <template v-else>
        <view class="btn primary" hover-class="hover-dim" :hover-stay-time="0" @tap="retry">重新支付</view>
        <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="toOrder">查看订单</view>
      </template>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.result {
  min-height: 100vh;
  background: $page-bg;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 0 80rpx;
}
.icon {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40rpx;
}
.icon.success {
  background: $brand;
}
.icon.fail {
  background: #ffb020;
}
.icon-mark {
  color: #fff;
  font-size: 72rpx;
  font-weight: 700;
}
.title {
  font-size: $fs-display;
  font-weight: 700;
  color: $text-title;
  margin-bottom: 24rpx;
}
.actions {
  width: 100%;
  padding: 0 64rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-top: 40rpx;
}
.btn {
  height: 88rpx;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-title;
  font-weight: 600;
}
.btn.primary {
  background: $brand;
  color: #fff;
}
.btn.ghost {
  background: $fill-secondary;
  color: $text-body;
}
</style>
