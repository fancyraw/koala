<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toast } from '@/utils/toast'

const orderNo = ref('')
const reason = ref('')
const remark = ref('')
const evidenceImages = ref([])
const submitting = ref(false)

// 退款类型由订单状态自动判断，此处只列常见原因
const reasons = ['不想要了', '商品与描述不符', '质量问题', '发错货', '其他']

onLoad((opts) => {
  orderNo.value = opts.no || ''
})

async function submit() {
  if (submitting.value) return
  if (!reason.value) return toast('请选择退款原因')
  submitting.value = true
  try {
    await api.afterSaleApply({
      orderNo: orderNo.value,
      reason: reason.value,
      remark: remark.value || undefined,
      evidenceImages: evidenceImages.value.length ? evidenceImages.value : undefined,
    })
    toast('申请已提交')
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
    submitting.value = false
  }
}
</script>

<template>
  <view class="aftersale">
    <view class="tip">退款类型将根据订单状态自动判断（待发货仅退款 / 待收货退货退款）。</view>

    <view class="section">
      <text class="section-title">退款原因</text>
      <view class="reasons">
        <view
          v-for="r in reasons"
          :key="r"
          class="reason"
          :class="{ active: reason === r }"
          @tap="reason = r"
        >
          {{ r }}
        </view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">补充说明</text>
      <textarea v-model="remark" class="remark" placeholder="选填，补充问题描述" placeholder-class="ph" auto-height />
    </view>

    <view class="section">
      <text class="section-title">上传凭证<text class="section-sub">（选填，最多3张）</text></text>
      <view class="uploader-wrap">
        <k-uploader v-model="evidenceImages" :max="3" />
      </view>
    </view>

    <view class="submit-bar">
      <view class="submit-btn" :class="{ disabled: submitting }" hover-class="hover-dim" :hover-stay-time="0" @tap="submit">提交申请</view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.aftersale {
  min-height: 100vh;
  background: $page-bg;
  display: flex;
  flex-direction: column;
}
.tip {
  padding: 24rpx $safe-x;
  font-size: $fs-aux;
  color: $text-weak;
  line-height: 1.5;
}
.section {
  margin: 0 $safe-x 24rpx;
  background: #fff;
  border-radius: $radius-card;
  padding: 24rpx;
}
.section-title {
  font-size: $fs-body;
  font-weight: 600;
  color: $text-title;
}
.section-sub {
  font-size: $fs-aux;
  font-weight: 400;
  color: $text-weak;
}
.uploader-wrap {
  margin-top: 20rpx;
}
.reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-top: 20rpx;
}
.reason {
  padding: 16rpx 32rpx;
  background: $fill-secondary;
  border-radius: $radius-pill;
  font-size: $fs-sub;
  color: $text-body;
  border: 2rpx solid transparent;
}
.reason.active {
  background: $brand-light;
  color: $brand;
  border-color: $brand;
}
.remark {
  margin-top: 20rpx;
  width: 100%;
  min-height: 160rpx;
  font-size: $fs-body;
  color: $text-body;
}
:deep(.ph) {
  color: $text-placeholder;
}
.submit-bar {
  margin-top: auto;
  padding: 20rpx $safe-x calc(20rpx + env(safe-area-inset-bottom));
}
.submit-btn {
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
.submit-btn.disabled {
  opacity: 0.6;
}
</style>
