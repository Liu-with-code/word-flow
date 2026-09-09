<template>
  <div class="wfl-page" v-loading="loading">
    <h1 class="wfl-page-title">学习统计</h1>
    <p class="wfl-page-subtitle">数据来自每一次选择、每一次翻译，坚持就是最好的记忆曲线。</p>

    <div class="stats-grid">
      <StatCard title="累计学习" :value="stats?.totalLearned ?? 0" icon="Notebook" tone="primary" />
      <StatCard title="长期掌握" :value="stats?.masteredCount ?? 0" icon="Medal" tone="success" />
      <StatCard title="待复习" :value="stats?.dueCount ?? 0" icon="AlarmClock" tone="warning" />
      <StatCard title="连续学习" :value="`${stats?.streakDays ?? 0} 天`" icon="Calendar" tone="danger" />
    </div>

    <div class="chart-grid">
      <div class="wfl-card accuracy-card">
        <div class="card-title">整体正确率</div>
        <ProgressRing :percent="Math.round(stats?.accuracy ?? 0)" :size="150" :stroke="14">
          <span class="accuracy-value">{{ Math.round(stats?.accuracy ?? 0) }}%</span>
          <span class="accuracy-label">正确率</span>
        </ProgressRing>
        <div class="accuracy-note">
          今日进度：{{ stats?.todayCompleted ?? 0 }} / {{ stats?.todayTotal ?? 0 }} 个单词
        </div>
      </div>

      <div class="wfl-card weekly-card">
        <div class="card-title">近 7 天作答量</div>
        <div class="bars">
          <div v-for="point in weekly" :key="point.date" class="bar-column">
            <span class="bar-value">{{ point.count }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ height: barHeight(point.count) }" />
            </div>
            <span class="bar-label">{{ shortDate(point.date) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="wfl-card tips-card">
      <div class="card-title">记忆小贴士</div>
      <p>
        艾宾浩斯遗忘曲线告诉我们：学完后第 1 天遗忘最快。WordFlow
        会在第 1、3、7、14、29、59 天左右安排复习，通过翻译短文反复「输出」，
        把短期记忆沉淀为长期记忆。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import * as statisticsApi from '@/api/statistics'
import ProgressRing from '@/components/ProgressRing.vue'
import StatCard from '@/components/StatCard.vue'
import type { DashboardStats, WeeklyPoint } from '@/types/models'

const loading = ref(false)
const stats = ref<DashboardStats | null>(null)
const weekly = ref<WeeklyPoint[]>([])

const maxCount = computed(() => Math.max(1, ...weekly.value.map((point) => point.count)))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [s, w] = await Promise.all([statisticsApi.getDashboard(), statisticsApi.getWeekly()])
    stats.value = s
    weekly.value = w
  } finally {
    loading.value = false
  }
}

function barHeight(count: number): string {
  return `${Math.max(4, Math.round((count / maxCount.value) * 100))}%`
}

function shortDate(date: string): string {
  const [, month, day] = date.split('-')
  return `${month}/${day}`
}
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 14px;
  margin-top: 14px;
}

.card-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 14px;
}

.accuracy-card {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.accuracy-value {
  font-size: 30px;
  font-weight: 800;
}

.accuracy-label {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.accuracy-note {
  margin-top: 16px;
  font-size: 13px;
  color: var(--wfl-text-secondary);
}

.weekly-card {
  min-height: 260px;
}

.bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 8px;
  height: 200px;
}

.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  height: 100%;
}

.bar-value {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.bar-track {
  flex: 1;
  width: 28px;
  max-width: 44px;
  background: var(--wfl-bg);
  border-radius: 8px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  border-radius: 8px;
  background: linear-gradient(180deg, #818cf8, #4f46e5);
  transition: height 0.4s ease;
}

.bar-label {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.tips-card {
  margin-top: 14px;
}

.tips-card p {
  margin: 0;
  line-height: 1.8;
  color: var(--wfl-text-secondary);
  font-size: 14px;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>

