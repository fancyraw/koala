<script setup>
import { ref } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { confirm, toast } from '@/utils/toast'

const list = ref([])
const loading = ref(true)
const loadError = ref(false)
const selectMode = ref(false)

onLoad((opts) => {
  selectMode.value = opts.select === '1'
})

onShow(() => load())

async function load() {
  loading.value = true
  loadError.value = false
  try {
    list.value = await api.addresses()
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function pick(addr) {
  if (!selectMode.value) return edit(addr.id)
  uni.setStorageSync('koala_selected_address', addr.id)
  uni.navigateBack()
}

function edit(id) {
  uni.navigateTo({ url: `/pages/address/edit${id ? '?id=' + id : ''}` })
}

async function del(addr) {
  const ok = await confirm('确定删除该地址？')
  if (!ok) return
  await api.addressDelete(addr.id)
  toast('已删除')
  load()
}
</script>

<template>
  <view class="addr-manage">
    <scroll-view scroll-y class="scroll" v-if="list.length">
      <view v-for="a in list" :key="a.id" class="addr" hover-class="hover-press" :hover-stay-time="0" @tap="pick(a)">
        <view class="a-body">
          <view class="a-line1">
            <text class="a-name">{{ a.name }}</text>
            <text class="a-phone">{{ a.phone }}</text>
            <text v-if="a.isDefault" class="a-default">默认</text>
          </view>
          <text class="a-detail">{{ a.fullAddress || a.detail }}</text>
        </view>
        <view class="a-actions">
          <text class="a-edit" @tap.stop="edit(a.id)">编辑</text>
          <text class="a-del" @tap.stop="del(a)">删除</text>
        </view>
      </view>
      <view class="safe-bottom" />
    </scroll-view>

    <k-empty v-else-if="!loading" :error="loadError" text="还没有收货地址" @retry="load" />

    <view class="add-bar">
      <view class="add-btn" hover-class="hover-dim" :hover-stay-time="0" @tap="edit()">＋ 新增收货地址</view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.addr-manage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.scroll {
  flex: 1;
}
.addr {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 28rpx 24rpx;
}
.a-line1 {
  display: flex;
  align-items: center;
  gap: 20rpx;
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
.a-default {
  font-size: $fs-tiny;
  color: $brand;
  background: $brand-light;
  padding: 2rpx 12rpx;
  border-radius: $radius-tag;
}
.a-detail {
  display: block;
  margin-top: 12rpx;
  font-size: $fs-sub;
  color: $text-weak;
  line-height: 1.5;
}
.a-actions {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 2rpx solid $divider;
  display: flex;
  justify-content: flex-end;
  gap: 40rpx;
}
.a-edit,
.a-del {
  font-size: $fs-aux;
  color: $text-weak;
}
.add-bar {
  padding: 20rpx $safe-x calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
}
.add-btn {
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
.safe-bottom {
  height: 32rpx;
}
</style>
