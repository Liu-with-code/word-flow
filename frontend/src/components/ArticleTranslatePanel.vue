<template>
  <div class="article-panel">
    <div class="article-card wfl-card">
      <div class="article-title">{{ title }}</div>
      <div class="article-content">{{ content }}</div>
      <div class="article-hint">请将上面这篇英文短文翻译成中文，提交后由 AI 批改。</div>
    </div>

    <div class="input-card wfl-card">
      <el-input
        v-model="text"
        type="textarea"
        :rows="8"
        placeholder="在这里输入你的中文翻译…"
        resize="none"
      />
      <div class="actions">
        <el-button type="primary" :loading="loading" :disabled="!text.trim()" @click="submit">
          {{ result && !result.passed ? '重新提交' : '提交翻译' }}
        </el-button>
      </div>
    </div>

    <div v-if="result" class="result-card wfl-card">
      <div class="result-head">
        <el-tag :type="result.passed ? 'success' : 'danger'" size="large" effect="dark">
          {{ result.passed ? '通过' : '未通过' }} · {{ result.score }} 分
        </el-tag>
        <span class="result-comment">{{ result.comment }}</span>
      </div>

      <template v-if="result.errors && result.errors.length">
        <div class="error-title">错误明细</div>
        <div v-for="(error, index) in result.errors" :key="index" class="error-item">
          <div class="error-row">
            <span class="error-label">期望表达</span>
            <span>{{ error.expected }}</span>
          </div>
          <div class="error-row">
            <span class="error-label">你的表达</span>
            <span class="error-user">{{ error.user }}</span>
          </div>
          <div class="error-row">
            <span class="error-label">修改建议</span>
            <span>{{ error.suggestion }}</span>
          </div>
        </div>
      </template>

      <el-collapse class="standard-collapse">
        <el-collapse-item title="查看标准译文">
          <div class="standard-zh">{{ result.standardZh }}</div>
        </el-collapse-item>
      </el-collapse>

      <div v-if="!result.passed" class="result-actions">
        <el-button @click="emit('retry')">重新翻译</el-button>
        <el-button type="primary" @click="emit('regenerate')">换一篇重练</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ArticleCheckResult } from '@/types/models'

withDefaults(
  defineProps<{
    title: string
    content: string
    result?: ArticleCheckResult | null
    loading?: boolean
  }>(),
  {
    result: null,
    loading: false,
  },
)

const emit = defineEmits<{
  (event: 'submit', translation: string): void
  (event: 'retry'): void
  (event: 'regenerate'): void
}>()

const text = ref('')

function submit() {
  if (text.value.trim()) {
    emit('submit', text.value.trim())
  }
}
</script>

<style scoped>
.article-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  padding: 24px;
}

.article-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
}

.article-content {
  font-size: 16px;
  line-height: 1.9;
  color: #2b2f3a;
  font-family: Georgia, 'Times New Roman', serif;
  white-space: pre-wrap;
}

.article-hint {
  margin-top: 14px;
  font-size: 13px;
  color: var(--wfl-text-secondary);
  border-top: 1px dashed var(--wfl-border);
  padding-top: 12px;
}

.input-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.result-comment {
  font-size: 14px;
  color: var(--wfl-text-secondary);
}

.error-title {
  margin: 18px 0 10px;
  font-weight: 700;
  font-size: 15px;
}

.error-item {
  border: 1px solid var(--wfl-border);
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
}

.error-row {
  display: flex;
  gap: 10px;
}

.error-label {
  flex-shrink: 0;
  color: var(--wfl-text-secondary);
  font-size: 12px;
  width: 64px;
}

.error-user {
  color: var(--wfl-danger);
}

.standard-collapse {
  margin-top: 14px;
}

.standard-zh {
  line-height: 1.9;
  font-size: 15px;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
</style>
