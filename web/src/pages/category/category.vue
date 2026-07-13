<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'

const categories = ref([])
const activeId = ref(null)
const products = ref([])
const loading = ref(false)
const loadError = ref(false)

const leftCol = computed(() => products.value.filter((_, i) => i % 2 === 0))
const rightCol = computed(() => products.value.filter((_, i) => i % 2 === 1))

onLoad(() => loadCategories())

async function loadCategories() {
  try {
    categories.value = await api.categories()
    if (categories.value.length) {
      activeId.value = categories.value[0].id
      loadProducts()
    }
  } catch (e) {
    loadError.value = true
  }
}

function pick(id) {
  if (activeId.value === id) return
  activeId.value = id
  loadProducts()
}

async function loadProducts() {
  loading.value = true
  loadError.value = false
  try {
    const res = await api.products({ categoryId: activeId.value, page: 1, size: 30 })
    products.value = res.list || []
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function retry() {
  if (categories.value.length) loadProducts()
  else loadCategories()
}
</script>

<template>
  <view class="category">
    <view class="rail">
      <scroll-view scroll-y class="rail-scroll">
        <view
          v-for="c in categories"
          :key="c.id"
          class="rail-item"
          :class="{ active: activeId === c.id }"
          hover-class="hover-dim"
          :hover-stay-time="0"
          @tap="pick(c.id)"
        >
          <text class="rail-txt">{{ c.name }}</text>
        </view>
      </scroll-view>
    </view>

    <scroll-view scroll-y class="content">
      <view v-if="products.length" class="waterfall">
        <view class="wf-col">
          <k-product-card v-for="p in leftCol" :key="p.id" :product="p" />
        </view>
        <view class="wf-col">
          <k-product-card v-for="p in rightCol" :key="p.id" :product="p" />
        </view>
      </view>
      <k-skeleton v-else-if="loading" type="waterfall" :count="6" />
      <k-empty v-else :error="loadError" text="该分类暂无商品" @retry="retry" />
      <view class="safe-bottom" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.category {
  height: 100vh;
  display: flex;
  background: $page-bg;
}
.rail {
  width: 180rpx;
  background: #fff;
  flex-shrink: 0;
}
.rail-scroll {
  height: 100%;
}
.rail-item {
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.rail-item.active {
  background: $page-bg;
}
.rail-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 28rpx;
  bottom: 28rpx;
  width: 6rpx;
  background: $brand;
  border-radius: 0 6rpx 6rpx 0;
}
.rail-txt {
  font-size: $fs-sub;
  color: $text-body;
}
.rail-item.active .rail-txt {
  color: $brand;
  font-weight: 600;
}
.content {
  flex: 1;
  height: 100%;
}
.waterfall {
  display: flex;
  padding: 16rpx 16rpx 0;
  gap: 16rpx;
}
.wf-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.safe-bottom {
  height: 32rpx;
}
</style>
