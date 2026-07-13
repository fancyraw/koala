<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataLine,
  Goods,
  List,
  User,
  Ticket,
  Picture,
  Setting,
  Key,
  Fold,
  Expand,
  ArrowDown,
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(false)

const menus = computed(() => {
  const base = [
    { index: '/dashboard', title: '数据看板', icon: DataLine },
    { index: '/products', title: '商品管理', icon: Goods },
    { index: '/orders', title: '订单管理', icon: List },
    { index: '/users', title: '用户管理', icon: User },
    { index: '/coupons', title: '优惠券管理', icon: Ticket },
    { index: '/banners', title: '内容管理', icon: Picture },
    { index: '/settings', title: '系统设置', icon: Setting },
  ]
  if (auth.isSuper) {
    base.push({ index: '/admins', title: '管理员管理', icon: Key, divided: true })
  }
  return base
})

const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'dashboard'))
const currentTitle = computed(() => route.meta.title || '')

function onSelect(index) {
  router.push(index)
}

async function logout() {
  await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' }).catch(() => 'cancel').then((r) => {
    if (r === 'cancel') return Promise.reject()
  })
  await auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="layout">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="topbar-left">
        <el-icon class="collapse-btn" @click="collapsed = !collapsed">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
        <div class="logo">K</div>
        <span class="app-name">KOALA 管理后台</span>
      </div>
      <div class="topbar-right">
        <el-dropdown @command="(c) => c === 'logout' && logout()">
          <span class="admin-entry">
            <el-avatar :size="28" :src="auth.avatarUrl">{{ auth.nickname.slice(0, 1) }}</el-avatar>
            <span class="admin-name">{{ auth.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="body">
      <!-- 侧边导航 -->
      <aside class="sidebar" :class="{ collapsed }">
        <el-menu :default-active="activeMenu" :collapse="collapsed" :collapse-transition="false" @select="onSelect">
          <template v-for="m in menus" :key="m.index">
            <div v-if="m.divided" class="menu-divider" />
            <el-menu-item :index="m.index">
              <el-icon><component :is="m.icon" /></el-icon>
              <template #title>{{ m.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
        <div v-if="!collapsed" class="version">v1.0.0</div>
      </aside>

      <!-- 主内容 -->
      <main class="content">
        <el-breadcrumb class="crumb" separator="/">
          <el-breadcrumb-item>首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.topbar {
  height: 56px;
  background: $topbar-bg;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
}
.logo {
  width: 32px;
  height: 32px;
  background: $brand;
  color: #fff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.app-name {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}
.admin-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  cursor: pointer;
  outline: none;
}
.admin-name {
  font-size: 14px;
}
.body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.sidebar {
  width: 220px;
  background: $sidebar-bg;
  border-right: 1px solid $border;
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
  &.collapsed {
    width: 56px;
  }
  :deep(.el-menu) {
    border-right: none;
    flex: 1;
  }
}
.menu-divider {
  height: 1px;
  background: $border;
  margin: 8px 16px;
}
.version {
  padding: 12px 20px;
  font-size: 12px;
  color: $text-placeholder;
}
.content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px 24px;
}
.crumb {
  margin-bottom: 16px;
}
</style>
