<template>
  <div class="wfl-page review-page" v-loading="busy">
    <h1 class="wfl-page-title">艾宾浩斯复习</h1>
    <p class="wfl-page-subtitle">复习文章严格按遗忘曲线安排词频：越容易忘的单词出现次数越多。</p>

    <div class="wfl-card intro-card">
      <div class="intro-info">
        <div class="due-number">
          {{ overview?.dueCount ?? 0 }}
        </div>
        <div>
          <div class="intro-title">个单词等待复习</div>
          <div class="intro-desc">通过翻译短文巩固记忆，正确率 ≥ 60% 才算通过</div>
        </div>
      </div>
      <el-button
        v-if="!session || !session.hasWords"
        type="success"
        size="large"
        :disabled="!overview || overview.dueCount === 0"
        @click="start"
      >
        生成复习短文
      </el-button>
    </div>

    <!-- 复习热身：先快速自测，记得的词直接推进阶段 -->
    <div v-if="warmup && warmup.words.length > 0" class="wfl-card warmup-card">
      <div class="warmup-head">
        <div>
          <div class="warmup-title">复习热身 · 快速自测</div>
          <div class="warmup-desc">
            先凭记忆判断「记得 / 不熟」：记得的单词直接推进复习阶段，不熟的会进入复习短文重点巩固。
          </div>
        </div>
        <div class="warmup-progress">
          已完成 <strong>{{ warmupDone }}</strong> / {{ warmup.words.length }}
        </div>
      </div>

      <div class="warmup-list">
        <div v-for="item in warmup.words" :key="item.word.id" class="warmup-item">
          <div class="warmup-word">
            <span class="word-text">{{ item.word.word }}</span>
            <span class="word-phonetic">{{ item.word.phonetic }}</span>
            <el-tag size="small" effect="plain" :type="priorityTone(item.priority)">
              紧急度 {{ item.priority }}
            </el-tag>
          </div>
          <div class="warmup-actions">
            <template v-if="markedIds.includes(item.word.id)">
              <el-tag
                size="small"
                :type="rememberedIds.includes(item.word.id) ? 'success' : 'warning'"
              >
                {{ rememberedIds.includes(item.word.id) ? '已记住，阶段已推进' : '已标记不熟' }}
              </el-tag>
            </template>
            <template v-else>
              <el-button
                size="small"
                type="success"
                plain
                :loading="markingId === item.word.id"
                @click="mark(item.word.id, true)"
              >
                记得
              </el-button>
              <el-button
                size="small"
                type="warning"
                plain
                :loading="markingId === item.word.id"
                @click="mark(item.word.id, false)"
              >
                不熟
              </el-button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div v-if="session && !session.hasWords" class="wfl-card empty-card">
      <el-icon :size="54" color="#86efac">
        <CircleCheckFilled />
      </el-icon>
      <h2>太棒了，暂时没有需要复习的单词</h2>
      <p>继续保持每日学习节奏，遗忘曲线会在恰当的时间提醒你。</p>
      <el-button type="primary" @click="router.push('/learn')"> 去学新词 </el-button>
    </div>

    <template v-if="session && session.hasWords && session.articleId && session.contentEn">
      <div class="wfl-card freq-card">
        <div class="freq-title">本篇文章单词出现优先级</div>
        <div class="freq-list">
          <el-tag
            v-for="item in session.frequency"
            :key="item.word"
            size="small"
            :type="item.priority >= 3 ? 'danger' : item.priority === 2 ? 'warning' : 'info'"
            effect="plain"
          >
            {{ item.word }} ×{{ item.priority }}
          </el-tag>
        </div>
      </div>

      <ArticleTranslatePanel
        :title="session.title || '复习短文'"
        :content="session.contentEn"
        :result="result"
        :loading="busy"
        @submit="submit"
        @retry="result = null"
        @regenerate="start"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import * as reviewApi from '@/api/review';
import ArticleTranslatePanel from '@/components/ArticleTranslatePanel.vue';
import type { ArticleCheckResult, ReviewOverview, ReviewStart, WarmupData } from '@/types/models';

const router = useRouter();

const busy = ref(false);
const overview = ref<ReviewOverview | null>(null);
const session = ref<ReviewStart | null>(null);
const result = ref<ArticleCheckResult | null>(null);
const warmup = ref<WarmupData | null>(null);
const markedIds = ref<number[]>([]);
const rememberedIds = ref<number[]>([]);
const markingId = ref<number | null>(null);

const warmupDone = computed(() => markedIds.value.length);

onMounted(load);

async function load() {
  const [overviewData, warmupData] = await Promise.all([
    reviewApi.getReviewOverview(),
    reviewApi.getWarmup(),
  ]);
  overview.value = overviewData;
  warmup.value = warmupData;
}

async function start() {
  busy.value = true;
  try {
    session.value = await reviewApi.startReview();
    result.value = null;
    if (session.value.hasWords) {
      await load();
    }
  } finally {
    busy.value = false;
  }
}

async function submit(translation: string) {
  if (!session.value?.articleId) return;
  busy.value = true;
  try {
    result.value = await reviewApi.checkReview(session.value.articleId, translation);
    if (result.value.passed) {
      await load();
    }
  } finally {
    busy.value = false;
  }
}

/** 热身作答：记得 -> 后端推进复习阶段；不熟 -> 保留在复习队列。 */
async function mark(wordId: number, remembered: boolean) {
  markingId.value = wordId;
  try {
    const marked = await reviewApi.markWarmup(wordId, remembered);
    markedIds.value = [...markedIds.value, wordId];
    if (remembered) {
      rememberedIds.value = [...rememberedIds.value, wordId];
    }
    overview.value = { dueCount: marked.remaining };
    ElMessage.success(
      remembered ? `已记住，进入第 ${marked.stage} 轮复习` : '已标记不熟，将在复习短文中重点出现',
    );
  } finally {
    markingId.value = null;
  }
}

function priorityTone(priority: number): 'danger' | 'warning' | 'info' {
  if (priority >= 3) {
    return 'danger';
  }
  return priority === 2 ? 'warning' : 'info';
}
</script>

<style scoped>
.review-page {
  max-width: 760px;
}

.intro-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.intro-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.due-number {
  font-size: 44px;
  font-weight: 800;
  color: var(--wfl-warning);
  line-height: 1;
}

.intro-title {
  font-weight: 700;
  font-size: 16px;
}

.intro-desc {
  font-size: 13px;
  color: var(--wfl-text-secondary);
}

.warmup-card {
  margin-bottom: 16px;
}

.warmup-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.warmup-title {
  font-weight: 700;
  font-size: 16px;
}

.warmup-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--wfl-text-secondary);
  line-height: 1.6;
}

.warmup-progress {
  font-size: 12px;
  color: var(--wfl-text-secondary);
  white-space: nowrap;
}

.warmup-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 340px;
  overflow-y: auto;
}

.warmup-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--wfl-bg);
}

.warmup-word {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.word-text {
  font-weight: 700;
}

.word-phonetic {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.warmup-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.freq-card {
  margin-bottom: 16px;
}

.freq-title {
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 10px;
}

.freq-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-card {
  text-align: center;
  padding: 56px 24px;
}

.empty-card h2 {
  margin: 16px 0 6px;
}

.empty-card p {
  color: var(--wfl-text-secondary);
  margin-bottom: 20px;
}

@media (max-width: 480px) {
  .intro-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .warmup-head {
    flex-direction: column;
  }

  .warmup-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
