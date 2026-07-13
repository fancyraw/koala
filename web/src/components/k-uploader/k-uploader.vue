<script setup>
import { computed } from 'vue'
import { chooseAndUpload } from '@/utils/uploader'
import { toast } from '@/utils/toast'

// v-model 绑定 URL 数组
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 3 },
})
const emit = defineEmits(['update:modelValue'])

const list = computed(() => props.modelValue || [])
const canAdd = computed(() => list.value.length < props.max)

async function add() {
  const remain = props.max - list.value.length
  if (remain <= 0) return
  try {
    const urls = await chooseAndUpload(remain)
    if (urls.length) emit('update:modelValue', [...list.value, ...urls])
  } catch (e) {
    toast(e.message || '上传失败')
  }
}

function remove(idx) {
  const next = list.value.slice()
  next.splice(idx, 1)
  emit('update:modelValue', next)
}

function preview(url) {
  uni.previewImage({ current: url, urls: list.value })
}
</script>

<template>
  <view class="k-uploader">
    <view v-for="(url, i) in list" :key="i" class="cell">
      <image class="img" :src="url" mode="aspectFill" @tap="preview(url)" />
      <view class="del" @tap.stop="remove(i)">
        <text class="del-x">✕</text>
      </view>
    </view>
    <view v-if="canAdd" class="cell add" @tap="add">
      <text class="plus">＋</text>
      <text class="add-txt">{{ list.length }}/{{ max }}</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.k-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.cell {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-img;
  position: relative;
  overflow: visible;
}
.img {
  width: 100%;
  height: 100%;
  border-radius: $radius-img;
  background: $fill-secondary;
}
.del {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.del-x {
  color: #fff;
  font-size: 22rpx;
}
.add {
  background: $fill-secondary;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}
.plus {
  font-size: 56rpx;
  color: $text-placeholder;
  line-height: 1;
}
.add-txt {
  font-size: $fs-tiny;
  color: $text-placeholder;
}
</style>
