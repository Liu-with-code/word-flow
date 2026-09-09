<template>
  <div class="wfl-page learn-page" v-loading="loading">
    <h1 class="wfl-page-title">今日学习</h1>
    <p class="wfl-page-subtitle">先看完整示例，再选释义，最后中英互译，全部通过才过关。</p>

    <!-- 空计划 -->
    <div v-if="!plan || plan.total === 0" class="wfl-card empty-card">
      <el-icon :size="54" color="#c7d2fe"><Reading /></el-icon>
      <h2>今天还没有学习计划</h2>
      <p>
        当前词书：<strong>{{ plan?.bookName || '四级核心词汇' }}</strong>，你的每日目标是
        <strong>{{ userStore.user?.dailyWordGoal || 20 }}</strong> 个新词。
        点击下方按钮将从词书中抽取未学新词，生成今日计划。
      </p>
      <div class="empty-actions">
        <el-button type="primary" size="large" :loading="loading" @click="startLearning">
          开始今日学习
        </el-button>
        <el-button size="large" plain @click="router.push('/words')">更换词书</el-button>
      </div>
    </div>

    <!-- 中途退出后再进入：继续学习过渡界面 -->
    <div v-else-if="showContinue" class="wfl-card empty-card">
      <el-icon :size="54" color="#6366f1"><Refresh /></el-icon>
      <h2>继续学习</h2>
      <p>
        今日计划来自「{{ plan.bookName }}」，已完成
        <strong>{{ plan.completed }} / {{ plan.total }}</strong> 个单词。
      </p>
      <p class="continue-note">
        点击继续后，系统会先检测你是否更换了词书或调整了每日目标，并自动同步最新计划。
      </p>
      <div class="empty-actions">
        <el-button type="primary" size="large" :loading="busy" @click="continueLearning">
          继续学习
        </el-button>
        <el-button size="large" plain @click="router.push('/words')">更换词书</el-button>
      </div>
    </div>

    <template v-else>
      <!-- 进度与步骤指示 -->
      <div class="wfl-card progress-bar-card">
        <div class="book-line">今日计划来自「{{ plan.bookName }}」词书</div>
        <el-progress
          :percentage="percent"
          :stroke-width="10"
          :format="progressFormat"
          class="progress-bar"
        />
        <div class="step-dots">
          <span
            v-for="(label, index) in stepLabels"
            :key="label"
            class="step-dot"
            :class="{ active: index <= stepIndex }"
          >
            {{ label }}
          </span>
        </div>
      </div>

      <!-- 完整示例 -->
      <div v-if="current && step === 'preview'" class="stage-card">
        <div class="stage-head">
          <span class="stage-title">单词完整示例</span>
          <span class="stage-order">第 {{ currentIndex + 1 }} / {{ plan.total }} 个</span>
        </div>
        <WordCard :word="current" />
        <el-alert
          v-if="stepResult && !stepResult.correct"
          class="stage-alert"
          type="error"
          :closable="false"
          show-icon
          :title="stepResult.message"
        />
        <el-button type="primary" size="large" class="stage-action" @click="startCurrentWord">
          我已学习该单词，开始测试
        </el-button>
      </div>

      <!-- 看英文选中文 -->
      <div v-else-if="current && step === 'chooseZh'" class="stage-card">
        <div class="stage-head">
          <span class="stage-title">这个英文单词的中文意思是？</span>
          <span class="stage-order">第 {{ currentIndex + 1 }} / {{ plan.total }} 个</span>
        </div>
        <div class="question-word">{{ current.word }}</div>
        <ChoiceGrid :options="zhOptions" :disabled="busy" @select="handleZh" />
      </div>

      <!-- 看中文选英文 -->
      <div v-else-if="current && step === 'chooseEn'" class="stage-card">
        <div class="stage-head">
          <span class="stage-title">看到中文，选出对应的英文单词</span>
          <span class="stage-order">第 {{ currentIndex + 1 }} / {{ plan.total }} 个</span>
        </div>
        <div class="question-word">{{ current.chinese }}</div>
        <ChoiceGrid :options="enOptions" :disabled="busy" @select="handleEn" />
      </div>

      <!-- 英译中 -->
      <div v-else-if="current && step === 'transEn'" class="stage-card">
        <div class="stage-head">
          <span class="stage-title">英文短句翻译成中文</span>
          <span class="stage-order">第 {{ currentIndex + 1 }} / {{ plan.total }} 个</span>
        </div>
        <TranslationPanel
          direction="en2zh"
          :sentence="practice?.sentenceEn || ''"
          :result="stepResult"
          :loading="busy"
          :streaming="streamDisplay"
          :hint="hint"
          @submit="handleTransEn"
          @hint="handleHint('en2zh')"
        />
      </div>

      <!-- 中译英 -->
      <div v-else-if="current && step === 'transZh'" class="stage-card">
        <div class="stage-head">
          <span class="stage-title">把中文短句翻译成英文</span>
          <span class="stage-order">第 {{ currentIndex + 1 }} / {{ plan.total }} 个</span>
        </div>
        <TranslationPanel
          direction="zh2en"
          :sentence="practice?.sentenceZh || ''"
          :result="stepResult"
          :loading="busy"
          :streaming="streamDisplay"
          :hint="hint"
          @submit="handleTransZh"
          @hint="handleHint('zh2en')"
        />
      </div>

      <!-- 今日总结短文 -->
      <div v-else-if="step === 'article' && article" class="stage-card">
        <div class="stage-head">
          <span class="stage-title">今日总结短文 · 全部单词都藏在里面</span>
          <span class="stage-order">通过率需 ≥ 60%</span>
        </div>
        <ArticleTranslatePanel
          :title="article.title"
          :content="article.contentEn"
          :result="articleResult"
          :loading="busy"
          @submit="handleArticleSubmit"
          @retry="articleResult = null"
          @regenerate="regenerateArticle"
        />
      </div>

      <!-- 完成 -->
      <div v-else-if="step === 'done'" class="wfl-card done-card">
        <el-icon :size="64" color="#16a34a"><CircleCheckFilled /></el-icon>
        <h2>今日学习全部完成！</h2>
        <p>你已经掌握了 {{ plan.total }} 个新词，并按遗忘曲线安排了复习。</p>
        <div class="done-actions">
          <el-button @click="router.push('/')">返回首页</el-button>
          <el-button type="primary" @click="router.push('/review')">去复习</el-button>
        </div>
      </div>
    </template>

    <!-- 翻译通过弹窗：展示得分、标准译文与 AI 指出的小问题 -->
    <el-dialog
      v-model="showPassDialog"
      width="min(480px, calc(100vw - 32px))"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      align-center
      class="pass-dialog"
    >
      <div class="pass-body">
        <div class="pass-head">
          <el-icon :size="48" color="#16a34a"><CircleCheckFilled /></el-icon>
          <div class="pass-title">翻译通过！</div>
          <div v-if="passResult?.score !== null && passResult?.score !== undefined" class="pass-score">
            得分 <strong>{{ passResult.score }}</strong>
          </div>
        </div>

        <div v-if="passResult?.comment" class="pass-block">
          <div class="pass-block-title">AI 点评</div>
          <div class="pass-comment">{{ passResult.comment }}</div>
        </div>

        <div v-if="passResult?.errors && passResult.errors.length" class="pass-block">
          <div class="pass-block-title">需要改进的小问题</div>
          <div v-for="(error, index) in passResult.errors" :key="index" class="pass-error">
            <div class="pass-error-row"><span class="pass-error-label">期望表达</span>{{ error.expected }}</div>
            <div class="pass-error-row"><span class="pass-error-label">你的表达</span>{{ error.user }}</div>
            <div class="pass-error-row"><span class="pass-error-label">修改建议</span>{{ error.suggestion }}</div>
          </div>
        </div>
        <div v-else class="pass-perfect">非常完美，继续保持！</div>

        <div v-if="passPair" class="pass-block pair-block">
          <div class="pass-block-title">标准译文对照</div>
          <div class="pair-grid">
            <div class="pair-col">
              <div class="pair-label">{{ passPair.sourceLabel }}</div>
              <div class="pair-text">{{ passPair.source }}</div>
            </div>
            <div class="pair-divider" />
            <div class="pair-col">
              <div class="pair-label">{{ passPair.standardLabel }}</div>
              <div class="pair-text pair-standard">{{ passPair.standard }}</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="success" size="large" class="pass-confirm" @click="confirmPass">继续</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import * as learningApi from '@/api/learning'
