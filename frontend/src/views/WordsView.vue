<template>
  <div class="wfl-page words-page">
    <h1 class="wfl-page-title">词书</h1>
    <p class="wfl-page-subtitle">
      先选择一本词书，再从中学习新词；当天已经开始的学习计划不受换书影响，次日生效。
    </p>

    <!-- 词书选择区 -->
    <div class="wfl-card book-section" v-loading="booksLoading">
      <div class="section-head">
        <span class="section-title">选择词书</span>
        <span class="section-note">「今日学习」将从当前词书中抽取未学新词</span>
      </div>

      <div v-if="books.length" class="book-grid">
        <div
          v-for="book in books"
          :key="book.id"
          class="book-card"
          :class="{ active: book.id === activeBookId }"
          :style="{ '--book-color': book.coverColor }"
        >
          <div class="book-cover">
            <span class="book-code">{{ book.code }}</span>
            <el-tag v-if="book.id === activeBookId" class="book-active-badge" size="small" type="success">
              当前
            </el-tag>
          </div>
          <div class="book-body">
            <div class="book-name">{{ book.name }}</div>
            <div class="book-meta">
              <el-tag size="small" effect="plain">{{ book.level }}</el-tag>
              <span class="book-count">{{ book.wordCount.toLocaleString() }} 词</span>
            </div>
            <div class="book-desc">{{ book.description }}</div>
            <el-button
              class="book-btn"
              size="small"
              :type="book.id === activeBookId ? 'success' : 'primary'"
              :plain="book.id !== activeBookId"
              :loading="selectingId === book.id"
              :disabled="book.id === activeBookId"
              @click="handleSelect(book)"
            >
              {{ book.id === activeBookId ? '正在使用' : '切换到此词书' }}
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无可用词书" :image-size="72" />
    </div>

    <!-- 词书内单词浏览 -->
    <div class="wfl-card table-card">
      <div class="toolbar">
        <el-select v-model="selectedBookId" placeholder="按词书筛选" class="book-select" @change="search">
          <el-option v-for="book in books" :key="book.id" :label="book.name" :value="book.id" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索单词或中文释义"
          clearable
          class="search-input"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <div class="expand-line"><span class="label">例句</span>{{ row.exampleEn }}</div>
              <div class="expand-line"><span class="label">译文</span>{{ row.exampleZh }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="word" label="单词" width="170">
          <template #default="{ row }">
            <div class="word-cell">
              <span class="word-name">{{ row.word }}</span>
              <span class="word-phonetic">{{ row.phonetic }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="partOfSpeech" label="词性" width="90" />
        <el-table-column prop="chinese" label="中文释义" min-width="200" />
        <el-table-column label="难度" width="110">
          <template #default="{ row }">
            <el-tag :type="difficultyType(row.difficulty)" size="small" effect="plain">
              {{ '★'.repeat(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as wordsApi from '@/api/words'
import * as booksApi from '@/api/books'
import * as learningApi from '@/api/learning'
import { useUserStore } from '@/stores/user'
import type { Book, Word } from '@/types/models'

const userStore = useUserStore()

const booksLoading = ref(false)
const loading = ref(false)
const books = ref<Book[]>([])
const activeBookId = ref(1)
const selectedBookId = ref(1)
const selectingId = ref<number | null>(null)
const rows = ref<Word[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

onMounted(init)

async function init() {
  await loadBooks()
  await load()
}

async function loadBooks() {
  booksLoading.value = true
  try {
    books.value = await booksApi.listBooks()
    const active = books.value.find((book) => book.active)
    const fromUser = userStore.user?.activeBookId
    activeBookId.value = active?.id ?? fromUser ?? books.value[0]?.id ?? 1
    selectedBookId.value = activeBookId.value
  } finally {
    booksLoading.value = false
  }
}

async function handleSelect(book: Book) {
  if (book.id === activeBookId.value) return
  selectingId.value = book.id
  try {
    await userStore.selectBook(book.id)
    try {
      await learningApi.reconcileToday()
    } catch {
      // 同步失败不阻塞切换
    }
    activeBookId.value = book.id
    selectedBookId.value = book.id
    ElMessage.success(`已切换到「${book.name}」，今日学习将从中抽取新词`)
    await load()
  } finally {
    selectingId.value = null
  }
}

async function load() {
  loading.value = true
  try {
    const result = await wordsApi.queryWords({
      keyword: keyword.value || undefined,
      bookId: selectedBookId.value || undefined,
      page: page.value,
      size: size.value,
    })
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function difficultyType(difficulty: number): 'success' | 'warning' | 'danger' {
  if (difficulty <= 2) return 'success'
  if (difficulty === 3) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.book-section {
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 4px;
}

.section-title {
  font-weight: 700;
  font-size: 16px;
}

.section-note {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 14px;
}

.book-card {
  border: 1px solid var(--wfl-border);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s, transform 0.2s;
}

.book-card:hover {
  box-shadow: var(--wfl-shadow);
  transform: translateY(-2px);
}

.book-card.active {
  border-color: var(--book-color);
  box-shadow: 0 0 0 2px var(--book-color);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--book-color) 22%, transparent);
}

.book-cover {
  position: relative;
  height: 72px;
  background: var(--book-color);
  background: linear-gradient(135deg, var(--book-color), color-mix(in srgb, var(--book-color) 55%, #ffffff));
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: 10px 14px;
}

.book-code {
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 1px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
}

.book-active-badge {
  position: absolute;
  top: 10px;
  right: 10px;
}

.book-body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.book-name {
  font-weight: 700;
  font-size: 15px;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.book-count {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.book-desc {
  font-size: 13px;
  color: var(--wfl-text-secondary);
  line-height: 1.5;
  flex: 1;
}

.book-btn {
  align-self: flex-start;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.book-select {
  width: 180px;
}

.search-input {
  max-width: 320px;
  flex: 1;
}

.table-card {
  padding: 8px 16px 16px;
}

.word-cell {
  display: flex;
  flex-direction: column;
}

.word-name {
  font-weight: 700;
}

.word-phonetic {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.expand-content {
  padding: 8px 40px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
}

.expand-line .label {
  display: inline-block;
  width: 48px;
  color: var(--wfl-text-secondary);
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 640px) {
  .toolbar {
    flex-wrap: wrap;
  }

  .book-select {
    width: 100%;
  }

  .search-input {
    max-width: none;
    flex: 1;
  }
}
</style>
