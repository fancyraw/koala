<script setup>
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toYuan, formatTime } from '@/utils/format'
import { afterSaleStatusText, afterSaleStatusColor } from '@/utils/aftersale-status'

const list = ref([])
const page = ref(1)
const size = 10
const finished = ref(false)
const loading = ref(false)
const firstLoaded = ref(false)
const loadError = ref(false)

onShow(() => {
  // 从详情页返回也刷新
  fetch(true)
})

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
    const res = await api.afterSaleList({ page: page.value, size })
    list.value = list.value.concat(res.list || [])
    if (list.value.length >= res.total || (res.list || []).length < size) finished.value = true
    else page.value += 1
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
    firstLoaded.value = true
  }
}

onReachBottom(() => fetch())

function toDetail(no) {
  uni.navigateTo({ url: `/pages/aftersale/detail?no=${no}` })
}

const typeText = (t) => (t === 1 ? '仅退款' : '退货退款')
</script>

<template>
  <view class="as-list">
    <scroll-view scroll-y class="scroll" v-if="list.length">
      <view v-for="a in list" :key="a.afterSaleNo" class="card" hover-class="hover-press" :hover-stay-time="0" @tap="toDetail(a.afterSaleNo)">
        <view class="head">
          <text class="type">{{ typeText(a.type) }}</text>
          <text class="status" :style="{ color: afterSaleStatusColor(a.status) }">
            {{ afterSaleStatusText(a.status) }}
          </text>
        </view>
        <view class="body">
          <view class="row"><text class="k">售后单号</text><text class="v">{{ a.afterSaleNo }}</text></view>
          <view class="row"><text class="k">退款原因</text><text class="v">{{ a.reason }}</text></view>
          <view class="row"><text class="k">退款金额</text><text class="v amount">¥{{ toYuan(a.refundAmount) }}</text></view>
          <view class="row"><text class="k">申请时间</text><text class="v">{{ formatTime(a.createdAt) }}</text></view>
        </view>
      </view>
      <view v-if="finished" class="tip">— 没有更多了 —</view>
      <view class="safe-bottom" />
    </scroll-view>

    <k-empty v-else-if="firstLoaded" :error="loadError" text="暂无售后记录" @retry="fetch(true)" />
  </view>
</template>

<style lang="scss" scoped>
.as-list {
  height: 100vh;
  background: $page-bg;
}
.scroll {
  height: 100%;
}
.card {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 24rpx;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid $divider;
}
.type {
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
}
.status {
  font-size: $fs-sub;
  font-weight: 600;
}
.body {
  padding-top: 16rpx;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
}
.k {
  font-size: $fs-aux;
  color: $text-weak;
}
.v {
  font-size: $fs-aux;
  color: $text-body;
  max-width: 60%;
  text-align: right;
}
.v.amount {
  color: $brand;
  font-weight: 600;
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
