<template>
  <div class="auth-page">
    <span class="blob blob-1" />
    <span class="blob blob-2" />

    <div class="auth-card">
      <div class="brand-head">
        <img class="brand-logo" src="/photo/logo.jpg" alt="WordFlow" />
        <div class="brand-copy">
          <div class="brand-name">WordFlow 词流</div>
          <div class="brand-slogan">
            {{ slogan.text }}<span v-if="slogan.source"> —— {{ slogan.source }}</span>
          </div>
        </div>
      </div>

      <h1 class="auth-title">欢迎回来</h1>
      <p class="auth-subtitle">登录后继续你的学习计划</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            size="large"
            show-password
            placeholder="请输入密码"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <router-link class="auth-link" to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { randomQuote } from '@/utils/quotes'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const slogan = ref(randomQuote())

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: hidden;
  background: linear-gradient(150deg, #eef2ff 0%, #faf9ff 50%, #fdf2f8 100%);
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(72px);
  opacity: 0.5;
}

.blob-1 {
  width: 320px;
  height: 320px;
  left: -90px;
  top: -80px;
  background: rgba(99, 102, 241, 0.4);
}

.blob-2 {
  width: 300px;
  height: 300px;
  right: -80px;
  bottom: -90px;
  background: rgba(236, 72, 153, 0.28);
}

.auth-card {
  position: relative;
  width: 420px;
  max-width: 100%;
  padding: 34px 34px 28px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(79, 70, 229, 0.14);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.brand-logo {
  width: 60px;
  height: 52px;
  border-radius: 14px;
  object-fit: cover;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.18);
  flex-shrink: 0;
}

.brand-copy {
  min-width: 0;
}

.brand-name {
  font-size: 19px;
  font-weight: 800;
}

.brand-slogan {
  margin-top: 3px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--wfl-text-secondary);
}

.auth-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
}

.auth-subtitle {
  margin: 6px 0 20px;
  color: var(--wfl-text-secondary);
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  border-radius: 12px;
  font-weight: 700;
  letter-spacing: 6px;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: var(--wfl-text-secondary);
}

.auth-link {
  color: var(--wfl-primary);
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 480px) {
  .auth-page {
    padding: 14px;
  }

  .auth-card {
    padding: 26px 20px 24px;
    border-radius: 18px;
  }

  .brand-logo {
    width: 54px;
    height: 46px;
  }

  .auth-title {
    font-size: 23px;
  }
}
</style>
