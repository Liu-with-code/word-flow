<template>
  <el-container class="layout">
    <!-- 桌面端侧边栏 -->
    <el-aside class="sidebar" width="228px">
      <div class="brand">
        <img class="brand-logo" src="/photo/logo.jpg" alt="WordFlow" />
        <div>
          <div class="brand-name">WordFlow</div>
          <div class="brand-slogan" :title="slogan.text">{{ slogan.text }}</div>
        </div>
      </div>

      <el-menu :default-active="activePath" router class="menu">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-user">
        <el-avatar :size="34" :src="userStore.user?.avatarUrl || undefined">
          {{ avatarText }}
        </el-avatar>
        <div class="sidebar-user-info">
          <div class="nickname">{{ userStore.user?.nickname || '未登录' }}</div>
          <div class="username">@{{ userStore.user?.username }}</div>
        </div>
      </div>
    </el-aside>

    <!-- 主内容 -->
    <el-container class="main-container">
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <!-- 移动端底部导航 -->
    <nav class="bottom-nav">
      <router-link
        v-for="item in mobileNavItems"
        :key="item.path"
        :to="item.path"
        class="bottom-nav-item"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { randomShortQuote } from '@/utils/quotes'

const route = useRoute()
const userStore = useUserStore()
const slogan = ref(randomShortQuote())

const navItems = [
  { path: '/', label: '首页', icon: 'HomeFilled' },
  { path: '/learn', label: '今日学习', icon: 'Reading' },
  { path: '/review', label: '复习', icon: 'Refresh' },
  { path: '/words', label: '词书', icon: 'Collection' },
  { path: '/stats', label: '统计', icon: 'DataAnalysis' },
  { path: '/settings', label: '设置', icon: 'Setting' },
]

const mobileNavItems = navItems

const activePath = computed(() => route.path)
const avatarText = computed(() => {
  const nickname = userStore.user?.nickname || '词'
  return nickname.slice(0, 1).toUpperCase()
})
</script>

<style scoped>
.layout {
  height: 100%;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-right: 1px solid var(--wfl-border);
  padding: 20px 12px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 20px;
}

.brand-logo {
  width: 44px;
  height: 38px;
  border-radius: 12px;
  object-fit: cover;
  mix-blend-mode: multiply;
  background: none;
  box-shadow: 0 2px 8px rgba(31, 35, 51, 0.12);
  transition: transform 0.2s ease;
}

.brand-logo:hover {
  transform: scale(1.06);
}

.brand-name {
  font-weight: 700;
  font-size: 16px;
}

.brand-slogan {
  font-size: 12px;
  color: var(--wfl-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

.menu {
  border-right: none;
  flex: 1;
}

.menu .el-menu-item {
  border-radius: 10px;
  margin-bottom: 4px;
  height: 44px;
}

.menu .el-menu-item.is-active {
  background: var(--wfl-primary-bg);
  color: var(--wfl-primary);
  font-weight: 600;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 10px 0;
  border-top: 1px solid var(--wfl-border);
}

.sidebar-user-info {
  min-width: 0;
}

.nickname {
  font-size: 14px;
  font-weight: 600;
}

.username {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.main-container {
  min-width: 0;
}

.main-content {
  padding: 0;
}

.bottom-nav {
  display: none;
}

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }

  .bottom-nav {
    display: flex;
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 10;
    background: #fff;
    border-top: 1px solid var(--wfl-border);
    padding: 6px 0 calc(6px + env(safe-area-inset-bottom));
  }

  .bottom-nav-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    font-size: 11px;
    color: var(--wfl-text-secondary);
    text-decoration: none;
  }

  .bottom-nav-item.router-link-active {
    color: var(--wfl-primary);
  }
}

@media (max-width: 380px) {
  .bottom-nav-item {
    font-size: 10px;
  }

  .bottom-nav-item .el-icon {
    font-size: 18px;
  }
}
</style>
