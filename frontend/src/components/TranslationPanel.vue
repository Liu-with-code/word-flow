<template>
  <div class="translation-panel">
    <div class="sentence-card wfl-card">
      <div class="sentence-label">{{ direction === 'en2zh' ? '请翻译下面的英文' : '请翻译下面的中文' }}</div>
      <div class="sentence-text">{{ sentence }}</div>
    </div>

    <div class="input-card wfl-card">
      <el-input
        v-model="text"
        type="textarea"
        :rows="4"
        :placeholder="direction === 'en2zh' ? '输入你的中文翻译…' : '输入你的英文翻译…'"
        resize="none"
      />
      <div class="actions">
        <span v-if="result && result.correct" class="pass-tag">
          <el-icon><CircleCheckFilled /></el-icon> 已通过
        </span>
        <el-button :disabled="loading" plain @click="emit('hint')">我不会</el-button>
        <el-button type="primary" :loading="loading" :disabled="!text.trim()" @click="submit">
          {{ result && !result.correct ? '重新提交' : '提交翻译' }}
        </el-button>
      </div>
    </div>

    <!-- AI 实时批改过程 -->
    <div v-if="streaming || loading" class="streaming-box">
      <span class="streaming-label">AI 批改中</span>
      <span class="streaming-text">{{ streaming || '…' }}</span>
    </div>

    <!-- 参考译文（我不会） -->
    <el-alert
      v-if="hint"
      type="info"
      :closable="false"
      class="feedback"
      show-icon
    >
      <template #title>
        <div class="feedback-title">参考译文（大模型生成）</div>
        <div class="feedback-standard">{{ hint }}</div>
        <div class="feedback-note">先理解参考译文，然后再次尝试翻译。</div>
      </template>
    </el-alert>

    <el-alert
      v-if="result && !result.correct"
      type="error"
      :closable="false"
      class="feedback"
      show-icon
    >
      <template #title>
        <div class="feedback-title">
          {{ result.comment }}
          <span v-if="result.score !== null" class="score">得分 {{ result.score }}</span>
        </div>
        <div v-if="result.standard" class="feedback-standard">
          <div class="feedback-standard-label">参考译文</div>
          <div>{{ result.standard }}</div>
        </div>
      </template>
    </el-alert>

    <el-alert
      v-if="result && result.correct"
      type="success"
      :closable="false"
      class="feedback"
      show-icon
    >
      <template #title>
        <div class="feedback-title">
          {{ result.comment || '翻译通过！' }}
          <span v-if="result.score !== null" class="score">得分 {{ result.score }}</span>
        </div>
        <div v-if="result.standard" class="feedback-standard">
          <div class="feedback-standard-label">参考译文</div>
          <div>{{ result.standard }}</div>
        </div>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { StepResult } from '@/types/models'

const props = withDefaults(
  defineProps<{
    direction: 'en2zh' | 'zh2en'
    sentence: string
    result?: StepResult | null
    loading?: boolean
    streaming?: string
    hint?: string | null
  }>(),
  {
    result: null,
    loading: false,
    streaming: '',
    hint: null,
  },
)

const emit = defineEmits<{
  (event: 'submit', translation: string): void
  (event: 'hint'): void
}>()

const text = ref('')

function submit() {
  if (text.value.trim()) {
    emit('submit', text.value.trim())
  }
}
</script>

<style scoped>
.translation-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sentence-card {
  text-align: center;
}

.sentence-label {
  font-size: 13px;
  color: var(--wfl-text-secondary);
  margin-bottom: 12px;
}

.sentence-text {
  font-size: 18px;
  line-height: 1.7;
  font-weight: 600;
}

.input-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.pass-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--wfl-success);
  font-weight: 600;
  font-size: 14px;
}

.streaming-box {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: var(--wfl-primary-bg);
  border: 1px solid color-mix(in srgb, var(--wfl-primary) 20%, transparent);
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 13px;
}

.streaming-label {
  flex-shrink: 0;
  color: var(--wfl-primary);
  font-weight: 700;
  white-space: nowrap;
}

.streaming-text {
  color: var(--wfl-text);
  line-height: 1.6;
  white-space: pre-wrap;
}

.feedback-note {
  margin-top: 6px;
  font-weight: 400;
  font-size: 12px;
  opacity: 0.8;
}

.feedback-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.score {
  font-size: 12px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 999px;
  padding: 1px 8px;
}

.feedback-standard {
  margin-top: 6px;
  font-weight: 400;
  font-size: 13px;
  opacity: 0.9;
}

.feedback-standard-label {
  font-size: 12px;
  opacity: 0.75;
  margin-bottom: 4px;
}
</style>