import { useUserStore } from '@/stores/user'
import WordCard from '@/components/WordCard.vue'
import ChoiceGrid from '@/components/ChoiceGrid.vue'
import TranslationPanel from '@/components/TranslationPanel.vue'
import ArticleTranslatePanel from '@/components/ArticleTranslatePanel.vue'
import type {
  ArticleCheckResult,
  FinishDay,
  LearningStep,
  PracticeSentence,
  StepResult,
  TodayPlan,
  Word,
} from '@/types/models'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const busy = ref(false)
const plan = ref<TodayPlan | null>(null)
const step = ref<LearningStep>('preview')
const zhOptions = ref<string[]>([])
const enOptions = ref<string[]>([])
const practice = ref<PracticeSentence | null>(null)
const stepResult = ref<StepResult | null>(null)
const streamText = ref('')
const hint = ref<string | null>(null)
const showPassDialog = ref(false)
const passResult = ref<StepResult | null>(null)
const passPair = ref<{
  sourceLabel: string
  source: string
  standardLabel: string
  standard: string
} | null>(null)
let pendingPassAction: (() => void | Promise<void>) | null = null
const article = ref<FinishDay | null>(null)
const articleResult = ref<ArticleCheckResult | null>(null)
const showContinue = ref(false)

const stepLabels = ['示例', '选义', '选词', '英译中', '中译英', '短文']
const stepOrder: LearningStep[] = ['preview', 'chooseZh', 'chooseEn', 'transEn', 'transZh', 'article']

