<template>
  <div class="wfl-page review-page" v-loading="busy">
    <h1 class="wfl-page-title">艾宾浩斯复习</h1>
    <p class="wfl-page-subtitle">
      复习文章严格按遗忘曲线安排词频：越容易忘的单词出现次数越多。
    </p>

    <div class="wfl-card intro-card">
      <div class="intro-info">
        <div class="due-number">{{ overview?.dueCount ?? 0 }}</div>
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

    <div v-if="session && !session.hasWords" class="wfl-card empty-card">
      <el-icon :size="54" color="#86efac"><CircleCheckFilled /></el-icon>
      <h2>太棒了，暂时没有需要复习的单词</h2>
      <p>继续保持每日学习节奏，遗忘曲线会在恰当的时间提醒你。</p>
      <el-button type="primary" @click="router.push('/learn')">去学新词</el-button>
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
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as reviewApi from '@/api/review'
import ArticleTranslatePanel from '@/components/ArticleTranslatePanel.vue'
import type { ArticleCheckResult, ReviewOverview, ReviewStart } from '@/types/models'

const router = useRouter()

const busy = ref(false)
const overview = ref<ReviewOverview | null>(null)
const session = ref<ReviewStart | null>(null)
const result = ref<ArticleCheckResult | null>(null)

onMounted(load)

async function load() {
  overview.value = await reviewApi.getReviewOverview()
}

async function start() {
  busy.value = true
  try {
    session.value = await reviewApi.startReview()
    result.value = null
    if (session.value.hasWords) {
      await load()
    }
  } finally {
    busy.value = false
  }
}

async function submit(translation: string) {
  if (!session.value?.articleId) return
  busy.value = true
  try {
    result.value = await reviewApi.checkReview(session.value.articleId, translation)
    if (result.value.passed) {
      await load()
    }
  } finally {
    busy.value = false
  }
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
}
</style>

