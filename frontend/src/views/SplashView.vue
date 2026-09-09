<template>
  <div class="splash-page">
    <div class="glow glow-a" />
    <div class="glow glow-b" />

    <div class="splash-content">
      <div class="logo-shell">
        <img class="logo" src="/photo/logo-icon.png" alt="WordFlow" />
      </div>
      <h1 class="brand">
        <span class="brand-en">WordFlow</span>
        <span class="brand-cn">词流</span>
      </h1>
      <p class="slogan">把词说出口，记忆便有了回响</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

onMounted(() => {
  // 渐进展示约 2.6 秒后进入首页或登录页
  setTimeout(() => {
    const next = typeof route.query.next === 'string' ? route.query.next : ''
    const target = userStore.token
      ? '/'
      : next && next.startsWith('/')
        ? next
        : '/login'
    router.replace(target)
  }, 2600)
})
</script>

<style scoped>
.splash-page {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(160deg, #eef2ff 0%, #f8f9ff 45%, #fdf2f8 100%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.55;
  animation: drift 6s ease-in-out infinite alternate;
}

.glow-a {
  width: 300px;
  height: 300px;
  left: -90px;
  top: -80px;
  background: rgba(99, 102, 241, 0.35);
}

.glow-b {
  width: 260px;
  height: 260px;
  right: -70px;
  bottom: -70px;
  background: rgba(236, 72, 153, 0.25);
  animation-delay: -3s;
}

@keyframes drift {
  to {
    transform: translate(24px, 18px) scale(1.08);
  }
}

.splash-content {
  position: relative;
  text-align: center;
  padding: 24px;
}

.logo-shell {
  width: 108px;
  height: 108px;
  margin: 0 auto 22px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 44px rgba(79, 70, 229, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  animation: rise-in 0.75s ease both;
}

.logo {
  width: 84px;
  height: 84px;
  object-fit: cover;
  border-radius: 22px;
}

.brand {
  margin: 0;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 10px;
  animation: rise-in 0.75s ease 0.32s both;
}

.brand-en {
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: linear-gradient(120deg, #4f46e5, #7c3aed);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-cn {
  font-size: 20px;
  font-weight: 600;
  color: var(--wfl-text-secondary);
}

.slogan {
  margin: 14px 0 0;
  font-size: 15px;
  color: var(--wfl-text-secondary);
  letter-spacing: 2px;
  animation: rise-in 0.9s ease 0.72s both;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .glow,
  .logo-shell,
  .brand,
  .slogan {
    animation: none;
  }
}
</style>
