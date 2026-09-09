<template>
  <div class="wfl-page" v-loading="loading">
    <h1 class="wfl-page-title">{{ greeting }}，{{ userStore.user?.nickname }}</h1>
    <p class="wfl-page-subtitle">
      {{ motto.text }}<span v-if="motto.source" class="motto-source"> —— {{ motto.source }}</span>
    </p>

    <div class="home-grid">
      <div class="wfl-card progress-card">
        <ProgressRing :percent="todayPercent" :size="132" :stroke="12">
          <span class="ring-value">{{ todayPercent }}%</span>
          <span class="ring-label">今日进度</span>
        </ProgressRing>
        <div class="progress-info">
          <div class="progress-count">
            {{ today?.completed ?? 0 }} / {{ goalTotal }}
           <span class="progress-unit">个单词</span>
          </div>
          <el-button
            type="primary"
            :disabled="!!today && today.total > 0 && today.completed >= today.total"
            @click="router.push('/learn')"
          >
            {{
              today && today.total > 0 && today.completed >= today.total
                ? '今日已完成'
                : today && today.completed > 0
                  ? '继续学习'
                  : '开始学习'
            }}
          </el-button>
        </div>
      </div>

      <div class="stats-grid">
        <StatCard title="已学单词" :value="stats?.totalLearned ?? 0" icon="Notebook" tone="primary" />
        <StatCard title="长期掌握" :value="stats?.masteredCount ?? 0" icon="Medal" tone="success" />
        <StatCard title="待复习" :value="stats?.dueCount ?? 0" icon="AlarmClock" tone="warning" />
        <StatCard title="连续学习" :value="`${stats?.streakDays ?? 0} 天`" icon="Calendar" tone="danger" />
      </div>
    </div>

    <div class="cta-grid">
      <div class="wfl-card cta-card">
        <div class="cta-head">
          <div class="cta-icon primary"><el-icon :size="22"><Reading /></el-icon></div>
          <div>
            <div class="cta-title">今日新词</div>
            <div class="cta-desc">先看示例，再选释义，最后中英互译</div>
          </div>
        </div>
        <div v-if="today?.bookName" class="cta-book">
          <span class="cta-book-label">当前词书</span>
          <span class="cta-book-name">{{ today.bookName }}</span>
          <router-link class="cta-book-link" to="/words">更换</router-link>
        </div>
        <div v-if="today && today.words.length" class="word-chips">
          <el-tag v-for="word in today.words.slice(0, 6)" :key="word.id" size="small" effect="plain">
            {{ word.word }}
          </el-tag>
          <span v-if="today.words.length > 6" class="more">+{{ today.words.length - 6 }}</span>
        </div>
        <el-button type="primary" plain class="cta-btn" @click="router.push('/learn')">
          进入今日学习
        </el-button>
      </div>

      <div class="wfl-card cta-card">
        <div class="cta-head">
          <div class="cta-icon success"><el-icon :size="22"><Refresh /></el-icon></div>
          <div>
            <div class="cta-title">艾宾浩斯复习</div>
            <div class="cta-desc">翻译短文，按遗忘曲线巩固旧词</div>
          </div>
        </div>
        <div class="due-badge">
          <span class="due-number">{{ review?.dueCount ?? 0 }}</span>
          个单词等待复习
        </div>
        <el-button
          type="success"
          plain
          class="cta-btn"
          :disabled="!review || review.dueCount === 0"
          @click="router.push('/review')"
        >
          {{ review && review.dueCount > 0 ? '开始复习' : '暂无需复习' }}
        </el-button>
      </div>
    </div>

    <div class="wfl-card recent-card">
      <div class="recent-head">
        <span class="recent-title">最近作答</span>
        <router-link class="recent-more" to="/stats">查看统计</router-link>
      </div>
      <el-empty v-if="!recent.length" description="还没有学习记录，去学第一个单词吧" :image-size="72" />
      <ul v-else class="recent-list">
        <li v-for="(record, index) in recent" :key="index" class="recent-item">
          <span class="recent-word">{{ record.word }}</span>
          <el-tag size="small" effect="plain">{{ stepLabel(record.stepType) }}</el-tag>
          <span class="recent-result" :class="record.correct ? 'ok' : 'bad'">
            {{ record.correct ? '答对' : '答错' }}
          </span>
          <span class="recent-time">{{ formatTime(record.createdAt) }}</span>
        </li>
      </ul>
    </div>

    <!-- 首次登录 / 长期未登录：每日目标设置弹窗 -->
    <el-dialog
      v-model="showSetupDialog"
      width="min(440px, calc(100vw - 32px))"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      align-center
    >
      <div class="setup-body">
        <el-icon :size="46" color="#4f46e5"><Reading /></el-icon>
        <div class="setup-title">
          {{ setupWelcome ? '欢迎回来，重新启航！' : '设置你的每日学习计划' }}
        </div>
        <div v-if="setupWelcome" class="setup-desc">{{ setupWelcomeText }}</div>
        <div v-else class="setup-desc">
          先定个小目标：每天背多少新词、复习多少单词？之后随时可在「设置」页修改。
        </div>
        <div class="setup-fields">
          <div class="setup-field">
            <span class="setup-label">每日新词</span>
            <el-input-number v-model="setupWordGoal" :min="1" :max="100" />
          </div>
          <div class="setup-field">
            <span class="setup-label">每日复习</span>
            <el-input-number v-model="setupReviewGoal" :min="1" :max="100" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="dismissSetup">暂不设置</el-button>
        <el-button type="primary" :loading="setupSaving" @click="saveSetup">保存并开始</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import * as statisticsApi from '@/api/statistics'
