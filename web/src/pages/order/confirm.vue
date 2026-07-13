<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toYuan } from '@/utils/format'
import { toast } from '@/utils/toast'

let orderItems = [] // [{skuId, quantity}]
const preview = ref(null)
const loading = ref(true)
const submitting = ref(false)
const remark = ref('')
const addressId = ref(null)
// 后端 submit 需要 submitToken；preview 未返回，用前端幂等键防重复提交
const submitToken = ref('')

const address = computed(() => {
  if (!preview.value) return null
  if (!preview.value.receiverName) return null
  return {
    name: preview.value.receiverName,
    phone: preview.value.receiverPhone,
    detail: preview.value.receiverAddress,
  }
})

onLoad((opts) => {
  if (opts.items) {
    try {
      orderItems = JSON.parse(decodeURIComponent(opts.items))
    } catch (e) {
      orderItems = []
    }
  }
  submitToken.value = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
})

onShow(() => {
  // 从地址选择返回时会带上 selectedAddressId
  const picked = uni.getStorageSync('koala_selected_address')
  if (picked) {
    addressId.value = picked
    uni.removeStorageSync('koala_selected_address')
  }
  loadPreview()
})

async function loadPreview() {
  loading.value = true
  try {
    preview.value = await api.orderPreview({
      items: orderItems,
      addressId: addressId.value || undefined,
    })
    if (preview.value.addressId) addressId.value = preview.value.addressId
  } finally {
    loading.value = false
  }
}

function chooseAddress() {
  uni.navigateTo({ url: '/pages/address/manage?select=1' })
}

async function submit() {
  if (submitting.value) return
  if (!addressId.value) return toast('请选择收货地址')
  submitting.value = true
  uni.showLoading({ title: '提交中', mask: true })
  try {
    const res = await api.orderSubmit({
      items: orderItems,
      addressId: addressId.value,
      submitToken: submitToken.value,
      remark: remark.value || undefined,
    })
    // 直接拉起支付
    await api.orderPay(res.orderNo)
    uni.hideLoading()
    uni.redirectTo({
      url: `/pages/order/pay-result?no=${res.orderNo}&status=success&amount=${res.payAmount}`,
    })
  } catch (e) {
    uni.hideLoading()
    // 提交成功但支付失败时，跳去订单详情让用户重试
    submitting.value = false
  }
}
</script>

