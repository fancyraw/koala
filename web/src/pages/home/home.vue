<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '@/api'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const loading = ref(true)
const loadError = ref(false)
const banners = ref([])
const categories = ref([])
const hotSelling = ref([])
const recommended = ref([])
const grantedCoupons = ref([])

const sysInfo = uni.getSystemInfoSync()
const statusBarHeight = sysInfo.statusBarHeight || 20

// 瀑布流分两列
const leftCol = computed(() => recommended.value.filter((_, i) => i % 2 === 0))
const rightCol = computed(() => recommended.value.filter((_, i) => i % 2 === 1))

async function load() {
  loadError.value = false
  try {
    // 首页需登录（后端会自动发券）。未登录先静默登录（mock）。
    if (!auth.isLogin) {
      await auth.login()
    }
    const view = await api.home()
    banners.value = view.banners || []
    categories.value = view.categories || []
    hotSelling.value = view.hotSelling || []
    recommended.value = view.recommended || []
    grantedCoupons.value = view.grantedCoupons || []
  } catch (e) {
    // 登录失败等，交由 request 层 toast；此处记录错误态用于兜底展示
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onShow(() => {
  if (loading.value) load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

function toList(categoryId) {
  const q = categoryId ? `?categoryId=${categoryId}` : ''
  uni.navigateTo({ url: `/pages/product/list${q}` })
}

function toDetail(id) {
  uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
}
</script>

<template>
  <view class="home">
    <!-- 沉浸式头部 -->
    <view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="header-inner">
        <text class="logo">Koala</text>
        <view class="search" hover-class="hover-dim" :hover-stay-time="0" @tap="toList()">
          <text class="search-ico">🔍</text>
          <text class="search-ph">搜索你想要的好物</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll" :style="{ paddingTop: statusBarHeight + 44 + 'px' }">
      <!-- Banner -->
      <swiper
        v-if="banners.length"
        class="banner"
        circular
        autoplay
        :interval="4000"
        indicator-dots
        indicator-active-color="#ff2442"
        indicator-color="rgba(255,255,255,.5)"
      >
        <swiper-item v-for="b in banners" :key="b.id">
          <image class="banner-img" :src="b.imageUrl" mode="aspectFill" />
        </swiper-item>
      </swiper>

      <!-- 发券横幅 -->
      <view v-if="grantedCoupons.length" class="coupon-strip" hover-class="hover-dim" :hover-stay-time="0" @tap="uni.navigateTo({ url: '/pages/coupon/mine' })">
        <text class="cs-ico">🎁</text>
        <text class="cs-txt">已为你发放 {{ grantedCoupons.length }} 张优惠券，去看看</text>
        <text class="cs-arrow">›</text>
      </view>

      <!-- 分类金刚区 -->
      <view v-if="categories.length" class="cats">
        <view v-for="c in categories" :key="c.id" class="cat" hover-class="hover-press" :hover-stay-time="0" @tap="toList(c.id)">
          <image v-if="c.iconUrl" class="cat-ico" :src="c.iconUrl" mode="aspectFill" />
          <view v-else class="cat-ico cat-ico-ph">{{ c.name.slice(0, 1) }}</view>
          <text class="cat-name">{{ c.name }}</text>
        </view>
      </view>

      <!-- 热销 -->
      <view v-if="hotSelling.length" class="section">
        <view class="section-head">
          <text class="section-title">🔥 热销好物</text>
        </view>
        <scroll-view scroll-x class="hot-scroll" show-scrollbar="false">
          <view class="hot-row">
            <view v-for="p in hotSelling" :key="p.id" class="hot-card" hover-class="hover-press" :hover-stay-time="0" @tap="toDetail(p.id)">
              <view class="hot-img"><k-image :src="p.mainImage" /></view>
              <text class="hot-name">{{ p.name }}</text>
              <k-price :value="p.minPrice" :size="30" />
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 推荐瀑布流 -->
      <view class="section">
        <view class="section-head">
          <text class="section-title">为你推荐</text>
        </view>
        <view v-if="recommended.length" class="waterfall">
          <view class="wf-col">
            <k-product-card v-for="p in leftCol" :key="p.id" :product="p" class="wf-item" />
          </view>
          <view class="wf-col">
            <k-product-card v-for="p in rightCol" :key="p.id" :product="p" class="wf-item" />
          </view>
        </view>
        <k-skeleton v-else-if="loading" type="waterfall" :count="6" />
        <k-empty v-else :error="loadError" text="暂无推荐商品" @retry="load" />
      </view>

      <view class="safe-bottom" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
.home {
  min-height: 100vh;
  background: $page-bg;
}
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  background: $page-bg;
}
.header-inner {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 $safe-x;
  gap: 20rpx;
}
.logo {
  font-size: 40rpx;
  font-weight: 700;
  color: $brand;
  letter-spacing: 1rpx;
}
.search {
  flex: 1;
  height: 64rpx;
  background: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  gap: 10rpx;
}
.search-ico {
  font-size: 26rpx;
}
.search-ph {
  font-size: $fs-sub;
  color: $text-placeholder;
}
.scroll {
  height: 100vh;
  box-sizing: border-box;
}
.banner {
  margin: 16rpx $safe-x 0;
  height: 300rpx;
  border-radius: $radius-card;
  overflow: hidden;
}
.banner-img {
  width: 100%;
  height: 100%;
}
.coupon-strip {
  margin: 20rpx $safe-x 0;
  height: 76rpx;
  background: $brand-light;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  gap: 12rpx;
  .cs-ico {
    font-size: 32rpx;
  }
  .cs-txt {
    flex: 1;
    font-size: $fs-sub;
    color: $brand;
    font-weight: 600;
  }
  .cs-arrow {
    color: $brand;
    font-size: 32rpx;
  }
}
.cats {
  margin-top: 24rpx;
  padding: 24rpx $safe-x 8rpx;
  background: #fff;
  border-radius: $radius-card;
  margin-left: $safe-x;
  margin-right: $safe-x;
  display: flex;
  flex-wrap: wrap;
}
.cat {
  width: 20%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24rpx;
}
.cat-ico {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: $fill-secondary;
}
.cat-ico-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-weak;
  font-size: 32rpx;
  font-weight: 600;
}
.cat-name {
  margin-top: 12rpx;
  font-size: $fs-aux;
  color: $text-body;
}
.section {
  margin-top: 24rpx;
}
.section-head {
  padding: 8rpx $safe-x 16rpx;
}
.section-title {
  font-size: $fs-display;
  font-weight: 700;
  color: $text-title;
}
.hot-scroll {
  white-space: nowrap;
}
.hot-row {
  display: inline-flex;
  padding: 0 $safe-x;
  gap: 20rpx;
}
.hot-card {
  width: 220rpx;
  background: #fff;
  border-radius: $radius-card;
  overflow: hidden;
  padding-bottom: 16rpx;
}
.hot-img {
  width: 220rpx;
  height: 220rpx;
}
.hot-name {
  display: block;
  padding: 12rpx 16rpx 8rpx;
  font-size: $fs-aux;
  color: $text-title;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.hot-card :deep(.k-price) {
  padding-left: 16rpx;
}
.waterfall {
  display: flex;
  padding: 0 $safe-x;
  gap: 20rpx;
}
.wf-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.safe-bottom {
  height: 40rpx;
}
</style>
