<script setup>
import { computed } from 'vue'

// 骨架屏。type: waterfall(瀑布流两列卡片) | detail(商品详情)
const props = defineProps({
  type: { type: String, default: 'waterfall' },
  count: { type: Number, default: 6 },
})

const cards = computed(() => Array.from({ length: props.count }))
// 瀑布流交错高度，避免呆板
const heights = [280, 340, 300, 360, 260, 320]
</script>

<template>
  <view class="k-skeleton">
    <template v-if="type === 'waterfall'">
      <view class="wf">
        <view class="col">
          <view
            v-for="(_, i) in cards"
            :key="'l' + i"
            v-show="i % 2 === 0"
            class="card sk-shine"
          >
            <view class="img" :style="{ height: heights[i % heights.length] + 'rpx' }" />
            <view class="line w80" />
            <view class="line w50" />
          </view>
        </view>
        <view class="col">
          <view
            v-for="(_, i) in cards"
            :key="'r' + i"
            v-show="i % 2 === 1"
            class="card sk-shine"
          >
            <view class="img" :style="{ height: heights[(i + 3) % heights.length] + 'rpx' }" />
            <view class="line w80" />
            <view class="line w50" />
          </view>
        </view>
      </view>
    </template>

    <template v-else-if="type === 'detail'">
      <view class="d-img sk-shine" />
      <view class="d-block">
        <view class="line w40 lg sk-shine" />
        <view class="line w90 sk-shine" />
        <view class="line w70 sk-shine" />
      </view>
      <view class="d-block">
        <view class="line w60 sk-shine" />
        <view class="line w90 sk-shine" />
        <view class="line w80 sk-shine" />
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.k-skeleton {
  width: 100%;
}
/* 微光泽流动 */
.sk-shine {
  position: relative;
  overflow: hidden;
}
.sk-shine::after {
  content: '';
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
  animation: sk-slide 1.4s infinite;
}
@keyframes sk-slide {
  100% {
    transform: translateX(100%);
  }
}
.wf {
  display: flex;
  padding: 8rpx $safe-x 0;
  gap: 20rpx;
}
.col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.card {
  background: #fff;
  border-radius: $radius-card;
  overflow: hidden;
  padding-bottom: 20rpx;
}
.img {
  width: 100%;
  background: #ececec;
}
.line {
  height: 24rpx;
  margin: 16rpx 20rpx 0;
  border-radius: 8rpx;
  background: #ececec;
}
.w40 {
  width: 40%;
}
.w50 {
  width: 50%;
}
.w60 {
  width: 60%;
}
.w70 {
  width: 70%;
}
.w80 {
  width: 80%;
}
.w90 {
  width: 90%;
}
.lg {
  height: 40rpx;
}
.d-img {
  width: 100%;
  height: 750rpx;
  background: #ececec;
}
.d-block {
  background: #fff;
  margin-top: 16rpx;
  padding: 24rpx 0 32rpx;
}
</style>
