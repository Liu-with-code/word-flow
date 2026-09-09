/** 全局接口模型：与后端 Result/实体保持一致 */

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatarUrl: string
  dailyWordGoal: number
  dailyReviewGoal: number
  activeBookId: number
  timezone: string
  dayBoundaryHour: number
  nightCutoffHour: number
  lastLoginAt: string | null
  setupPromptAt: string | null
  createdAt: string
}

export interface Book {
  id: number
  name: string
  code: string
  level: string
  description: string
  coverColor: string
  wordCount: number
  sortNo: number
  active: boolean
}

export interface Word {
  id: number
  bookId: number
  word: string
  phonetic: string
  chinese: string
  partOfSpeech: string
  exampleEn: string
  exampleZh: string
  difficulty: number
  level: string
}

export interface TodayPlan {
  planDate: string
  total: number
  completed: number
  currentIndex: number
  words: Word[]
  bookId: number
  bookName: string
}

export interface PracticeSentence {
  sentenceEn: string
  sentenceZh: string
}

export interface HintResponse {
  hint: string
  sentence: string
}

export interface StepResult {
  correct: boolean
  message: string
  nextStep: string
  score: number | null
  comment: string | null
  standard: string | null
  finished: boolean
  errors: ErrorItem[] | null
}

export interface FinishDay {
  articleId: number
  title: string
  contentEn: string
  wordCount: number
}

export interface ErrorItem {
  segment: string
  expected: string
  user: string
  suggestion: string
}

export interface ArticleCheckResult {
  articleId: number
  passed: boolean
  score: number
  comment: string
  errors: ErrorItem[]
  standardZh: string
}

export interface ReviewOverview {
  dueCount: number
}

export interface NightPrompt {
  show: boolean
  date: string
  count: number
}

export interface WordFrequency {
  word: string
  priority: number
}

export interface ReviewStart {
  hasWords: boolean
  articleId: number | null
  title: string | null
  contentEn: string | null
  words: Word[]
  frequency: WordFrequency[]
}

export interface DashboardStats {
  totalLearned: number
  masteredCount: number
  dueCount: number
  todayTotal: number
  todayCompleted: number
  streakDays: number
  accuracy: number
}

export interface WeeklyPoint {
  date: string
  count: number
}

export interface RecentRecord {
  word: string
  stepType: string
  correct: boolean
  createdAt: string
}

export type LearningStep =
  | 'preview'
  | 'chooseZh'
  | 'chooseEn'
  | 'transEn'
  | 'transZh'
  | 'complete'
  | 'article'
  | 'done'
