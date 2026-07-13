<script setup>
import { ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toYuan } from '@/utils/format'
import { statusText, statusColor } from '@/utils/order-status'

const tabs = [
  { status: null, label: '全部' },
  { status: 0, label: '待付款' },
  { status: 1, label: '待发货' },
  { status: 2, label: '待收货' },
  { status: 3, label: '已完成' },
]
const activeStatus = ref(null)
const list = ref([])
const page = ref(1)
const size = 10
const finished = ref(false)
const loading = ref(false)
const loadError = ref(false)

onLoad((opts) => {
  if (opts.status !== undefined) activeStatus.value = Number(opts.status)
  fetch(true)
})

function pick(status) {
  if (activeStatus.value === status) return
  activeStatus.value = status
  fetch(true)
}

async function fetch(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
    list.value = []
  }
  if (finished.value) return
  loading.value = true
  loadError.value = false
  try {
    const res = await api.orders({
      status: activeStatus.value ?? undefined,
      page: page.value,
      size,
    })
    list.value = list.value.concat(res.list || [])
    if (list.value.length >= res.total || (res.list || []).length < size) finished.value = true
    else page.value += 1
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onReachBottom(() => fetch())

function toDetail(no) {
  uni.navigateTo({ url: `/pages/order/detail?no=${no}` })
}
</script>

<template>
  <view class="olist">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.label"
        class="tab"
        :class="{ active: activeStatus === t.status }"
        @tap="pick(t.status)"
      >
        {{ t.label }}
      </view>
    </view>

    <scroll-view scroll-y class="scroll" v-if="list.length">
      <view v-for="o in list" :key="o.orderNo" class="order" hover-class="hover-press" :hover-stay-time="0" @tap="toDetail(o.orderNo)">
        <view class="o-head">
          <text class="o-no">{{ o.orderNo }}</text>
          <text class="o-status" :style="{ color: statusColor(o.status) }">{{ statusText(o.status) }}</text>
        </view>
        <view v-for="(it, i) in o.items" :key="i" class="o-item">
          <view class="oi-img"><k-image :src="it.productImage" radius="24rpx" /></view>
          <view class="oi-meta">
            <text class="oi-name">{{ it.productName }}</text>
            <text class="oi-sku">{{ it.skuName }}</text>
          </view>
          <view class="oi-right">
            <k-price :value="it.unitPrice" :size="28" />
            <text class="oi-qty">×{{ it.quantity }}</text>
          </view>
        </view>
        <view class="o-foot">
          <text class="o-total">共 {{ o.items?.length || 0 }} 件 实付</text>
          <k-price :value="o.payAmount" :size="34" />
        </view>
      </view>
      <view v-if="finished" class="tip">— 没有更多了 —</view>
      <view class="safe-bottom" />
    </scroll-view>

    <k-empty v-else-if="!loading" :error="loadError" text="暂无相关订单" @retry="fetch(true)" />
  </view>
</template>

<style lang="scss" scoped>
.olist {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.tabs {
  display: flex;
  background: #fff;
  padding: 0 $safe-x;
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
.order {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 24rpx;
}
.o-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid $divider;
}
.o-no {
  font-size: $fs-aux;
  color: $text-weak;
}
.o-status {
  font-size: $fs-sub;
  font-weight: 600;
}
.o-item {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 0;
  align-items: center;
}
.oi-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-img;
  background: $fill-secondary;
}
.oi-meta {
  flex: 1;
  min-width: 0;
}
.oi-name {
  font-size: $fs-sub;
  color: $text-title;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.oi-sku {
  margin-top: 8rpx;
  font-size: $fs-aux;
  color: $text-weak;
}
.oi-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
}
.oi-qty {
  font-size: $fs-aux;
  color: $text-weak;
}
.o-foot {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 12rpx;
  padding-top: 16rpx;
  border-top: 2rpx solid $divider;
}
.o-total {
  font-size: $fs-sub;
  color: $text-body;
}
.tip {
  text-align: center;
  padding: 32rpx 0;
  font-size: $fs-aux;
  color: $text-placeholder;
}
.safe-bottom {
  height: 32rpx;
}
</style>
