<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toYuan, formatTime } from '@/utils/format'

const tabs = [
  { status: 0, label: '未使用' },
  { status: 1, label: '已使用' },
  { status: 2, label: '已过期' },
]
const activeStatus = ref(0)
const list = ref([])
const loading = ref(false)
const loadError = ref(false)

onLoad(() => load())

function pick(s) {
  if (activeStatus.value === s) return
  activeStatus.value = s
  load()
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    list.value = await api.couponMine(activeStatus.value)
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function useCoupon() {
  uni.switchTab({ url: '/pages/home/home' })
}
</script>

<template>
  <view class="coupon">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.status"
        class="tab"
        :class="{ active: activeStatus === t.status }"
        @tap="pick(t.status)"
      >
        {{ t.label }}
      </view>
    </view>

    <scroll-view scroll-y class="scroll" v-if="list.length">
      <view
        v-for="c in list"
        :key="c.id"
        class="ticket"
        :class="{ disabled: activeStatus !== 0 }"
      >
        <view class="left">
          <view class="amount">
            <text class="cur">¥</text>
            <text class="num">{{ toYuan(c.discountAmount) }}</text>
          </view>
          <text class="cond">{{ c.type === 1 ? `满${toYuan(c.minSpend)}可用` : '无门槛' }}</text>
        </view>
        <view class="right">
          <text class="name">{{ c.name }}</text>
          <text class="expire">{{ c.expireAt ? '有效期至 ' + formatTime(c.expireAt) : '长期有效' }}</text>
          <view v-if="c.nearExpiry && activeStatus === 0" class="near">即将过期</view>
        </view>
        <view v-if="activeStatus === 0" class="use-btn" hover-class="hover-dim" :hover-stay-time="0" @tap="useCoupon">去使用</view>
      </view>
      <view class="safe-bottom" />
    </scroll-view>

    <k-empty v-else-if="!loading" :error="loadError" text="暂无优惠券" @retry="load" />
  </view>
</template>

<style lang="scss" scoped>
.coupon {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.tabs {
  display: flex;
  background: #fff;
}
.tab {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-body;
  color: $text-body;
  position: relative;
}
.tab.active {
  color: $brand;
  font-weight: 600;
}
.tab.active::after {
  content: '';
  position: absolute;
  bottom: 8rpx;
  width: 40rpx;
  height: 6rpx;
  background: $brand;
  border-radius: 6rpx;
}
.scroll {
  flex: 1;
}
.ticket {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  display: flex;
  align-items: center;
  padding: 32rpx 24rpx;
  position: relative;
  overflow: hidden;
}
.ticket.disabled {
  opacity: 0.55;
}
.left {
  width: 200rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 2rpx dashed $divider;
  padding-right: 24rpx;
}
.amount {
  display: flex;
  align-items: baseline;
  color: $brand;
}
.cur {
  font-size: 28rpx;
  font-weight: 700;
}
.num {
  font-size: 60rpx;
  font-weight: 700;
}
.cond {
  font-size: $fs-tiny;
  color: $text-weak;
  margin-top: 4rpx;
}
.right {
  flex: 1;
  padding-left: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.name {
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
}
.expire {
  font-size: $fs-aux;
  color: $text-weak;
}
.near {
  align-self: flex-start;
  font-size: $fs-tiny;
  color: $brand;
  background: $brand-light;
  padding: 2rpx 12rpx;
  border-radius: $radius-tag;
}
.use-btn {
  padding: 12rpx 28rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  font-size: $fs-aux;
  font-weight: 600;
}
.safe-bottom {
  height: 32rpx;
}
</style>
