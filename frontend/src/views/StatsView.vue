<template>
  <div class="wfl-page" v-loading="loading">
    <h1 class="wfl-page-title">学习统计</h1>
    <p class="wfl-page-subtitle">数据来自每一次选择、每一次翻译，坚持就是最好的记忆曲线。</p>

    <div class="stats-grid">
      <StatCard title="累计学习" :value="stats?.totalLearned ?? 0" icon="Notebook" tone="primary" />
      <StatCard title="长期掌握" :value="stats?.masteredCount ?? 0" icon="Medal" tone="success" />
      <StatCard title="待复习" :value="stats?.dueCount ?? 0" icon="AlarmClock" tone="warning" />
      <StatCard
        title="连续学习"
        :value="`${stats?.streakDays ?? 0} 天`"
        icon="Calendar"
        tone="danger"
      />
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

    <div class="chart-grid second-row">
      <div class="wfl-card stage-card">
        <div class="card-title">记忆阶段分布</div>
        <p class="card-hint">
          共 {{ distribution?.total ?? 0 }} 个单词进入记忆曲线，越靠前的阶段越需要巩固。
        </p>
        <StageBarChart :buckets="distribution?.buckets ?? []" />
      </div>

      <div class="wfl-card forecast-card">
        <div class="card-title">未来 14 天复习量预测</div>
        <div class="forecast-summary">
          <span class="forecast-chip danger">已逾期 {{ forecast?.overdueCount ?? 0 }}</span>
          <span class="forecast-chip primary">待复习 {{ forecast?.totalPlanned ?? 0 }}</span>
        </div>
        <div class="forecast-bars">
          <div v-for="point in forecastPoints" :key="point.date" class="forecast-column">
            <span class="forecast-value">{{ point.count || '' }}</span>
            <div class="forecast-track">
              <div class="forecast-fill" :style="{ height: forecastHeight(point.count) }" />
            </div>
            <span class="forecast-label">{{ dayLabel(point.date) }}</span>
          </div>
        </div>
        <p class="card-hint">柱高表示该日将到期的复习单词数，便于提前安排学习节奏。</p>
      </div>
    </div>

    <div class="wfl-card weak-card">
      <div class="card-title-row">
        <div class="card-title">最容易出错的单词</div>
        <el-button text type="primary" @click="router.push('/mistakes')">
          打开错题本
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <el-empty
        v-if="weakWords.length === 0"
        description="还没有错题，继续保持！"
        :image-size="72"
      />
      <div v-else class="weak-list">
        <div v-for="item in weakWords" :key="item.word.id" class="weak-item">
          <div class="weak-main">
            <span class="weak-word">{{ item.word.word }}</span>
            <span class="weak-phonetic">{{ item.word.phonetic }}</span>
            <span class="weak-chinese">{{ item.word.chinese }}</span>
          </div>
          <div class="weak-meta">
            <el-tag size="small" type="danger" effect="plain"> 错 {{ item.wrongCount }} 次 </el-tag>
            <el-tag size="small" type="info" effect="plain"> 正确率 {{ item.accuracy }}% </el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="wfl-card tips-card">
      <div class="card-title">记忆小贴士</div>
      <p>
        艾宾浩斯遗忘曲线告诉我们：学完后第 1 天遗忘最快。WordFlow 会在第 1、3、7、14、29、59
        天左右安排复习，通过翻译短文反复「输出」， 把短期记忆沉淀为长期记忆。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import * as statisticsApi from '@/api/statistics';
import ProgressRing from '@/components/ProgressRing.vue';
import StageBarChart from '@/components/StageBarChart.vue';
import StatCard from '@/components/StatCard.vue';
import type {
  DashboardStats,
  ReviewForecast,
  StageDistribution,
  WeakWord,
  WeeklyPoint,
} from '@/types/models';

const router = useRouter();

const loading = ref(false);
const stats = ref<DashboardStats | null>(null);
const weekly = ref<WeeklyPoint[]>([]);
const distribution = ref<StageDistribution | null>(null);
const forecast = ref<ReviewForecast | null>(null);
const weakWords = ref<WeakWord[]>([]);

const maxCount = computed(() => Math.max(1, ...weekly.value.map((point) => point.count)));
const forecastPoints = computed(() => forecast.value?.points ?? []);
const maxForecast = computed(() =>
  Math.max(1, ...forecastPoints.value.map((point) => point.count)),
);

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const [overview, week, stages, reviewForecast, weak] = await Promise.all([
      statisticsApi.getDashboard(),
      statisticsApi.getWeekly(),
      statisticsApi.getStageDistribution(),
      statisticsApi.getReviewForecast(14),
      statisticsApi.getWeakWords(5),
    ]);
    stats.value = overview;
    weekly.value = week;
    distribution.value = stages;
    forecast.value = reviewForecast;
    weakWords.value = weak;
  } finally {
    loading.value = false;
  }
}

function barHeight(count: number): string {
  return `${Math.max(4, Math.round((count / maxCount.value) * 100))}%`;
}

function forecastHeight(count: number): string {
  if (count === 0) {
    return '2px';
  }
  return `${Math.max(6, Math.round((count / maxForecast.value) * 100))}%`;
}

function shortDate(date: string): string {
  const [, month, day] = date.split('-');
  return `${month}/${day}`;
}

/** 预测图中每根柱子仅显示「日」，月份变化时补上月前缀，避免信息过载。 */
function dayLabel(date: string): string {
  const [, month, day] = date.split('-');
  return Number(day) === 1 ? `${month}/${day}` : day;
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

.second-row {
  grid-template-columns: 1fr 1fr;
}

.card-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 14px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-title-row .card-title {
  margin-bottom: 0;
}

.card-hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: var(--wfl-text-secondary);
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

.forecast-summary {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.forecast-chip {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
}

.forecast-chip.danger {
  color: var(--wfl-danger);
  background: #fee2e2;
}

.forecast-chip.primary {
  color: var(--wfl-primary);
  background: var(--wfl-primary-bg);
}

.forecast-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 4px;
  height: 160px;
}

.forecast-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
  height: 100%;
}

.forecast-value {
  font-size: 11px;
  color: var(--wfl-text-secondary);
  min-height: 14px;
}

.forecast-track {
  flex: 1;
  width: 100%;
  max-width: 18px;
  background: var(--wfl-bg);
  border-radius: 6px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.forecast-fill {
  width: 100%;
  border-radius: 6px;
  background: linear-gradient(180deg, #34d399, #059669);
  transition: height 0.4s ease;
}

.forecast-label {
  font-size: 11px;
  color: var(--wfl-text-secondary);
}

.weak-card {
  margin-top: 14px;
}

.weak-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.weak-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--wfl-bg);
}

.weak-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.weak-word {
  font-weight: 700;
  font-size: 15px;
}

.weak-phonetic {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.weak-chinese {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.weak-meta {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
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

  .chart-grid,
  .second-row {
    grid-template-columns: 1fr;
  }

  .weak-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
