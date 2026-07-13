<script setup>
import { computed } from 'vue'

// 自定义导航栏（沉浸式页面用）。自动适配状态栏高度与胶囊按钮。
const props = defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true },
  bg: { type: String, default: 'transparent' },
  color: { type: String, default: '#1a1a1a' },
})

const sysInfo = uni.getSystemInfoSync()
const statusBarHeight = sysInfo.statusBarHeight || 20
// 小程序胶囊：导航内容区高度约 44px
const navContentHeight = 44

const totalHeight = computed(() => statusBarHeight + navContentHeight)

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/home/home' })
}

defineExpose({ totalHeight })
</script>

<template>
  <view class="k-navbar" :style="{ height: totalHeight + 'px', background: bg }">
    <view class="status" :style="{ height: statusBarHeight + 'px' }" />
    <view class="bar" :style="{ height: navContentHeight + 'px' }">
      <view v-if="back" class="back" @tap="goBack">
        <text class="arrow" :style="{ color }">‹</text>
      </view>
      <text class="title" :style="{ color }">{{ title }}</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.k-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}
.bar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back {
  position: absolute;
  left: 12rpx;
  top: 0;
  bottom: 0;
  width: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.arrow {
  font-size: 56rpx;
  line-height: 1;
  font-weight: 300;
}
.title {
  font-size: $fs-title;
  font-weight: 600;
  max-width: 60%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
