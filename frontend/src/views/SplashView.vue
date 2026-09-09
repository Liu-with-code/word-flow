<template>
  <div class="splash-page">
    <span class="glow glow-a" />
    <span class="glow glow-b" />

    <div class="splash-content">
      <div class="brand-stage" :class="{ in: logoIn }">
        <div class="logo-shell">
          <img class="logo" src="/photo/logo-icon.png" alt="WordFlow" />
        </div>
        <h1 class="brand">
          <span class="brand-en">WordFlow</span>
          <span class="brand-cn">词流</span>
        </h1>
      </div>

      <p class="slogan" :class="{ in: sloganIn }">让词句经由你，抵达记忆</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const logoIn = ref(false)
const sloganIn = ref(false)
let timers: number[] = []

onMounted(() => {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (reduced) {
    logoIn.value = true
    sloganIn.value = true
    timers.push(window.setTimeout(goNext, 1400))
    return
  }

  // 第一幕：Logo 与名称浮现；第二幕：标语浮现
  timers.push(window.setTimeout(() => (logoIn.value = true), 120))
  timers.push(window.setTimeout(() => (sloganIn.value = true), 950))
  timers.push(window.setTimeout(goNext, 2600))
})

onBeforeUnmount(() => {
  timers.forEach((t) => window.clearTimeout(t))
})

function goNext() {
  const next = typeof route.query.next === 'string' ? route.query.next : ''
  const target = userStore.token
    ? '/'
    : next && next.startsWith('/')
      ? next
      : '/login'
  router.replace(target)
}
</script>

<style scoped>
.splash-page {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(160deg, #eef2ff 0%, #faf9ff 45%, #fdf2f8 100%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(72px);
  opacity: 0.5;
  animation: drift 7s ease-in-out infinite alternate;
}

.glow-a {
  width: 320px;
  height: 320px;
  left: -100px;
  top: -90px;
  background: rgba(99, 102, 241, 0.35);
}

.glow-b {
  width: 280px;
  height: 280px;
  right: -80px;
  bottom: -80px;
  background: rgba(236, 72, 153, 0.22);
  animation-delay: -3.5s;
}

@keyframes drift {
  to {
    transform: translate(22px, 16px) scale(1.06);
  }
}

.splash-content {
  position: relative;
  text-align: center;
  padding: 24px;
}

.brand-stage {
  opacity: 0;
  transform: translateY(18px) scale(0.96);
  transition:
    opacity 0.75s ease,
    transform 0.85s cubic-bezier(0.22, 1, 0.36, 1);
}

.brand-stage.in {
  opacity: 1;
  transform: none;
}

.logo-shell {
  width: 112px;
  height: 112px;
  margin: 0 auto 22px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 46px rgba(79, 70, 229, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.logo {
  width: 88px;
  height: 88px;
  border-radius: 24px;
  object-fit: cover;
}

.brand {
  margin: 0;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 10px;
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
  margin: 18px 0 0;
  font-size: 15px;
  color: var(--wfl-text-secondary);
  letter-spacing: 3px;
  opacity: 0;
  transform: translateY(12px);
  transition:
    opacity 0.8s ease 0.05s,
    transform 0.9s cubic-bezier(0.22, 1, 0.36, 1);
}

.slogan.in {
  opacity: 1;
  transform: none;
}

@media (prefers-reduced-motion: reduce) {
  .glow {
    animation: none;
  }

  .brand-stage,
  .slogan {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
</style>