<template>
  <view class="confirm" v-if="preview">
    <scroll-view scroll-y class="scroll">
      <!-- 地址 -->
      <view class="address-card" hover-class="hover-dim" :hover-stay-time="0" @tap="chooseAddress">
        <template v-if="address">
          <view class="addr-main">
            <text class="addr-name">{{ address.name }}</text>
            <text class="addr-phone">{{ address.phone }}</text>
          </view>
          <text class="addr-detail">{{ address.detail }}</text>
        </template>
        <view v-else class="addr-empty">
          <text class="addr-empty-txt">＋ 请选择收货地址</text>
        </view>
        <text class="addr-arrow">›</text>
      </view>

      <!-- 商品清单 -->
      <view class="goods-card">
        <view v-for="(it, i) in preview.items" :key="i" class="goods-row">
          <view class="goods-img"><k-image :src="it.productImage" radius="24rpx" /></view>
          <view class="goods-meta">
            <text class="goods-name">{{ it.productName }}</text>
            <text class="goods-sku">{{ it.skuName }}</text>
            <view class="goods-bottom">
              <k-price :value="it.unitPrice" :size="32" />
              <text class="goods-qty">×{{ it.quantity }}</text>
            </view>
          </view>
        </view>

        <view class="remark-row">
          <text class="remark-label">备注</text>
          <input v-model="remark" class="remark-input" placeholder="选填，给商家留言" placeholder-class="ph" />
        </view>
      </view>

      <!-- 凑单提示 -->
      <view v-if="preview.upsell" class="upsell" @tap="uni.switchTab({ url: '/pages/home/home' })">
        <text class="upsell-txt">
          再买 ¥{{ toYuan(preview.upsell.needMore) }} 可用「{{ preview.upsell.couponName }}」再省 ¥{{ toYuan(preview.upsell.extraSave) }}
        </text>
        <text class="upsell-arrow">去凑单 ›</text>
      </view>

      <!-- 金额明细 -->
      <view class="amount-card">
        <view class="amt-row">
          <text class="amt-label">商品金额</text>
          <text class="amt-val">¥{{ toYuan(preview.productAmount) }}</text>
        </view>
        <view class="amt-row" v-if="Number(preview.couponDiscount) > 0">
          <text class="amt-label">优惠券</text>
          <text class="amt-val discount">−¥{{ toYuan(preview.couponDiscount) }}</text>
        </view>
        <view class="amt-row">
          <text class="amt-label">运费</text>
          <text class="amt-val">{{ Number(preview.shippingFee) > 0 ? '¥' + toYuan(preview.shippingFee) : '免运费' }}</text>
        </view>
        <view v-if="preview.appliedCoupons?.length" class="applied">
          <text v-for="c in preview.appliedCoupons" :key="c.userCouponId" class="applied-tag">
            {{ c.couponName }}
          </text>
        </view>
      </view>

      <view class="safe-bottom" />
    </scroll-view>

    <!-- 底部提交栏 -->
    <view class="pay-bar">
      <view class="pay-total">
        <text class="pt-label">实付</text>
        <k-price :value="preview.payAmount" :size="44" />
      </view>
      <view class="pay-btn" :class="{ disabled: submitting }" hover-class="hover-dim" :hover-stay-time="0" @tap="submit">提交订单</view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.confirm {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.scroll {
  flex: 1;
}
.address-card {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 32rpx 24rpx;
  display: flex;
  flex-direction: column;
  position: relative;
}
.addr-main {
  display: flex;
  align-items: baseline;
  gap: 20rpx;
}
.addr-name {
  font-size: $fs-title;
  font-weight: 600;
  color: $text-title;
}
.addr-phone {
  font-size: $fs-sub;
  color: $text-body;
}
.addr-detail {
  margin-top: 12rpx;
  font-size: $fs-sub;
  color: $text-weak;
  line-height: 1.5;
  padding-right: 40rpx;
}
.addr-empty-txt {
  font-size: $fs-body;
  color: $text-body;
}
.addr-arrow {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 40rpx;
  color: $text-placeholder;
}
.goods-card {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 8rpx 24rpx;
}
.goods-row {
  display: flex;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 2rpx solid $divider;
}
.goods-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: $radius-img;
  background: $fill-secondary;
}
.goods-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.goods-name {
  font-size: $fs-body;
  color: $text-title;
  font-weight: 600;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.goods-sku {
  margin-top: 8rpx;
  align-self: flex-start;
  font-size: $fs-aux;
  color: $text-weak;
  background: $fill-secondary;
  padding: 4rpx 14rpx;
  border-radius: $radius-tag;
}
.goods-bottom {
  margin-top: auto;
  padding-top: 12rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.goods-qty {
  font-size: $fs-sub;
  color: $text-weak;
}
.remark-row {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
}
.remark-label {
  font-size: $fs-body;
  color: $text-body;
  width: 100rpx;
}
.remark-input {
  flex: 1;
  font-size: $fs-body;
  text-align: right;
  color: $text-title;
}
:deep(.ph) {
  color: $text-placeholder;
}
.upsell {
  margin: 24rpx $safe-x 0;
  background: $brand-light;
  border-radius: $radius-sm;
  padding: 20rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.upsell-txt {
  flex: 1;
  font-size: $fs-sub;
  color: $brand;
  font-weight: 600;
}
.upsell-arrow {
  font-size: $fs-sub;
  color: $brand;
}
.amount-card {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 24rpx;
}
.amt-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}
.amt-label {
  font-size: $fs-sub;
  color: $text-body;
}
.amt-val {
  font-size: $fs-sub;
  color: $text-title;
}
.amt-val.discount {
  color: $brand;
}
.applied {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 12rpx;
}
.applied-tag {
  font-size: $fs-tiny;
  color: $brand;
  background: $brand-light;
  padding: 4rpx 14rpx;
  border-radius: $radius-tag;
}
.safe-bottom {
  height: 40rpx;
}
.pay-bar {
  height: calc(100rpx + env(safe-area-inset-bottom));
  padding: 0 $safe-x env(safe-area-inset-bottom);
  background: #fff;
  border-top: 2rpx solid $divider;
  display: flex;
  align-items: center;
}
.pay-total {
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}
.pt-label {
  font-size: $fs-sub;
  color: $text-body;
}
.pay-btn {
  min-width: 240rpx;
  height: 76rpx;
  padding: 0 48rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-title;
  font-weight: 600;
}
.pay-btn.disabled {
  background: $text-placeholder;
}
</style>