const currentIndex = computed(() => plan.value?.currentIndex ?? 0)
const current = computed(() => plan.value?.words[currentIndex.value] ?? null)
const percent = computed(() => {
  if (!plan.value || plan.value.total === 0) return 0
  return Math.round((plan.value.completed / plan.value.total) * 100)
})
const stepIndex = computed(() => {
  if (step.value === 'done') return stepLabels.length
  return Math.max(0, stepOrder.indexOf(step.value))
})

/** 大模型流式输出的是 JSON 片段，实时提取 comment 字段展示为自然语言。 */
const streamDisplay = computed(() => {
  const match = streamText.value.match(/"comment"\s*:\s*"((?:[^"\\]|\\.)*)"/)
  if (match) {
    return match[1].replace(/\\n/g, ' ').replace(/\\"/g, '"')
  }
  const trimmed = streamText.value.trim()
  if (!trimmed) return ''
  // 还没解析出 comment 前，避免把原始 JSON 片段暴露给用户
  return trimmed.startsWith('{') ? '' : trimmed
})

watch(current, () => resetWordStep())

onMounted(init)

async function init() {
  loading.value = true
  try {
    plan.value = await learningApi.getTodayPlan()
    if (plan.value && plan.value.total > 0 && plan.value.completed >= plan.value.total) {
      const stored = readStoredArticle(plan.value.planDate)
      if (stored) {
        article.value = stored.article
        step.value = stored.resultPassed ? 'done' : 'article'
      } else {
        article.value = await learningApi.finishDay()
        writeStoredArticle(plan.value.planDate, article.value, false)
        step.value = 'article'
      }
    } else if (plan.value && plan.value.total > 0) {
      // 计划进行中：先展示“继续学习”，点击后再做计划协调
      showContinue.value = true
    }
  } finally {
    loading.value = false
  }
}

function resetWordStep() {
  step.value = 'preview'
  zhOptions.value = []
  enOptions.value = []
  practice.value = null
  stepResult.value = null
  streamText.value = ''
  hint.value = null
}

async function startLearning() {
  loading.value = true
  try {
    plan.value = await learningApi.startLearning()
    showContinue.value = false
    resetWordStep()
  } finally {
    loading.value = false
  }
}

async function loadPlan() {
  plan.value = await learningApi.getTodayPlan()
}

