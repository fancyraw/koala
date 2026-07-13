<script setup>
// 商品卡片（瀑布流/网格通用）。图片即主角，通图圆角只在顶部。
import { ref } from 'vue'

const props = defineProps({
  product: { type: Object, required: true },
})
const emit = defineEmits(['tap'])

const imgFailed = ref(false)

function onTap() {
  if (props.product.soldOut) return
  emit('tap', props.product)
  uni.navigateTo({ url: `/pages/product/detail?id=${props.product.id}` })
}
</script>

<template>
  <view class="card" hover-class="hover-press" :hover-stay-time="0" @tap="onTap">
    <view class="cover">
      <view v-if="!product.mainImage || imgFailed" class="img-ph"><text class="ph-ico">🖼️</text></view>
      <image v-else class="img" :src="product.mainImage" mode="widthFix" lazy-load @error="imgFailed = true" />
      <view v-if="product.soldOut" class="soldout">
        <text class="soldout-txt">已售罄</text>
      </view>
      <view v-else-if="product.tagName" class="tag">{{ product.tagName }}</view>
    </view>
    <view class="body">
      <text class="name">{{ product.name }}</text>
      <view v-if="product.highlight" class="highlight">{{ product.highlight }}</view>
      <view class="bottom">
        <k-price :value="product.minPrice" :size="34" />
        <text v-if="product.salesCount" class="sales">已售{{ product.salesCount }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.card {
  background: $card-bg;
  border-radius: $radius-card;
  overflow: hidden;
}
.cover {
  position: relative;
  width: 100%;
  background: $fill-secondary;
}
.img {
  width: 100%;
  display: block;
}
.img-ph {
  width: 100%;
  height: 280rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.img-ph .ph-ico {
  font-size: 64rpx;
  opacity: 0.35;
}
.tag {
  position: absolute;
  left: 12rpx;
  top: 12rpx;
  padding: 4rpx 12rpx;
  background: $brand-light;
  color: $brand;
  font-size: $fs-tiny;
  border-radius: $radius-tag;
}
.soldout {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.55);
}
.soldout-txt {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: $fs-aux;
  display: flex;
  align-items: center;
  justify-content: center;
}
.body {
  padding: 16rpx 20rpx 20rpx;
}
.name {
  font-size: $fs-body;
  color: $text-title;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.highlight {
  margin-top: 8rpx;
  font-size: $fs-aux;
  color: $text-weak;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.bottom {
  margin-top: 16rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.sales {
  font-size: $fs-tiny;
  color: $text-placeholder;
}
</style>
