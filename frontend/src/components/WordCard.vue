<template>
  <div class="word-card wfl-card">
    <div class="word-head">
      <span class="word-badge">{{ word.partOfSpeech || 'word' }}</span>
      <span class="word-level">{{ word.level }}</span>
    </div>
    <div class="word-main">
      <div class="word-text">{{ word.word }}</div>
      <div class="word-phonetic">{{ word.phonetic }}</div>
      <div class="word-chinese">{{ word.chinese }}</div>
    </div>
    <div v-if="mode === 'preview'" class="word-examples">
      <div class="example">
        <span class="example-en">{{ word.exampleEn }}</span>
        <span class="example-zh">{{ word.exampleZh }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Word } from '@/types/models'

withDefaults(
  defineProps<{
    word: Word
    mode?: 'preview' | 'summary'
  }>(),
  {
    mode: 'preview',
  },
)
</script>

<style scoped>
.word-card {
  text-align: center;
  padding: 28px 24px;
}

.word-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.word-badge,
.word-level {
  font-size: 12px;
  color: var(--wfl-text-secondary);
  background: var(--wfl-bg);
  padding: 3px 10px;
  border-radius: 999px;
}

.word-text {
  font-size: 38px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.word-phonetic {
  color: var(--wfl-primary);
  margin-top: 6px;
  font-size: 15px;
}

.word-chinese {
  margin-top: 14px;
  font-size: 20px;
  font-weight: 600;
}

.word-examples {
  margin-top: 24px;
  border-top: 1px dashed var(--wfl-border);
  padding-top: 18px;
}

.example {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.example-en {
  font-size: 15px;
  color: var(--wfl-text);
  font-style: italic;
}

.example-zh {
  font-size: 13px;
  color: var(--wfl-text-secondary);
}
</style>

