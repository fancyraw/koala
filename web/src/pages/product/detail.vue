<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { useCartStore } from '@/store/cart'
import { toast } from '@/utils/toast'

const cart = useCartStore()
const detail = ref(null)
const loading = ref(true)
const loadError = ref(false)
const mainImgFailed = ref(false)

// SKU 选择弹层
const skuVisible = ref(false)
const skuMode = ref('cart') // cart | buy
const selectedSku = ref(null)
const quantity = ref(1)

const priceRange = computed(() => {
  const skus = detail.value?.skus || []
  if (!skus.length) return { min: 0, max: 0 }
  const prices = skus.map((s) => Number(s.price))
  return { min: Math.min(...prices), max: Math.max(...prices) }
})

const maxQty = computed(() => {
  const stock = selectedSku.value?.stock ?? 0
  const limit = detail.value?.perOrderLimit || 0
  if (limit > 0) return Math.min(stock, limit)
  return stock
})

const productId = ref('')

onLoad((opts) => {
  productId.value = opts.id
  load(opts.id)
})

async function load(id) {
  loading.value = true
  loadError.value = false
  try {
    detail.value = await api.productDetail(id)
    const first = (detail.value.skus || []).find((s) => s.stock > 0) || detail.value.skus?.[0]
    selectedSku.value = first || null
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function openSku(mode) {
  if (detail.value?.soldOut) return toast('该商品已售罄')
  skuMode.value = mode
  quantity.value = 1
  skuVisible.value = true
}

function pickSku(sku) {
  if (sku.stock <= 0) return
  selectedSku.value = sku
  quantity.value = 1
}

function stepQty(delta) {
  const next = quantity.value + delta
  if (next < 1) return
  if (maxQty.value && next > maxQty.value) {
    return toast(detail.value.perOrderLimit && next > detail.value.perOrderLimit ? `每单限购${detail.value.perOrderLimit}件` : '库存不足')
  }
  quantity.value = next
}

async function confirmSku() {
  if (!selectedSku.value) return toast('请选择规格')
  if (selectedSku.value.stock <= 0) return toast('该规格无货')

  if (skuMode.value === 'cart') {
    await cart.add(selectedSku.value.id, quantity.value)
    skuVisible.value = false
    toast('已加入购物车')
  } else {
    skuVisible.value = false
    const items = [{ skuId: selectedSku.value.id, quantity: quantity.value }]
    uni.navigateTo({
      url: `/pages/order/confirm?items=${encodeURIComponent(JSON.stringify(items))}`,
    })
  }
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/cart' })
}
</script>

<template>
  <view v-if="loading && !detail" class="detail-loading">
    <k-navbar title="" bg="#ffffff" />
    <k-skeleton type="detail" />
  </view>

  <view v-else-if="!detail" class="detail-loading">
    <k-navbar title="" bg="#ffffff" />
    <k-empty :error="loadError" text="商品不存在或已下架" @retry="load(productId)" />
  </view>

  <view class="detail" v-else-if="detail">
    <k-navbar :title="detail.name" bg="#ffffff" />

    <scroll-view scroll-y class="scroll">
      <view v-if="!detail.mainImage || mainImgFailed" class="main-img main-img-ph"><text class="ph-ico">🖼️</text></view>
      <image v-else class="main-img" :src="detail.mainImage" mode="widthFix" @error="mainImgFailed = true" />

      <view class="info">
        <view class="price-row">
          <k-price :value="priceRange.min" :size="52" />
          <text v-if="priceRange.max > priceRange.min" class="price-max">~ {{ priceRange.max.toFixed(2) }}</text>
          <text v-if="detail.salesCount" class="sales">已售 {{ detail.salesCount }}</text>
        </view>
        <text class="name">{{ detail.name }}</text>
        <view v-if="detail.tagName" class="tag">{{ detail.tagName }}</view>
      </view>

      <view v-if="detail.highlights?.length" class="highlights">
        <view v-for="(h, i) in detail.highlights" :key="i" class="hl">
          <text class="hl-dot">·</text>
          <text class="hl-txt">{{ h }}</text>
        </view>
      </view>

      <view class="picker" hover-class="hover-dim" :hover-stay-time="0" @tap="openSku('cart')">
        <text class="picker-label">选择</text>
        <text class="picker-value">
          {{ selectedSku ? selectedSku.name : '请选择规格' }}
        </text>
        <text class="picker-arrow">›</text>
      </view>

      <view v-if="detail.detailImages?.length" class="detail-imgs">
        <view class="di-title">商品详情</view>
        <image
          v-for="(img, i) in detail.detailImages"
          :key="i"
          class="di-img"
          :src="img"
          mode="widthFix"
        />
      </view>

      <view class="safe-bottom" />
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <view class="ab-icon" @tap="goCart">
        <text class="ab-emoji">🛒</text>
        <text class="ab-label">购物车</text>
      </view>
      <view class="ab-btns">
        <view class="ab-btn ab-cart" hover-class="hover-dim" :hover-stay-time="0" @tap="openSku('cart')">加入购物车</view>
        <view class="ab-btn ab-buy" :class="{ disabled: detail.soldOut }" hover-class="hover-dim" :hover-stay-time="0" @tap="openSku('buy')">
          {{ detail.soldOut ? '已售罄' : '立即购买' }}
        </view>
      </view>
    </view>

    <!-- SKU 弹层 -->
    <view v-if="skuVisible" class="sku-mask" @tap="skuVisible = false">
      <view class="sku-sheet" @tap.stop>
        <view class="sku-head">
          <view class="sku-img"><k-image :src="selectedSku?.mainImage || detail.mainImage" radius="24rpx" /></view>
          <view class="sku-head-info">
            <k-price :value="selectedSku ? selectedSku.price : priceRange.min" :size="44" />
            <text class="sku-stock">库存 {{ selectedSku?.stock ?? 0 }}</text>
            <text class="sku-picked">{{ selectedSku ? selectedSku.name : '请选择规格' }}</text>
          </view>
          <text class="sku-close" @tap="skuVisible = false">✕</text>
        </view>

        <scroll-view scroll-y class="sku-body">
          <view class="sku-group">
            <text class="sku-group-title">规格</text>
            <view class="sku-options">
              <view
                v-for="s in detail.skus"
                :key="s.id"
                class="sku-opt"
                :class="{ active: selectedSku?.id === s.id, disabled: s.stock <= 0 }"
                @tap="pickSku(s)"
              >
                {{ s.name }}
              </view>
            </view>
          </view>

          <view class="qty-row">
            <text class="qty-label">数量</text>
            <view class="stepper">
              <view class="step" :class="{ disabled: quantity <= 1 }" @tap="stepQty(-1)">−</view>
              <text class="step-val">{{ quantity }}</text>
              <view class="step" @tap="stepQty(1)">+</view>
            </view>
          </view>
          <view v-if="detail.perOrderLimit" class="limit-tip">每单限购 {{ detail.perOrderLimit }} 件</view>
        </scroll-view>

        <view class="sku-confirm" hover-class="hover-dim" :hover-stay-time="0" @tap="confirmSku">
          {{ skuMode === 'cart' ? '加入购物车' : '立即购买' }}
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.detail {
  min-height: 100vh;
  background: $page-bg;
}
.detail-loading {
  min-height: 100vh;
  background: $page-bg;
  padding-top: 88rpx;
}
.scroll {
  height: 100vh;
}
.main-img {
  width: 100%;
  background: $fill-secondary;
}
.main-img-ph {
  height: 750rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.main-img-ph .ph-ico {
  font-size: 96rpx;
  opacity: 0.35;
}
.info {
  background: #fff;
  padding: 24rpx $safe-x;
  margin-top: -12rpx;
  border-radius: $radius-card $radius-card 0 0;
  position: relative;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
}
.price-max {
  color: $text-weak;
  font-size: $fs-sub;
}
.sales {
  margin-left: auto;
  font-size: $fs-aux;
  color: $text-placeholder;
}
.name {
  display: block;
  margin-top: 16rpx;
  font-size: $fs-display;
  font-weight: 600;
  color: $text-title;
  line-height: 1.4;
}
.tag {
  display: inline-block;
  margin-top: 16rpx;
  padding: 4rpx 14rpx;
  background: $brand-light;
  color: $brand;
  font-size: $fs-tiny;
  border-radius: $radius-tag;
}
.highlights {
  margin-top: 16rpx;
  background: #fff;
  padding: 20rpx $safe-x;
}
.hl {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-bottom: 8rpx;
}
.hl-dot {
  color: $brand;
  font-weight: 700;
}
.hl-txt {
  flex: 1;
  font-size: $fs-sub;
  color: $text-body;
}
.picker {
  margin-top: 16rpx;
  background: #fff;
  padding: 28rpx $safe-x;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.picker-label {
  color: $text-weak;
  font-size: $fs-body;
}
.picker-value {
  flex: 1;
  color: $text-title;
  font-size: $fs-body;
}
.picker-arrow {
  color: $text-placeholder;
  font-size: 36rpx;
}
.detail-imgs {
  margin-top: 16rpx;
  background: #fff;
  padding-bottom: 8rpx;
}
.di-title {
  text-align: center;
  padding: 28rpx 0;
  font-size: $fs-title;
  font-weight: 600;
  color: $text-title;
}
.di-img {
  width: 100%;
  display: block;
}
.safe-bottom {
  height: 140rpx;
}
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(100rpx + env(safe-area-inset-bottom));
  padding: 0 $safe-x env(safe-area-inset-bottom);
  background: #fff;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border-top: 2rpx solid $divider;
}
.ab-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 96rpx;
}
.ab-emoji {
  font-size: 40rpx;
}
.ab-label {
  font-size: $fs-tiny;
  color: $text-weak;
}
.ab-btns {
  flex: 1;
  display: flex;
  height: 76rpx;
  border-radius: $radius-pill;
  overflow: hidden;
}
.ab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-body;
  font-weight: 600;
  color: #fff;
}
.ab-cart {
  background: #ff8fa0;
}
.ab-buy {
  background: $brand;
}
.ab-buy.disabled {
  background: $text-placeholder;
}
/* SKU 弹层 */
.sku-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}
.sku-sheet {
  width: 100%;
  background: #fff;
  border-radius: $radius-card $radius-card 0 0;
  padding: 32rpx $safe-x calc(24rpx + env(safe-area-inset-bottom));
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}
.sku-head {
  display: flex;
  gap: 20rpx;
  position: relative;
  padding-bottom: 24rpx;
}
.sku-img {
  width: 180rpx;
  height: 180rpx;
  border-radius: $radius-img;
  background: $fill-secondary;
  margin-top: -60rpx;
}
.sku-head-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 8rpx;
}
.sku-stock {
  font-size: $fs-aux;
  color: $text-weak;
}
.sku-picked {
  font-size: $fs-sub;
  color: $text-body;
}
.sku-close {
  position: absolute;
  right: 0;
  top: 0;
  font-size: 36rpx;
  color: $text-placeholder;
}
.sku-body {
  flex: 1;
  max-height: 46vh;
}
.sku-group-title {
  display: block;
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
  margin: 16rpx 0;
}
.sku-options {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.sku-opt {
  padding: 16rpx 32rpx;
  background: $fill-secondary;
  border-radius: $radius-pill;
  font-size: $fs-sub;
  color: $text-body;
  border: 2rpx solid transparent;
}
.sku-opt.active {
  background: $brand-light;
  color: $brand;
  border-color: $brand;
}
.sku-opt.disabled {
  color: $text-placeholder;
  opacity: 0.5;
}
.qty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
}
.qty-label {
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
}
.stepper {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.step {
  width: 60rpx;
  height: 60rpx;
  background: $fill-secondary;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  color: $text-body;
}
.step.disabled {
  color: $text-placeholder;
}
.step-val {
  width: 88rpx;
  text-align: center;
  font-size: $fs-body;
}
.limit-tip {
  margin-top: 16rpx;
  font-size: $fs-aux;
  color: $text-weak;
}
.sku-confirm {
  margin-top: 24rpx;
  height: 88rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-title;
  font-weight: 600;
}
</style>
