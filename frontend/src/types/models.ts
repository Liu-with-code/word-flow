/** 全局接口模型：与后端 Result/实体保持一致 */

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  avatarUrl: string;
  dailyWordGoal: number;
  dailyReviewGoal: number;
  activeBookId: number;
  timezone: string;
  dayBoundaryHour: number;
  nightCutoffHour: number;
  lastLoginAt: string | null;
  setupPromptAt: string | null;
  createdAt: string;
}

export interface Book {
  id: number;
  name: string;
  code: string;
  level: string;
  description: string;
  coverColor: string;
  wordCount: number;
  sortNo: number;
  active: boolean;
  /** 已开始学习的单词数（进入过四步练习） */
  learnedCount: number;
  /** 已掌握（通过全部复习轮次）的单词数 */
  masteredCount: number;
  /** 尚未学习的单词数 */
  remainingCount: number;
  /** 学习进度百分比 0-100 */
  progressPercent: number;
}

export interface Word {
  id: number;
  bookId: number;
  word: string;
  phonetic: string;
  chinese: string;
  partOfSpeech: string;
  exampleEn: string;
  exampleZh: string;
  difficulty: number;
  level: string;
}

export interface TodayPlan {
  planDate: string;
  total: number;
  completed: number;
  currentIndex: number;
  words: Word[];
  bookId: number;
  bookName: string;
  dailyWordGoal: number;
}

export interface PracticeSentence {
  sentenceEn: string;
  sentenceZh: string;
}

export interface HintResponse {
  hint: string;
  sentence: string;
}

export interface StepResult {
  correct: boolean;
  message: string;
  nextStep: string;
  score: number | null;
  comment: string | null;
  standard: string | null;
  finished: boolean;
  errors: ErrorItem[] | null;
}

export interface FinishDay {
  articleId: number;
  title: string;
  contentEn: string;
  wordCount: number;
}

export interface ErrorItem {
  segment: string;
  expected: string;
  user: string;
  suggestion: string;
}

export interface ArticleCheckResult {
  articleId: number;
  passed: boolean;
  score: number;
  comment: string;
  errors: ErrorItem[];
  standardZh: string;
}

export interface ReviewOverview {
  dueCount: number;
}

export interface NightPrompt {
  show: boolean;
  date: string;
  count: number;
}

export interface WordFrequency {
  word: string;
  priority: number;
}

export interface ReviewStart {
  hasWords: boolean;
  articleId: number | null;
  title: string | null;
  contentEn: string | null;
  words: Word[];
  frequency: WordFrequency[];
}

export interface DashboardStats {
  totalLearned: number;
  masteredCount: number;
  dueCount: number;
  todayTotal: number;
  todayCompleted: number;
  streakDays: number;
  accuracy: number;
}

export interface WeeklyPoint {
  date: string;
  count: number;
}

export interface RecentRecord {
  word: string;
  stepType: string;
  correct: boolean;
  createdAt: string;
}

/** 单个艾宾浩斯阶段桶（统计可视化） */
export interface StageBucket {
  label: string;
  stage: number;
  count: number;
  description: string;
}

/** 记忆阶段分布 */
export interface StageDistribution {
  total: number;
  complete: number;
  reviewing: number;
  mastered: number;
  learning: number;
  buckets: StageBucket[];
}

/** 未来复习量预测单日数据点 */
export interface ReviewForecastPoint {
  date: string;
  count: number;
}

/** 未来复习量预测 */
export interface ReviewForecast {
  days: number;
  overdueCount: number;
  totalPlanned: number;
  points: ReviewForecastPoint[];
}

/** 薄弱单词（错题） */
export interface WeakWord {
  word: Word;
  wrongCount: number;
  correctCount: number;
  accuracy: number;
  stage: number;
  status: string;
  lastLearnedAt: string | null;
}

/** 复习热身单词卡片 */
export interface WarmupWord {
  word: Word;
  stage: number;
  lastReviewAt: string | null;
  priority: number;
  marked: boolean;
}

/** 复习热身概览 */
export interface WarmupData {
  dueCount: number;
  total: number;
  markedCount: number;
  words: WarmupWord[];
}

/** 热身作答结果 */
export interface WarmupMarkResult {
  remembered: boolean;
  stage: number;
  status: string;
  remaining: number;
}

export type LearningStep =
  'preview' | 'chooseZh' | 'chooseEn' | 'transEn' | 'transZh' | 'complete' | 'article' | 'done';
