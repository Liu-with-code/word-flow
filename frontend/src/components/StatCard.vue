<template>
  <div class="stat-card wfl-card">
    <div class="stat-icon" :style="{ background: toneBg, color: toneColor }">
      <el-icon :size="20"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-info">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-title">{{ title }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    title: string
    value: string | number
    icon?: string
    tone?: 'primary' | 'success' | 'warning' | 'danger'
  }>(),
  {
    icon: 'TrendCharts',
    tone: 'primary',
  },
)

const toneMap: Record<string, { bg: string; color: string }> = {
  primary: { bg: '#eef2ff', color: '#4f46e5' },
  success: { bg: '#ecfdf5', color: '#16a34a' },
  warning: { bg: '#fffbeb', color: '#d97706' },
  danger: { bg: '#fef2f2', color: '#dc2626' },
}

const toneBg = computed(() => toneMap[props.tone].bg)
const toneColor = computed(() => toneMap[props.tone].color)
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
}

.stat-title {
  font-size: 13px;
  color: var(--wfl-text-secondary);
}
</style>

