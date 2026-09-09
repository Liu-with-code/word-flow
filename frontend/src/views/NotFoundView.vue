<template>
  <div class="not-found">
    <div class="code">404</div>
    <h1>页面不存在</h1>
    <p>你访问的页面可能已被移动或删除。</p>
    <el-button type="primary" @click="router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--wfl-bg);
  text-align: center;
}

.code {
  font-size: 80px;
  font-weight: 900;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

h1 {
  margin: 0;
}

p {
  color: var(--wfl-text-secondary);
  margin-bottom: 16px;
}
</style>
