<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/api'
import { useCartStore } from '@/store/cart'
import { toYuan } from '@/utils/format'
import { toast, confirm } from '@/utils/toast'

const cartStore = useCartStore()
const view = ref(null)
const loading = ref(true)
const loadError = ref(false)
const editing = ref(false)

const items = computed(() => view.value?.items || [])
const allChecked = computed(() => items.value.length > 0 && items.value.every((i) => i.checked || i.invalid))
const checkedAmount = computed(() => view.value?.checkedAmount ?? 0)
const checkedCount = computed(() => view.value?.checkedCount ?? 0)

onShow(() => load())

async function load() {
  loading.value = true
  loadError.value = false
  try {
    view.value = await cartStore.refresh()
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function toggleItem(item) {
  if (item.invalid) return
  view.value = await api.cartUpdate({ id: item.id, checked: !item.checked })
  cartStore.apply(view.value)
}

async function toggleAll() {
  const target = !allChecked.value
  // 逐条更新有效项（后端无批量勾选接口）
  for (const it of items.value) {
    if (it.invalid) continue
    if (it.checked !== target) {
      view.value = await api.cartUpdate({ id: it.id, checked: target })
    }
  }
  cartStore.apply(view.value)
}

async function stepQty(item, delta) {
  const next = item.quantity + delta
  if (next < 1) return
  if (item.perOrderLimit && next > item.perOrderLimit) return toast(`每单限购${item.perOrderLimit}件`)
  if (item.stock && next > item.stock) return toast('库存不足')
  view.value = await api.cartUpdate({ id: item.id, quantity: next })
  cartStore.apply(view.value)
}

async function removeItems(ids) {
  const ok = await confirm('确定删除选中商品？')
  if (!ok) return
  view.value = await api.cartRemove(ids)
  cartStore.apply(view.value)
  toast('已删除')
}

function removeChecked() {
  const ids = items.value.filter((i) => i.checked).map((i) => i.id)
  if (!ids.length) return toast('请选择商品')
  removeItems(ids)
}

function submit() {
  const chosen = items.value.filter((i) => i.checked && !i.invalid)
  if (!chosen.length) return toast('请选择商品')
  const orderItems = chosen.map((i) => ({ skuId: i.skuId, quantity: i.quantity }))
  uni.navigateTo({
    url: `/pages/order/confirm?items=${encodeURIComponent(JSON.stringify(orderItems))}`,
  })
}

function toDetail(item) {
  uni.navigateTo({ url: `/pages/product/detail?id=${item.productId}` })
}
</script>

<template>
  <view class="cart-page">
    <view class="head">
      <text class="head-title">购物车</text>
      <text v-if="items.length" class="head-edit" @tap="editing = !editing">
        {{ editing ? '完成' : '管理' }}
      </text>
    </view>

    <scroll-view scroll-y class="scroll" v-if="items.length">
      <view v-for="item in items" :key="item.id" class="row" :class="{ invalid: item.invalid }">
        <view class="check" @tap="toggleItem(item)">
          <view class="checkbox" :class="{ on: item.checked && !item.invalid, dis: item.invalid }">
            <text v-if="item.checked && !item.invalid" class="tick">✓</text>
          </view>
        </view>
        <view class="thumb" @tap="toDetail(item)">
          <k-image :src="item.mainImage" radius="24rpx" />
        </view>
        <view class="meta">
          <text class="pname" @tap="toDetail(item)">{{ item.productName }}</text>
          <text class="sku">{{ item.skuName }}</text>
          <view v-if="item.invalid" class="badge-invalid">{{ item.soldOut ? '已售罄' : '已失效' }}</view>
          <view class="row-bottom">
            <k-price :value="item.price" :size="34" />
            <view class="stepper" v-if="!item.invalid">
              <view class="step" :class="{ disabled: item.quantity <= 1 }" @tap="stepQty(item, -1)">−</view>
              <text class="step-val">{{ item.quantity }}</text>
              <view class="step" @tap="stepQty(item, 1)">+</view>
            </view>
          </view>
        </view>
      </view>
      <view class="safe-bottom" />
    </scroll-view>

    <k-empty v-else-if="!loading" :error="loadError" text="购物车还是空的" @retry="load">
      <view class="go-shop" hover-class="hover-dim" :hover-stay-time="0" @tap="uni.switchTab({ url: '/pages/home/home' })">去逛逛</view>
    </k-empty>

    <!-- 结算栏 -->
    <view class="bar" v-if="items.length">
      <view class="bar-check" @tap="toggleAll">
        <view class="checkbox" :class="{ on: allChecked }">
          <text v-if="allChecked" class="tick">✓</text>
        </view>
        <text class="bar-all">全选</text>
      </view>

      <template v-if="!editing">
        <view class="bar-total">
          <text class="bt-label">合计</text>
          <k-price :value="checkedAmount" :size="40" />
        </view>
        <view class="bar-submit" :class="{ disabled: checkedCount === 0 }" hover-class="hover-dim" :hover-stay-time="0" @tap="submit">
          结算{{ checkedCount ? `(${checkedCount})` : '' }}
        </view>
      </template>
      <template v-else>
        <view class="bar-del" hover-class="hover-dim" :hover-stay-time="0" @tap="removeChecked">删除</view>
      </template>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.cart-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.head {
  padding: 24rpx $safe-x 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.head-title {
  font-size: $fs-display;
  font-weight: 700;
  color: $text-title;
}
.head-edit {
  font-size: $fs-body;
  color: $text-body;
}
.scroll {
  flex: 1;
}
.row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #fff;
  margin: 0 $safe-x 16rpx;
  padding: 20rpx;
  border-radius: $radius-card;
}
.row.invalid {
  opacity: 0.7;
}
.check {
  padding: 8rpx;
}
.checkbox {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid $text-placeholder;
  display: flex;
  align-items: center;
  justify-content: center;
}
.checkbox.on {
  background: $brand;
  border-color: $brand;
}
.checkbox.dis {
  background: $divider;
  border-color: $divider;
}
.tick {
  color: #fff;
  font-size: 26rpx;
}
.thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-img;
  background: $fill-secondary;
}
.meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.pname {
  font-size: $fs-body;
  color: $text-title;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.sku {
  margin-top: 8rpx;
  font-size: $fs-aux;
  color: $text-weak;
  background: $fill-secondary;
  align-self: flex-start;
  padding: 4rpx 14rpx;
  border-radius: $radius-tag;
}
.badge-invalid {
  margin-top: 8rpx;
  align-self: flex-start;
  font-size: $fs-tiny;
  color: $text-weak;
}
.row-bottom {
  margin-top: auto;
  padding-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stepper {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.step {
  width: 52rpx;
  height: 52rpx;
  background: $fill-secondary;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: $text-body;
}
.step.disabled {
  color: $text-placeholder;
}
.step-val {
  width: 72rpx;
  text-align: center;
  font-size: $fs-sub;
}
.go-shop {
  margin-top: 32rpx;
  padding: 16rpx 48rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  font-size: $fs-body;
  font-weight: 600;
}
.safe-bottom {
  height: 40rpx;
}
.bar {
  height: calc(100rpx + env(safe-area-inset-bottom));
  padding: 0 $safe-x env(safe-area-inset-bottom);
  background: #fff;
  border-top: 2rpx solid $divider;
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.bar-check {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.bar-all {
  font-size: $fs-body;
  color: $text-body;
}
.bar-total {
  flex: 1;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8rpx;
}
.bt-label {
  font-size: $fs-sub;
  color: $text-body;
}
.bar-submit {
  min-width: 200rpx;
  height: 76rpx;
  padding: 0 40rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-title;
  font-weight: 600;
}
.bar-submit.disabled {
  background: $text-placeholder;
}
.bar-del {
  flex: 1;
  height: 76rpx;
  border: 2rpx solid $brand;
  color: $brand;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-body;
  font-weight: 600;
}
</style>
