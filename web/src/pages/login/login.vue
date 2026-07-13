<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/auth'
import { toast } from '@/utils/toast'

const auth = useAuthStore()
const logining = ref(false)

async function login() {
  if (logining.value) return
  logining.value = true
  try {
    await auth.login()
    toast('登录成功')
    setTimeout(() => {
      const pages = getCurrentPages()
      if (pages.length > 1) uni.navigateBack()
      else uni.switchTab({ url: '/pages/home/home' })
    }, 600)
  } catch (e) {
    logining.value = false
  }
}
</script>

<template>
  <view class="login">
    <k-navbar :back="true" bg="transparent" />
    <view class="brand">
      <text class="logo">Koala</text>
      <text class="slogan">发现生活里的好物</text>
    </view>
    <view class="actions">
      <view class="wx-btn" :class="{ disabled: logining }" hover-class="hover-dim" :hover-stay-time="0" @tap="login">
        <text class="wx-ico"></text>
        微信一键登录
      </view>
      <text class="terms">登录即代表同意《用户协议》与《隐私政策》</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
}
.brand {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
}
.logo {
  font-size: 88rpx;
  font-weight: 700;
  color: $brand;
  letter-spacing: 2rpx;
}
.slogan {
  font-size: $fs-body;
  color: $text-weak;
}
.actions {
  padding: 0 64rpx 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32rpx;
}
.wx-btn {
  width: 100%;
  height: 92rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: $fs-title;
  font-weight: 600;
}
.wx-btn.disabled {
  opacity: 0.6;
}
.terms {
  font-size: $fs-tiny;
  color: $text-placeholder;
}
</style>