/** 继续学习：先协调今日计划（词书/目标变更时同步更新），再进入学习。 */
async function continueLearning() {
  busy.value = true
  try {
    plan.value = await learningApi.reconcileToday()
    showContinue.value = false
    resetWordStep()
  } finally {
    busy.value = false
  }
}

function startCurrentWord() {
  if (!current.value) return
  zhOptions.value = buildZhOptions()
  step.value = 'chooseZh'
  prefetchPractice()
}

/** 提前向后端请求 AI 练习句并缓存，用户到翻译阶段时无需再等待。 */
async function prefetchPractice() {
  if (!current.value) return
  try {
    practice.value = await learningApi.getPracticeSentence(current.value.id)
  } catch {
    // 预取失败不阻塞学习，进入翻译阶段时会再次请求
  }
}

function buildZhOptions(): string[] {
  const correct = current.value!.chinese
  const pool = unique(
    [
      ...(plan.value?.words ?? []).map((word) => word.chinese),
      ...FALLBACK_WORDS.map((word) => word.chinese),
    ].filter((item) => item !== correct),
  )
  return shuffle([correct, ...pickN(pool, 3)])
}

function buildEnOptions(): string[] {
  const correct = current.value!.word
  const pool = unique(
    [
      ...(plan.value?.words ?? []).map((word) => word.word),
      ...FALLBACK_WORDS.map((word) => word.word),
    ].filter((item) => item !== correct),
  )
  return shuffle([correct, ...pickN(pool, 3)])
}

async function handleZh(option: string) {
  if (!current.value) return
  busy.value = true
  try {
    stepResult.value = await learningApi.checkZh(current.value.id, option)
    if (stepResult.value.correct) {
      enOptions.value = buildEnOptions()
      step.value = 'chooseEn'
    } else {
      step.value = 'preview'
    }
  } finally {
    busy.value = false
  }
}

async function handleEn(option: string) {
  if (!current.value) return
  busy.value = true
  try {
    stepResult.value = await learningApi.checkEn(current.value.id, option)
    if (stepResult.value.correct) {
      if (!practice.value) {
        practice.value = await learningApi.getPracticeSentence(current.value.id)
      }
      stepResult.value = null
      step.value = 'transEn'
    } else {
      step.value = 'preview'
    }
  } finally {
    busy.value = false
  }
}

async function handleTransEn(translation: string) {
  if (!current.value || !practice.value) return
  busy.value = true
  stepResult.value = null
  streamText.value = ''
  try {
    const result = await learningApi.streamTranslate(
      {
        wordId: current.value.id,
        sentence: practice.value.sentenceEn,
        userTranslation: translation,
        direction: 'en2zh',
      },
      (chunk) => {
        streamText.value += chunk
      },
    )
    streamText.value = ''
    if (result.correct) {
      openPassDialog(
        result,
        {
          sourceLabel: '英文原文',
          source: practice.value.sentenceEn,
          standardLabel: '标准译文（中文）',
          standard: result.standard || practice.value.sentenceZh,
        },
        () => {
          stepResult.value = null
          hint.value = null
          step.value = 'transZh'
        },
      )
    } else {
      stepResult.value = result
    }
  } finally {
    busy.value = false
  }
}

async function handleTransZh(translation: string) {
  if (!current.value || !practice.value) return
  busy.value = true
  stepResult.value = null
  streamText.value = ''
  try {
    const result = await learningApi.streamTranslate(
      {
        wordId: current.value.id,
        sentence: practice.value.sentenceZh,
        userTranslation: translation,
        direction: 'zh2en',
      },
      (chunk) => {
        streamText.value += chunk
      },
    )
    streamText.value = ''
    if (result.correct) {
      const wordId = current.value.id
      openPassDialog(
        result,
        {
          sourceLabel: '中文原文',
          source: practice.value.sentenceZh,
          standardLabel: '标准译文（英文）',
          standard: result.standard || practice.value.sentenceEn,
        },
        async () => {
          busy.value = true
          try {
            stepResult.value = null
            hint.value = null
            const complete = await learningApi.completeWord(wordId)
            if (complete.finished) {
              article.value = await learningApi.finishDay()
              if (plan.value) {
                writeStoredArticle(plan.value.planDate, article.value, false)
              }
              step.value = 'article'
            } else {
              await loadPlan()
            }
          } finally {
            busy.value = false
          }
        },
      )
    } else {
      stepResult.value = result
    }
  } finally {
    busy.value = false
  }
}

