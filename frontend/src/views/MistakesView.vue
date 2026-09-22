<template>
  <div class="wfl-page mistakes-page" v-loading="loading">
    <h1 class="wfl-page-title">错题本</h1>
    <p class="wfl-page-subtitle">
      这里收录了你答错过的单词。把错题重新加入今日学习，用「输出」把易错点彻底吃透。
    </p>

    <div class="wfl-card summary-card">
      <div class="summary-item">
        <div class="summary-value">
          {{ total }}
        </div>
        <div class="summary-label">错题总数</div>
      </div>
      <div class="summary-item">
        <div class="summary-value">
          {{ selectedIds.length }}
        </div>
        <div class="summary-label">已选择</div>
      </div>
      <div class="summary-actions">
        <el-button :disabled="rows.length === 0" @click="selectAllVisible"> 全选本页 </el-button>
        <el-button :disabled="selectedIds.length === 0" @click="selectedIds = []">
          清空选择
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="selectedIds.length === 0"
          @click="addSelectedToToday"
        >
          加入今日学习
        </el-button>
      </div>
    </div>

    <div class="wfl-card">
      <el-empty v-if="!loading && rows.length === 0" description="还没有错题记录，继续保持！" />
      <el-checkbox-group v-else v-model="selectedIds" class="mistake-list">
        <label v-for="row in rows" :key="row.word.id" class="mistake-item">
          <el-checkbox :value="row.word.id" class="mistake-check" />
          <div class="item-word">
            <span class="word-text">{{ row.word.word }}</span>
            <span class="word-phonetic">{{ row.word.phonetic }}</span>
          </div>
          <div class="item-chinese">{{ row.word.chinese }}</div>
          <div class="item-meta">
            <span class="wrong-count">错 {{ row.wrongCount }}</span>
            <span class="slash">/</span>
            <span class="correct-count">对 {{ row.correctCount }}</span>
            <el-tag :type="accuracyTone(row.accuracy)" size="small" effect="plain">
              {{ row.accuracy }}%
            </el-tag>
            <el-tag type="info" size="small" effect="plain">{{ stageLabel(row.stage) }}</el-tag>
            <span class="item-time">{{ formatTime(row.lastLearnedAt) }}</span>
          </div>
        </label>
      </el-checkbox-group>

      <div v-if="total > size" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="changePage"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { addWordsToToday } from '@/api/learning';
import * as statisticsApi from '@/api/statistics';
import type { WeakWord } from '@/types/models';

const router = useRouter();

const loading = ref(false);
const submitting = ref(false);
const rows = ref<WeakWord[]>([]);
const selectedIds = ref<number[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const result = await statisticsApi.getWeakWordPage(page.value, size.value);
    rows.value = result.list;
    total.value = result.total;
    selectedIds.value = [];
  } finally {
    loading.value = false;
  }
}

function changePage(next: number) {
  page.value = next;
  void load();
}

function selectAllVisible() {
  selectedIds.value = rows.value.map((row) => row.word.id);
}

async function addSelectedToToday() {
  if (selectedIds.value.length === 0) {
    return;
  }
  submitting.value = true;
  try {
    const plan = await addWordsToToday(selectedIds.value);
    ElMessage.success(`已加入今日学习，今日进度 ${plan.completed}/${plan.total}`);
    router.push('/learn');
  } finally {
    submitting.value = false;
  }
}

function accuracyTone(accuracy: number): 'danger' | 'warning' | 'success' {
  if (accuracy < 40) {
    return 'danger';
  }
  return accuracy < 70 ? 'warning' : 'success';
}

/** 阶段文案与后端艾宾浩斯阶段语义保持一致 */
function stageLabel(stage: number): string {
  if (stage <= 0) {
    return '学习中';
  }
  if (stage > 6) {
    return '长期';
  }
  return `第 ${stage} 轮`;
}

function formatTime(value: string | null): string {
  if (!value) {
    return '—';
  }
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped>
.mistakes-page {
  max-width: 1080px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 14px;
}

.summary-item {
  text-align: center;
}

.summary-value {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
}

.summary-label {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.summary-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.mistake-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mistake-item {
  display: grid;
  grid-template-columns: 32px 150px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--wfl-bg);
  cursor: pointer;
}

.mistake-check {
  margin-right: 0;
}

.item-word {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.word-text {
  font-weight: 700;
}

.word-phonetic {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.item-chinese {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  font-size: 12px;
}

.wrong-count {
  color: var(--wfl-danger);
  font-weight: 700;
}

.correct-count {
  color: var(--wfl-success);
  font-weight: 700;
}

.slash {
  color: var(--wfl-text-secondary);
}

.item-time {
  color: var(--wfl-text-secondary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 900px) {
  .summary-card {
    flex-wrap: wrap;
    gap: 16px;
  }

  .summary-actions {
    margin-left: 0;
    width: 100%;
  }

  .mistake-item {
    grid-template-columns: 28px 1fr;
    row-gap: 6px;
  }

  .item-meta {
    grid-column: 1 / -1;
    flex-wrap: wrap;
  }
}
</style>
