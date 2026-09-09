<template>
  <div class="auth-page">
    <div class="auth-card wfl-card">
      <div class="auth-brand">
        <img class="brand-logo" src="/photo/logo.jpg" alt="WordFlow" />
        <div>
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
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            size="large"
          />
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
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(1000px 500px at 85% -10%, rgba(124, 58, 237, 0.14), transparent),
    radial-gradient(800px 400px at -10% 110%, rgba(79, 70, 229, 0.12), transparent),
    var(--wfl-bg);
}

.auth-card {
  width: 420px;
  max-width: 100%;
  padding: 36px 32px 28px;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand-logo {
  width: 60px;
  height: 52px;
  border-radius: 14px;
  object-fit: cover;
  mix-blend-mode: multiply;
  background: none;
  box-shadow: 0 3px 10px rgba(31, 35, 51, 0.1);
}

.brand-name {
  font-size: 17px;
  font-weight: 700;
}

.brand-slogan {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.auth-title {
  margin: 0;
  font-size: 26px;
}

.auth-subtitle {
  margin: 6px 0 22px;
  color: var(--wfl-text-secondary);
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
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
    padding: 16px;
  }

  .auth-card {
    padding: 28px 20px 24px;
  }
}
</style>