async function handleHint(direction: 'en2zh' | 'zh2en') {
  if (!current.value) return
  try {
    await ElMessageBox.confirm(
      '查看参考译文后，本单词将被判定为未通过，需要重新学习该单词。确定查看吗？',
      '提示',
      {
        confirmButtonText: '确定查看',
        cancelButtonText: '我再想想',
        type: 'warning',
      },
    )
  } catch {
    return // 用户选择“我再想想”，继续尝试翻译
  }
  busy.value = true
  try {
    const result = await learningApi.translateHint(current.value.id, direction)
    const sourceLabel = direction === 'en2zh' ? '英文原文' : '中文原文'
    const standardLabel = direction === 'en2zh' ? '标准译文（中文）' : '标准译文（英文）'
    await ElMessageBox.alert(
      `<div style="font-size:14px;line-height:1.8">
         <div style="color:#909399;font-size:12px;margin-bottom:4px">${sourceLabel}</div>
         <div style="margin-bottom:12px">${escapeHtml(result.sentence)}</div>
         <div style="border-top:1px dashed #dcdfe6;margin:12px 0"></div>
         <div style="color:#16a34a;font-size:12px;margin-bottom:4px">${standardLabel}</div>
         <div>${escapeHtml(result.hint)}</div>
       </div>`,
      '参考译文（大模型生成）',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '重新学习该单词',
        type: 'info',
      },
    )
    // 重新学习该单词：回到完整示例，并清空已展示的答案，防止直接照抄提交
    resetWordStep()
  } finally {
    busy.value = false
  }
}

function openPassDialog(
  result: StepResult,
  pair: {
    sourceLabel: string
    source: string
    standardLabel: string
    standard: string
  },
  action: () => void | Promise<void>,
) {
  passResult.value = result
  passPair.value = pair
  pendingPassAction = action
  showPassDialog.value = true
}

