<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { useCartStore } from './store/cart'
import { useAuthStore } from './store/auth'

onLaunch(() => {
  const auth = useAuthStore()
  auth.restore()
  if (auth.token) {
    useCartStore().refreshBadge()
  }
})
</script>

<style lang="scss">
/* 全局基础样式，token 见 uni.scss */
page {
  background-color: $page-bg;
  color: $text-body;
  font-size: 28rpx;
  font-family: -apple-system, 'PingFang SC', 'SF Pro', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.5;
}

view,
text,
button,
input,
scroll-view,
image {
  box-sizing: border-box;
}

button::after {
  border: none;
}

image {
  display: block;
  will-change: transform;
}

/* 全局点击态（hover-class 引用的类须非 scoped，故置于此）。
   hover-press：按下微缩+压暗，适合卡片/列表行；
   hover-dim：仅压暗，适合实心主按钮（缩放会露出下层背景）。 */
.hover-press {
  transform: scale(0.97);
  opacity: 0.9;
  transition: transform 0.1s ease, opacity 0.1s ease;
}
.hover-dim {
  opacity: 0.8;
  transition: opacity 0.1s ease;
}
</style>
