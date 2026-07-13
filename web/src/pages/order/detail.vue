<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toYuan, formatTime } from '@/utils/format'
import { statusText, statusColor } from '@/utils/order-status'
import { toast, confirm } from '@/utils/toast'

const orderNo = ref('')
const order = ref(null)
const loading = ref(true)
const loadError = ref(false)

const st = computed(() => order.value?.status)

onLoad((opts) => {
  orderNo.value = opts.no
  load()
})

async function load() {
  loading.value = true
  loadError.value = false
  try {
    order.value = await api.orderDetail(orderNo.value)
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function pay() {
  try {
    await api.orderPay(orderNo.value)
    uni.redirectTo({ url: `/pages/order/pay-result?no=${orderNo.value}&status=success` })
  } catch (e) {}
}

async function cancel() {
  const ok = await confirm('确定取消该订单？')
  if (!ok) return
  await api.orderCancel(orderNo.value)
  toast('已取消')
  load()
}

async function receive() {
  const ok = await confirm('确认已收到货？')
  if (!ok) return
  await api.orderConfirm(orderNo.value)
  toast('已确认收货')
  load()
}

async function del() {
  const ok = await confirm('确定删除该订单？')
  if (!ok) return
  await api.orderDelete(orderNo.value)
  toast('已删除')
  setTimeout(() => uni.navigateBack(), 600)
}

function applyAfterSale() {
  uni.navigateTo({ url: `/pages/aftersale/apply?no=${orderNo.value}` })
}

function copyNo() {
  uni.setClipboardData({ data: orderNo.value })
}
</script>

<template>
  <k-empty v-if="!order && !loading" :error="loadError" text="订单不存在" @retry="load" />
  <view class="odetail" v-else-if="order">
    <scroll-view scroll-y class="scroll">
      <!-- 状态头 -->
      <view class="status-hero" :style="{ background: statusColor(st) }">
        <text class="sh-text">{{ statusText(st) }}</text>
        <text v-if="st === 0 && order.expireAt" class="sh-sub">请尽快完成支付</text>
        <text v-else-if="st === 2" class="sh-sub">商品已发出，请注意查收</text>
      </view>

      <!-- 收货地址 -->
      <view class="card address">
        <text class="a-icon">📍</text>
        <view class="a-body">
          <view class="a-line1">
            <text class="a-name">{{ order.receiverName }}</text>
            <text class="a-phone">{{ order.receiverPhone }}</text>
          </view>
          <text class="a-detail">{{ order.receiverAddress }}</text>
        </view>
      </view>

      <!-- 物流 -->
      <view v-if="order.logisticsNo" class="card logistics">
        <text class="l-company">{{ order.logisticsCompany || '物流' }}</text>
        <text class="l-no">运单号 {{ order.logisticsNo }}</text>
      </view>

      <!-- 商品 -->
      <view class="card goods">
        <view v-for="(it, i) in order.items" :key="i" class="g-row">
          <view class="g-img"><k-image :src="it.productImage" radius="24rpx" /></view>
          <view class="g-meta">
            <text class="g-name">{{ it.productName }}</text>
            <text class="g-sku">{{ it.skuName }}</text>
            <view class="g-bottom">
              <k-price :value="it.unitPrice" :size="30" />
              <text class="g-qty">×{{ it.quantity }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 金额 -->
      <view class="card amounts">
        <view class="amt"><text>商品金额</text><text>¥{{ toYuan(order.productAmount) }}</text></view>
        <view class="amt" v-if="Number(order.couponDiscount) > 0">
          <text>优惠券</text><text class="dc">−¥{{ toYuan(order.couponDiscount) }}</text>
        </view>
        <view class="amt"><text>运费</text><text>{{ Number(order.shippingFee) > 0 ? '¥' + toYuan(order.shippingFee) : '免运费' }}</text></view>
        <view class="amt total"><text>实付款</text><k-price :value="order.payAmount" :size="38" /></view>
      </view>

      <!-- 订单信息 -->
      <view class="card meta-info">
        <view class="mi-row"><text class="mi-k">订单编号</text><text class="mi-v" @tap="copyNo">{{ order.orderNo }} 复制</text></view>
        <view class="mi-row"><text class="mi-k">下单时间</text><text class="mi-v">{{ formatTime(order.createdAt) }}</text></view>
        <view class="mi-row" v-if="order.paidAt"><text class="mi-k">支付时间</text><text class="mi-v">{{ formatTime(order.paidAt) }}</text></view>
        <view class="mi-row" v-if="order.remark"><text class="mi-k">备注</text><text class="mi-v">{{ order.remark }}</text></view>
      </view>

      <view class="safe-bottom" />
    </scroll-view>

    <!-- 操作栏 -->
    <view class="action-bar">
      <template v-if="st === 0">
        <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="cancel">取消订单</view>
        <view class="btn primary" hover-class="hover-dim" :hover-stay-time="0" @tap="pay">去支付</view>
      </template>
      <template v-else-if="st === 2">
        <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="applyAfterSale">申请售后</view>
        <view class="btn primary" hover-class="hover-dim" :hover-stay-time="0" @tap="receive">确认收货</view>
      </template>
      <template v-else-if="st === 3">
        <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="applyAfterSale">申请售后</view>
        <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="del">删除订单</view>
      </template>
      <template v-else-if="st === 4 || st === 6">
        <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="del">删除订单</view>
      </template>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.odetail {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.scroll {
  flex: 1;
}
.status-hero {
  padding: 48rpx $safe-x 40rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.sh-text {
  color: #fff;
  font-size: 44rpx;
  font-weight: 700;
}
.sh-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: $fs-sub;
}
.card {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 24rpx;
}
.address {
  display: flex;
  gap: 16rpx;
}
.a-icon {
  font-size: 36rpx;
}
.a-body {
  flex: 1;
}
.a-line1 {
  display: flex;
  gap: 20rpx;
  align-items: baseline;
}
.a-name {
  font-size: $fs-title;
  font-weight: 600;
  color: $text-title;
}
.a-phone {
  font-size: $fs-sub;
  color: $text-body;
}
.a-detail {
  display: block;
  margin-top: 8rpx;
  font-size: $fs-sub;
  color: $text-weak;
  line-height: 1.5;
}
.logistics {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.l-company {
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
}
.l-no {
  font-size: $fs-sub;
  color: $text-weak;
}
.g-row {
  display: flex;
  gap: 20rpx;
  padding: 16rpx 0;
}
.g-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: $radius-img;
  background: $fill-secondary;
}
.g-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.g-name {
  font-size: $fs-body;
  color: $text-title;
  font-weight: 600;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.g-sku {
  margin-top: 8rpx;
  align-self: flex-start;
  font-size: $fs-aux;
  color: $text-weak;
  background: $fill-secondary;
  padding: 4rpx 14rpx;
  border-radius: $radius-tag;
}
.g-bottom {
  margin-top: auto;
  padding-top: 12rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.g-qty {
  font-size: $fs-sub;
  color: $text-weak;
}
.amt {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  font-size: $fs-sub;
  color: $text-body;
}
.amt .dc {
  color: $brand;
}
.amt.total {
  margin-top: 8rpx;
  padding-top: 20rpx;
  border-top: 2rpx solid $divider;
  align-items: baseline;
  font-weight: 600;
  color: $text-title;
}
.mi-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
}
.mi-k {
  font-size: $fs-sub;
  color: $text-weak;
}
.mi-v {
  font-size: $fs-sub;
  color: $text-body;
}
.safe-bottom {
  height: 40rpx;
}
.action-bar {
  height: calc(100rpx + env(safe-area-inset-bottom));
  padding: 0 $safe-x env(safe-area-inset-bottom);
  background: #fff;
  border-top: 2rpx solid $divider;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20rpx;
}
.btn {
  min-width: 160rpx;
  height: 72rpx;
  padding: 0 36rpx;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-body;
  font-weight: 600;
}
.btn.primary {
  background: $brand;
  color: #fff;
}
.btn.ghost {
  border: 2rpx solid $divider;
  color: $text-body;
}
</style>
