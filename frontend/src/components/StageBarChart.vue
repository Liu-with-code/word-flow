<template>
  <div class="stage-chart">
    <div v-for="bucket in buckets" :key="bucket.stage" class="stage-row">
      <div class="stage-head">
        <span class="stage-label">{{ bucket.label }}</span>
        <span class="stage-count">{{ bucket.count }}</span>
      </div>
      <div class="stage-track">
        <div
          class="stage-fill"
          :style="{ width: barWidth(bucket.count), background: colorOf(bucket.stage) }"
        />
      </div>
      <div class="stage-desc">
        {{ bucket.description }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { StageBucket } from '@/types/models';

/**
 * 记忆阶段分布条形图：纯 CSS 绘制，便于移植到小程序 / Android WebView。
 */
const props = defineProps<{
  buckets: StageBucket[];
}>();

const maxCount = computed(() => Math.max(1, ...props.buckets.map((bucket) => bucket.count)));

function barWidth(count: number): string {
  if (count === 0) {
    return '0%';
  }
  return `${Math.max(3, Math.round((count / maxCount.value) * 100))}%`;
}

/** 阶段配色：学习中灰、早期阶段暖色（更易忘）、长期记忆绿色 */
function colorOf(stage: number): string {
  if (stage === 0) {
    return 'linear-gradient(90deg, #cbd5e1, #94a3b8)';
  }
  if (stage <= 2) {
    return 'linear-gradient(90deg, #fb923c, #ea580c)';
  }
  if (stage <= 4) {
    return 'linear-gradient(90deg, #818cf8, #4f46e5)';
  }
  if (stage <= 6) {
    return 'linear-gradient(90deg, #34d399, #059669)';
  }
  return 'linear-gradient(90deg, #22c55e, #15803d)';
}
</script>

<style scoped>
.stage-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stage-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 13px;
}

.stage-label {
  font-weight: 600;
}

.stage-count {
  color: var(--wfl-text-secondary);
  font-variant-numeric: tabular-nums;
}

.stage-track {
  margin-top: 4px;
  height: 10px;
  border-radius: 6px;
  background: var(--wfl-bg);
  overflow: hidden;
}

.stage-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.4s ease;
}

.stage-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--wfl-text-secondary);
}
</style>