import * as learningApi from '@/api/learning'
import * as reviewApi from '@/api/review'
import * as booksApi from '@/api/books'
import { useUserStore } from '@/stores/user'
import { randomQuote } from '@/utils/quotes'
import ProgressRing from '@/components/ProgressRing.vue'
import StatCard from '@/components/StatCard.vue'
import type { DashboardStats, RecentRecord, ReviewOverview, TodayPlan } from '@/types/models'

const router = useRouter()
const userStore = useUserStore()
const motto = ref(randomQuote())

const loading = ref(false)
const stats = ref<DashboardStats | null>(null)
const today = ref<TodayPlan | null>(null)
const review = ref<ReviewOverview | null>(null)
const recent = ref<RecentRecord[]>([])

const SETUP_ABSENCE_DAYS = 7
const showSetupDialog = ref(false)
const setupWelcome = ref(false)
const setupWelcomeText = ref('')
const setupWordGoal = ref(20)
const setupReviewGoal = ref(20)
const setupSaving = ref(false)

const hour = new Date().getHours()
const greeting = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'

/** 进度分母：有计划用计划总数，无计划用用户设置的每日目标。 */
const goalTotal = computed(() => today.value?.total || userStore.user?.dailyWordGoal || 0)

const todayPercent = computed(() => {
  if (!goalTotal.value) return 0
  return Math.round(((today.value?.completed ?? 0) / goalTotal.value) * 100)
})

function onWindowFocus() {
  loadAll()
}

onMounted(() => {
  window.addEventListener('focus', onWindowFocus)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', onWindowFocus)
})

async function loadAll() {
  loading.value = true
  try {
    const [s, t, r, rec] = await Promise.all([
      statisticsApi.getDashboard(),
      learningApi.getTodayPlan(),
      reviewApi.getReviewOverview(),
      statisticsApi.getRecent(8),
    ])
    stats.value = s
    today.value = t
    review.value = r
    recent.value = rec
    await checkNightPrompt()
    await checkSetupPrompt()
  } finally {
    loading.value = false
  }
}

/** 首次登录或长期未登录时弹出每日目标设置；设置/暂不后不再频繁打扰。 */
async function checkSetupPrompt() {
  try {
    await userStore.fetchMe()
  } catch {
    // 网络异常不阻塞首页
  }
  const user = userStore.user
  if (!user) return

  let shouldShow = false
  let welcome = (stats.value?.totalLearned ?? 0) > 0

  // 服务端记录：setupPromptAt 为空 = 从未处理过设置弹窗
  if (!user.setupPromptAt) {
    shouldShow = true
  } else {
    // 已处理过：仅当长期未登录（>=7 天）时再次欢迎
    const last = user.lastLoginAt ? new Date(user.lastLoginAt.replace(' ', 'T')) : null
    if (last && Date.now() - last.getTime() >= SETUP_ABSENCE_DAYS * 86400000) {
      shouldShow = true
      welcome = true
    }
  }
  if (!shouldShow) return

  setupWordGoal.value = user.dailyWordGoal || 20
  setupReviewGoal.value = user.dailyReviewGoal || 20
  setupWelcome.value = welcome
  if (welcome) {
    setupWelcomeText.value = await buildWelcomeText()
  }
  showSetupDialog.value = true
}

async function buildWelcomeText() {
  try {
    const [books, s] = await Promise.all([booksApi.listBooks(), statisticsApi.getDashboard()])
    const active = books.find((book) => book.active)
    const bookName = active?.name || '四级核心词汇'
    return (
      `上次你在学习「${bookName}」，累计学习了 ${s.totalLearned} 个单词，` +
      `连续学习 ${s.streakDays} 天。重新启航前，再确认一下每日计划吧。`
    )
  } catch {
    return '好久不见，重新启航前，先确认一下每日计划吧。'
  }
}

