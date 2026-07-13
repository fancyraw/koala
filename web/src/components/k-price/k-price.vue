<script setup>
import { computed } from 'vue'
import { splitPrice } from '@/utils/format'

const props = defineProps({
  value: { type: [Number, String], default: 0 },
  size: { type: Number, default: 32 }, // 整数部分 rpx
  color: { type: String, default: '#ff2442' },
  bold: { type: Boolean, default: true },
  prefix: { type: String, default: '¥' },
})

const parts = computed(() => splitPrice(props.value))
const symbolSize = computed(() => Math.round(props.size * 0.6))
const decSize = computed(() => Math.round(props.size * 0.72))
</script>

<template>
  <text class="k-price" :style="{ color, fontWeight: bold ? 700 : 400 }">
    <text class="sym" :style="{ fontSize: symbolSize + 'rpx' }">{{ prefix }}</text>
    <text class="int" :style="{ fontSize: size + 'rpx' }">{{ parts.int }}</text>
    <text class="dec" :style="{ fontSize: decSize + 'rpx' }">.{{ parts.dec }}</text>
  </text>
</template>

<style lang="scss" scoped>
.k-price {
  display: inline-flex;
  align-items: baseline;
  font-family: 'SF Pro', -apple-system, sans-serif;
}
.sym {
  margin-right: 2rpx;
}
</style>
