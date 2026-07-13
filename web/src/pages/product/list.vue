<script setup>
import { ref, computed } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { api } from '@/api'

const categoryId = ref(null)
const keyword = ref('')
const list = ref([])
const page = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const loadError = ref(false)

const leftCol = computed(() => list.value.filter((_, i) => i % 2 === 0))
const rightCol = computed(() => list.value.filter((_, i) => i % 2 === 1))

onLoad((opts) => {
  if (opts.categoryId) categoryId.value = Number(opts.categoryId)
  if (opts.keyword) keyword.value = opts.keyword
  fetch(true)
})

async function fetch(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
    list.value = []
  }
  if (finished.value) return
  loading.value = true
  loadError.value = false
  try {
    const res = await api.products({
      categoryId: categoryId.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size,
    })
    total.value = res.total
    list.value = list.value.concat(res.list || [])
    if (list.value.length >= res.total || (res.list || []).length < size) {
      finished.value = true
    } else {
      page.value += 1
    }
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onReachBottom(() => fetch())

function onSearch() {
  fetch(true)
}
</script>

<template>
  <view class="list-page">
    <view class="searchbar">
      <view class="sb-input">
        <text class="sb-ico">🔍</text>
        <input
          v-model="keyword"
          class="sb-field"
          type="text"
          placeholder="搜索商品"
          placeholder-class="sb-ph"
          confirm-type="search"
          @confirm="onSearch"
        />
      </view>
    </view>

    <scroll-view scroll-y class="scroll">
      <view v-if="list.length" class="waterfall">
        <view class="wf-col">
          <k-product-card v-for="p in leftCol" :key="p.id" :product="p" />
        </view>
        <view class="wf-col">
          <k-product-card v-for="p in rightCol" :key="p.id" :product="p" />
        </view>
      </view>

      <k-skeleton v-else-if="loading" type="waterfall" :count="6" />
      <k-empty v-else :error="loadError" text="没有找到相关商品" @retry="fetch(true)" />

      <view v-if="loading && list.length" class="loading-tip">加载中…</view>
      <view v-else-if="finished && list.length" class="loading-tip">— 到底啦 —</view>
      <view class="safe-bottom" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.list-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.searchbar {
  padding: 16rpx $safe-x;
  background: $page-bg;
}
.sb-input {
  height: 68rpx;
  background: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  gap: 12rpx;
}
.sb-ico {
  font-size: 28rpx;
}
.sb-field {
  flex: 1;
  font-size: $fs-body;
  color: $text-body;
}
:deep(.sb-ph) {
  color: $text-placeholder;
}
.scroll {
  flex: 1;
}
.waterfall {
  display: flex;
  padding: 8rpx $safe-x 0;
  gap: 20rpx;
}
.wf-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.loading-tip {
  text-align: center;
  padding: 32rpx 0;
  font-size: $fs-aux;
  color: $text-placeholder;
}
.safe-bottom {
  height: 32rpx;
}
</style>
