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

      <h1 class="auth-title">创建账号</h1>
      <p class="auth-subtitle">每天 20 个单词，用输出检验输入</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4-20 位字母、数字或下划线" size="large" />
        </el-form-item>
        <el-form-item label="昵称（可选）" prop="nickname">
          <el-input v-model="form.nickname" placeholder="你希望别人怎么称呼你" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6-32 位密码"
            size="large"
          />
        </el-form-item>
        <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="submit">
          注册并开始学习
        </el-button>
      </el-form>

      <div class="auth-footer">
        已有账号？
        <router-link class="auth-link" to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { randomQuote } from '@/utils/quotes'

const router = useRouter()
const userStore = useUserStore()
const slogan = ref(randomQuote())

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度须在 4-20 之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度须在 6-32 之间', trigger: 'blur' },
  ],
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.register(form.username, form.password, form.nickname)
    router.replace('/')
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