async function saveSetup() {
  setupSaving.value = true
  try {
    await userStore.updateProfile({
      dailyWordGoal: setupWordGoal.value,
      dailyReviewGoal: setupReviewGoal.value,
    })
    await userStore.markSetupPromptSeen()
    showSetupDialog.value = false
    await loadAll()
  } finally {
    setupSaving.value = false
  }
}

async function dismissSetup() {
  await userStore.markSetupPromptSeen()
  showSetupDialog.value = false
}

/** 白天登录时：若检测到凌晨背过单词，询问是否提前加入今日复习。 */
async function checkNightPrompt() {
  try {
    const prompt = await reviewApi.getNightPrompt()
    if (!prompt.show) return
    let apply = false
    try {
      await ElMessageBox.confirm(
        `检测到你今天凌晨学习了 ${prompt.count} 个单词，是否将它们提前加入今日复习？`,
        '夜间学习智能提醒',
        {
          confirmButtonText: '加入今日复习',
          cancelButtonText: '暂不',
          type: 'info',
        },
      )
      apply = true
    } catch {
      apply = false
    }
    await reviewApi.answerNightPrompt(apply)
    review.value = await reviewApi.getReviewOverview()
  } catch {
    // 弹窗失败不影响首页加载
  }
}

function stepLabel(step: string): string {
  const map: Record<string, string> = {
    CHOOSE_ZH: '选释义',
    CHOOSE_EN: '选单词',
    TRANS_EN: '英译中',
    TRANS_ZH: '中译英',
    ARTICLE: '短文翻译',
  }
  return map[step] || step
}

function formatTime(value: string): string {
  const date = new Date(value.replace(' ', 'T'))
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  return sameDay
    ? date.toTimeString().slice(0, 5)
    : date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}
</script>

<style scoped>
.home-grid {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 16px;
}

.progress-card {
  display: flex;
  align-items: center;
  gap: 22px;
}

.ring-value {
  font-size: 26px;
  font-weight: 800;
}

.ring-label {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.progress-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.progress-count {
  font-size: 28px;
  font-weight: 800;
}

.progress-unit {
  font-size: 14px;
  color: var(--wfl-text-secondary);
  font-weight: 400;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.cta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.cta-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cta-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cta-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cta-icon.primary {
  background: var(--wfl-primary-bg);
  color: var(--wfl-primary);
}

.cta-icon.success {
  background: #ecfdf5;
  color: var(--wfl-success);
}

.cta-title {
  font-weight: 700;
  font-size: 16px;
}

.cta-desc {
  font-size: 13px;
  color: var(--wfl-text-secondary);
}

.cta-book {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  background: var(--wfl-primary-bg);
  border-radius: 8px;
  padding: 6px 10px;
  color: var(--wfl-primary);
}

.cta-book-label {
  color: var(--wfl-text-secondary);
}

.cta-book-name {
  font-weight: 700;
}

.cta-book-link {
  margin-left: auto;
  color: var(--wfl-primary);
  text-decoration: none;
  font-size: 12px;
}

.word-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.more {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.due-badge {
  font-size: 14px;
  color: var(--wfl-text-secondary);
}

.due-number {
  font-size: 30px;
  font-weight: 800;
  color: var(--wfl-warning);
  margin-right: 6px;
}

.cta-btn {
  margin-top: auto;
  align-self: flex-start;
}

.recent-card {
  margin-top: 16px;
}

.recent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.recent-title {
  font-weight: 700;
  font-size: 16px;
}

.recent-more {
  font-size: 13px;
  color: var(--wfl-primary);
  text-decoration: none;
}

.recent-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--wfl-border);
  font-size: 14px;
}

.recent-item:last-child {
  border-bottom: none;
}

.recent-word {
  width: 110px;
  font-weight: 600;
}

.recent-result {
  font-size: 13px;
}

.recent-result.ok {
  color: var(--wfl-success);
}

.recent-result.bad {
  color: var(--wfl-danger);
}

.recent-time {
  margin-left: auto;
  color: var(--wfl-text-secondary);
  font-size: 12px;
}

.motto-source {
  font-size: 12px;
  color: var(--wfl-text-secondary);
  margin-left: 6px;
}

.setup-body {
  text-align: center;
  padding: 8px 4px;
}

.setup-title {
  font-size: 20px;
  font-weight: 800;
  margin: 12px 0 8px;
}

.setup-desc {
  font-size: 14px;
  color: var(--wfl-text-secondary);
  line-height: 1.7;
}

.setup-fields {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 18px;
  flex-wrap: wrap;
}

.setup-field {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--wfl-bg);
  border-radius: 10px;
  padding: 8px 12px;
}

.setup-label {
  font-size: 13px;
  color: var(--wfl-text-secondary);
}

@media (max-width: 900px) {
  .home-grid {
    grid-template-columns: 1fr;
  }

  .cta-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
}
</style>