async function confirmPass() {
  showPassDialog.value = false
  passResult.value = null
  passPair.value = null
  const action = pendingPassAction
  pendingPassAction = null
  if (action) {
    await action()
  }
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

async function handleArticleSubmit(translation: string) {
  if (!article.value) return
  busy.value = true
  try {
    articleResult.value = await learningApi.checkArticle(article.value.articleId, translation)
    if (articleResult.value.passed) {
      if (plan.value) {
        writeStoredArticle(plan.value.planDate, article.value, true)
      }
      step.value = 'done'
    }
  } finally {
    busy.value = false
  }
}

async function regenerateArticle() {
  if (!plan.value) return
  busy.value = true
  try {
    article.value = await learningApi.finishDay()
    articleResult.value = null
    writeStoredArticle(plan.value.planDate, article.value, false)
  } finally {
    busy.value = false
  }
}

function progressFormat(percentage: number) {
  return `${percentage}%`
}

function shuffle<T>(list: T[]): T[] {
  const copy = [...list]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

function pickN<T>(list: T[], n: number): T[] {
  return shuffle(list).slice(0, n)
}

function unique<T>(list: T[]): T[] {
  return Array.from(new Set(list))
}

function articleStorageKey(date: string) {
  return `wordflow_learn_article_${date}`
}

function readStoredArticle(date: string): { article: FinishDay; resultPassed: boolean } | null {
  try {
    const raw = localStorage.getItem(articleStorageKey(date))
    return raw ? (JSON.parse(raw) as { article: FinishDay; resultPassed: boolean }) : null
  } catch {
    return null
  }
}

function writeStoredArticle(date: string, value: FinishDay, resultPassed: boolean) {
  localStorage.setItem(articleStorageKey(date), JSON.stringify({ article: value, resultPassed }))
}

const FALLBACK_WORDS: Pick<Word, 'word' | 'chinese'>[] = [
  { word: 'ability', chinese: '能力；才能' },
  { word: 'access', chinese: '通道；使用权；进入' },
  { word: 'achieve', chinese: '达到；取得；实现' },
  { word: 'attitude', chinese: '态度；看法' },
  { word: 'balance', chinese: '平衡；余额' },
  { word: 'benefit', chinese: '利益；受益' },
  { word: 'available', chinese: '可获得的；有空的' },
  { word: 'ancient', chinese: '古代的；古老的' },
]
</script>

<style scoped>
.learn-page {
  max-width: 760px;
}

.progress-bar-card {
  margin-bottom: 16px;
}

.book-line {
  font-size: 13px;
  color: var(--wfl-text-secondary);
  margin-bottom: 10px;
}

.progress-bar {
  margin-bottom: 12px;
}

.step-dots {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.step-dot {
  font-size: 12px;
  color: var(--wfl-text-secondary);
  background: var(--wfl-bg);
  border-radius: 999px;
  padding: 3px 10px;
}

.step-dot.active {
  background: var(--wfl-primary-bg);
  color: var(--wfl-primary);
  font-weight: 600;
}

.stage-card {
  background: var(--wfl-card);
  border-radius: var(--wfl-radius);
  box-shadow: var(--wfl-shadow);
  padding: 24px;
}

.stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.stage-title {
  font-size: 15px;
  font-weight: 700;
}

.stage-order {
  font-size: 13px;
  color: var(--wfl-text-secondary);
}

.stage-alert {
  margin-top: 16px;
}

.stage-action {
  width: 100%;
  margin-top: 16px;
}

.question-word {
  text-align: center;
  font-size: 30px;
  font-weight: 800;
  margin: 8px 0 24px;
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

.empty-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.continue-note {
  font-size: 13px !important;
  color: var(--wfl-text-secondary) !important;
}

.done-card {
  text-align: center;
  padding: 56px 24px;
}

.done-card h2 {
  margin: 16px 0 6px;
}

.done-card p {
  color: var(--wfl-text-secondary);
}

.done-actions {
  margin-top: 20px;
}

.pass-body {
  padding: 4px 8px;
}

.pass-head {
  text-align: center;
}

.pass-title {
  font-size: 22px;
  font-weight: 800;
  margin: 10px 0 8px;
}

.pass-score {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f0fdf4;
  color: var(--wfl-success);
  border-radius: 999px;
  padding: 5px 16px;
  font-size: 14px;
  font-weight: 600;
}

.pass-score strong {
  color: var(--wfl-success);
  font-size: 20px;
}

.pass-block {
  margin-top: 16px;
  background: var(--wfl-bg);
  border: 1px solid var(--wfl-border);
  border-radius: 12px;
  padding: 12px 14px;
}

.pass-block-title {
  font-size: 12px;
  color: var(--wfl-text-secondary);
  font-weight: 700;
  margin-bottom: 8px;
}

.pass-comment {
  font-size: 14px;
  line-height: 1.8;
}

.pass-error {
  border: 1px solid var(--wfl-border);
  border-radius: 10px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 13px;
}

.pass-error:last-child {
  margin-bottom: 0;
}

.pass-error-row {
  display: flex;
  gap: 8px;
  line-height: 1.6;
}

.pass-error-label {
  flex-shrink: 0;
  width: 64px;
  color: var(--wfl-text-secondary);
  font-size: 12px;
}

.pass-perfect {
  margin-top: 16px;
  text-align: center;
  color: var(--wfl-success);
  font-weight: 700;
}

.pair-block {
  background: #f8fafc;
}

.pair-grid {
  display: grid;
  grid-template-columns: 1fr 1px 1fr;
  gap: 14px;
  align-items: stretch;
}

.pair-divider {
  background: var(--wfl-border);
}

.pair-label {
  font-size: 12px;
  color: var(--wfl-text-secondary);
  font-weight: 600;
  margin-bottom: 6px;
}

.pair-text {
  font-size: 14px;
  line-height: 1.8;
  color: var(--wfl-text);
}

.pair-standard {
  color: var(--wfl-success);
  font-weight: 600;
}

.pass-confirm {
  min-width: 120px;
}

.pass-dialog :deep(.el-dialog__body) {
  max-height: 68vh;
  overflow-y: auto;
}

@media (max-width: 480px) {
  .pair-grid {
    grid-template-columns: 1fr;
  }

  .pair-divider {
    height: 1px;
    width: 100%;
  }
}
</style>
