<script setup>
import { ref, watch } from 'vue'

// 统一图片：空 URL / 加载失败显示占位，加载成功淡入。
const props = defineProps({
  src: { type: String, default: '' },
  mode: { type: String, default: 'aspectFill' },
  radius: { type: String, default: '0' }, // 传入如 '24rpx'
  lazy: { type: Boolean, default: true },
})

const loaded = ref(false)
const failed = ref(false)

watch(
  () => props.src,
  () => {
    loaded.value = false
    failed.value = false
  }
)

function onLoad() {
  loaded.value = true
}
function onError() {
  failed.value = true
}
</script>

<template>
  <view class="k-image" :style="{ borderRadius: radius }">
    <view v-if="!src || failed" class="ph">
      <text class="ph-ico">🖼️</text>
    </view>
    <image
      v-else
      class="img"
      :class="{ show: loaded }"
      :src="src"
      :mode="mode"
      :lazy-load="lazy"
      @load="onLoad"
      @error="onError"
    />
  </view>
</template>

<style lang="scss" scoped>
.k-image {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: $fill-secondary;
}
.img {
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.img.show {
  opacity: 1;
}
.ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $fill-secondary;
}
.ph-ico {
  font-size: 56rpx;
  opacity: 0.35;
}
</style>
