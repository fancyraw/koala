<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toYuan, formatTime } from '@/utils/format'
import { afterSaleStatusText, afterSaleStatusColor, canFillTracking } from '@/utils/aftersale-status'
import { toast, confirm } from '@/utils/toast'

const afterSaleNo = ref('')
const view = ref(null)
const loading = ref(true)
const loadError = ref(false)
const trackingInput = ref('')

const st = computed(() => view.value?.status)
const showTracking = computed(() => canFillTracking(view.value))

// 进度节点（退货退款完整链路；仅退款走 0→4）
const steps = computed(() => {
  if (!view.value) return []
  if (view.value.type === 1) {
    return [
      { label: '提交申请', done: st.value >= 0 },
      { label: '商家审核', done: st.value >= 4 || st.value === 5 },
      { label: '退款完成', done: st.value === 4 },
    ]
  }
  return [
    { label: '提交申请', done: st.value >= 0 },
    { label: '审核通过', done: st.value >= 1 },
    { label: '买家寄回', done: st.value >= 2 },
    { label: '商家收货', done: st.value >= 3 },
    { label: '退款完成', done: st.value >= 4 },
  ]
})

onLoad((opts) => {
  afterSaleNo.value = opts.no
  load()
})

async function load() {
  loading.value = true
  loadError.value = false
  try {
    view.value = await api.afterSaleDetail(afterSaleNo.value)
    trackingInput.value = view.value.returnTrackingNo || ''
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function saveTracking() {
  if (!trackingInput.value.trim()) return toast('请输入寄回单号')
  await api.afterSaleTracking(afterSaleNo.value, trackingInput.value.trim())
  toast('已提交')
  load()
}

async function cancel() {
  const ok = await confirm('确定撤销该售后申请？')
  if (!ok) return
  await api.afterSaleCancel(afterSaleNo.value)
  toast('已撤销')
  setTimeout(() => uni.navigateBack(), 600)
}

const typeText = (t) => (t === 1 ? '仅退款' : '退货退款')
</script>

<template>
  <k-empty v-if="!view && !loading" :error="loadError" text="售后单不存在" @retry="load" />
  <view class="as-detail" v-else-if="view">
    <scroll-view scroll-y class="scroll">
      <!-- 状态头 -->
      <view class="hero" :style="{ background: afterSaleStatusColor(st) }">
        <text class="hero-status">{{ afterSaleStatusText(st) }}</text>
        <text v-if="view.auditRemark" class="hero-sub">{{ view.auditRemark }}</text>
        <text v-else-if="st === 1" class="hero-sub">请尽快将商品寄回并填写单号</text>
      </view>

      <!-- 进度 -->
      <view class="card steps">
        <view v-for="(s, i) in steps" :key="i" class="step" :class="{ done: s.done }">
          <view class="dot" />
          <text class="step-label">{{ s.label }}</text>
          <view v-if="i < steps.length - 1" class="line" :class="{ done: steps[i + 1].done }" />
        </view>
      </view>

      <!-- 寄回单号 -->
      <view v-if="showTracking" class="card tracking">
        <text class="t-title">寄回单号</text>
        <view class="t-row">
          <input v-model="trackingInput" class="t-input" placeholder="填写快递单号" placeholder-class="ph" />
          <view class="t-btn" hover-class="hover-dim" :hover-stay-time="0" @tap="saveTracking">提交</view>
        </view>
      </view>

      <!-- 售后信息 -->
      <view class="card info">
        <view class="row"><text class="k">售后类型</text><text class="v">{{ typeText(view.type) }}</text></view>
        <view class="row"><text class="k">退款金额</text><text class="v amount">¥{{ toYuan(view.refundAmount) }}</text></view>
        <view class="row"><text class="k">退款原因</text><text class="v">{{ view.reason }}</text></view>
        <view class="row" v-if="view.remark"><text class="k">补充说明</text><text class="v">{{ view.remark }}</text></view>
        <view class="row"><text class="k">售后单号</text><text class="v">{{ view.afterSaleNo }}</text></view>
        <view class="row"><text class="k">关联订单</text><text class="v">{{ view.orderNo }}</text></view>
        <view class="row"><text class="k">申请时间</text><text class="v">{{ formatTime(view.createdAt) }}</text></view>
      </view>

      <!-- 凭证图 -->
      <view v-if="view.evidenceImages?.length" class="card evidence">
        <text class="e-title">凭证</text>
        <view class="e-imgs">
          <image
            v-for="(img, i) in view.evidenceImages"
            :key="i"
            class="e-img"
            :src="img"
            mode="aspectFill"
            @tap="uni.previewImage({ current: img, urls: view.evidenceImages })"
          />
        </view>
      </view>

      <view class="safe-bottom" />
    </scroll-view>

    <!-- 撤销（仅待审核） -->
    <view v-if="st === 0" class="action-bar">
      <view class="btn ghost" hover-class="hover-dim" :hover-stay-time="0" @tap="cancel">撤销申请</view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.as-detail {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.scroll {
  flex: 1;
}
.hero {
  padding: 48rpx $safe-x 40rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.hero-status {
  color: #fff;
  font-size: 44rpx;
  font-weight: 700;
}
.hero-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: $fs-sub;
}
.card {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 24rpx;
}
.steps {
  display: flex;
  padding: 40rpx 24rpx;
}
.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background: $text-placeholder;
  z-index: 1;
}
.step.done .dot {
  background: $brand;
}
.step-label {
  margin-top: 12rpx;
  font-size: $fs-tiny;
  color: $text-weak;
}
.step.done .step-label {
  color: $text-title;
}
.line {
  position: absolute;
  top: 11rpx;
  left: 50%;
  width: 100%;
  height: 4rpx;
  background: $divider;
}
.line.done {
  background: $brand;
}
.tracking {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.t-title {
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
}
.t-row {
  display: flex;
  gap: 20rpx;
  align-items: center;
}
.t-input {
  flex: 1;
  height: 72rpx;
  background: $fill-secondary;
  border-radius: $radius-sm;
  padding: 0 24rpx;
  font-size: $fs-sub;
  color: $text-body;
}
:deep(.ph) {
  color: $text-placeholder;
}
.t-btn {
  padding: 0 40rpx;
  height: 72rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  font-size: $fs-sub;
  font-weight: 600;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
}
.k {
  font-size: $fs-sub;
  color: $text-weak;
}
.v {
  font-size: $fs-sub;
  color: $text-body;
  max-width: 62%;
  text-align: right;
}
.v.amount {
  color: $brand;
  font-weight: 600;
}
.e-title {
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
}
.e-imgs {
  display: flex;
  gap: 16rpx;
  margin-top: 16rpx;
}
.e-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-img;
  background: $fill-secondary;
}
.safe-bottom {
  height: 40rpx;
}
.action-bar {
  padding: 20rpx $safe-x calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 2rpx solid $divider;
}
.btn {
  height: 88rpx;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-body;
  font-weight: 600;
}
.btn.ghost {
  border: 2rpx solid $divider;
  color: $text-body;
}
</style>
