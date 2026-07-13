<script setup>
defineProps({
  // 普通空态文案与图标
  text: { type: String, default: '这里空空如也' },
  icon: { type: String, default: '📭' },
  // 错误态：置 true 时展示网络异常兜底 + 重试按钮
  error: { type: Boolean, default: false },
  errorText: { type: String, default: '网络开小差了' },
  errorIcon: { type: String, default: '📡' },
})
const emit = defineEmits(['retry'])
</script>

<template>
  <view class="k-empty">
    <text class="ico">{{ error ? errorIcon : icon }}</text>
    <text class="txt">{{ error ? errorText : text }}</text>
    <view v-if="error" class="retry" hover-class="hover-dim" :hover-stay-time="0" @tap="emit('retry')">重新加载</view>
    <slot v-else />
  </view>
</template>

<style lang="scss" scoped>
.k-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0 80rpx;
  .ico {
    font-size: 96rpx;
    opacity: 0.5;
    margin-bottom: 24rpx;
  }
  .txt {
    font-size: $fs-sub;
    color: $text-weak;
  }
  .retry {
    margin-top: 32rpx;
    padding: 16rpx 56rpx;
    border: 2rpx solid $brand;
    color: $brand;
    border-radius: $radius-pill;
    font-size: $fs-body;
    font-weight: 600;
  }
}
</style>
