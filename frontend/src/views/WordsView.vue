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
        <span class="section-note">
          每本书的进度独立保存，切换词书不会清空；「今日学习」从当前词书抽取未学新词
        </span>
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
            <div class="book-badges">
              <el-tag v-if="book.active" size="small" type="success"> 当前 </el-tag>
              <el-tag v-if="isFinished(book)" size="small" type="warning" effect="dark">
                已学完
              </el-tag>
            </div>
          </div>
          <div class="book-body">
            <div class="book-name">
              {{ book.name }}
            </div>
            <div class="book-meta">
              <el-tag size="small" effect="plain">
                {{ book.level }}
              </el-tag>
              <span class="book-count">{{ book.wordCount.toLocaleString() }} 词</span>
            </div>
            <div class="book-desc">
              {{ book.description }}
            </div>

            <!-- 本书学习进度：按账号维度统计，切换词书后依然保留 -->
            <div class="book-progress">
              <div class="progress-head">
                <span class="progress-label">学习进度</span>
                <span class="progress-percent">{{ book.progressPercent }}%</span>
              </div>
              <el-progress
                :percentage="book.progressPercent"
                :stroke-width="10"
                :show-text="false"
                :color="book.coverColor"
              />
              <div class="progress-stats">
                <span
                  >已学 <strong>{{ book.learnedCount.toLocaleString() }}</strong></span
                >
                <span
                  >已掌握 <strong>{{ book.masteredCount.toLocaleString() }}</strong></span
                >
                <span
                  >剩余 <strong>{{ book.remainingCount.toLocaleString() }}</strong></span
                >
              </div>
            </div>

            <div class="book-actions">
              <el-button
                size="small"
                :type="book.active ? 'success' : 'primary'"
                :plain="!book.active"
                :loading="selectingId === book.id"
                :disabled="book.active"
                @click="handleSelect(book)"
              >
                {{ book.active ? '正在使用' : '切换到此词书' }}
              </el-button>
              <el-tooltip content="清空该词书的全部学习进度，使其重新可以学习" placement="top">
                <el-button
                  size="small"
                  :type="isFinished(book) ? 'warning' : 'default'"
                  :plain="!isFinished(book)"
                  :loading="restartingId === book.id"
                  :disabled="book.learnedCount === 0"
                  @click="handleRestart(book)"
                >
                  重新背诵
                </el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无可用词书" :image-size="72" />
    </div>

    <!-- 词书内单词浏览 -->
    <div class="wfl-card table-card">
      <div class="toolbar">
        <el-select
          v-model="selectedBookId"
          placeholder="按词书筛选"
          class="book-select"
          @change="search"
        >
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
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="search"> 查询 </el-button>
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
            <el-tag size="small">
              {{ row.level }}
            </el-tag>
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
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as wordsApi from '@/api/words';
import * as booksApi from '@/api/books';
import * as learningApi from '@/api/learning';
import { useUserStore } from '@/stores/user';
import type { Book, Word } from '@/types/models';

const userStore = useUserStore();

const booksLoading = ref(false);
const loading = ref(false);
const books = ref<Book[]>([]);
const activeBookId = ref(1);
const selectedBookId = ref(1);
const selectingId = ref<number | null>(null);
const restartingId = ref<number | null>(null);
const rows = ref<Word[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');

onMounted(init);

async function init() {
  await loadBooks();
  await load();
}

async function loadBooks() {
  booksLoading.value = true;
  try {
    books.value = await booksApi.listBooks();
    const active = books.value.find((book) => book.active);
    const fromUser = userStore.user?.activeBookId;
    activeBookId.value = active?.id ?? fromUser ?? books.value[0]?.id ?? 1;
    selectedBookId.value = activeBookId.value;
  } finally {
    booksLoading.value = false;
  }
}

/**
 * 切换当前词书。
 *
 * 注意：进度由后端按「账号 + 单词」维度统计并随词书列表一次返回，
 * 这里只更新 books 列表的 active 标记与本地选中项，不清空任何进度数据。
 */
async function handleSelect(book: Book) {
  if (book.id === activeBookId.value) return;
  selectingId.value = book.id;
  try {
    await userStore.selectBook(book.id);
    try {
      await learningApi.reconcileToday();
    } catch {
      // 同步失败不阻塞切换
    }
    activeBookId.value = book.id;
    selectedBookId.value = book.id;
    // 已学完的词书切换后重新可学：提示用户可用「重新背诵」
    const tip = isFinished(book)
      ? '该词书已学完，如需重学可点「重新背诵」'
      : '今日学习将从中抽取新词';
    ElMessage.success(`已切换到「${book.name}」，${tip}`);
    await load();
  } finally {
    selectingId.value = null;
  }
}

/** 重新背诵：二次确认后清空该词书进度（其他词书不受影响）。 */
async function handleRestart(book: Book) {
  try {
    await ElMessageBox.confirm(
      `将清空你在「${book.name}」的 ${book.learnedCount.toLocaleString()} 个单词学习进度，` +
        '该词书会重新变为可学习状态。其他词书的进度、历史作答记录与已生成短文都会保留。',
      '重新背诵确认',
      { type: 'warning', confirmButtonText: '确认清空并重学', cancelButtonText: '取消' },
    );
  } catch {
    // 用户取消
    return;
  }
  restartingId.value = book.id;
  try {
    books.value = await booksApi.restartBook(book.id);
    ElMessage.success(`「${book.name}」已重置，可以重新开始背诵了`);
  } finally {
    restartingId.value = null;
  }
}

/** 是否已学完（已学数量达到词书总词数） */
function isFinished(book: Book): boolean {
  return book.wordCount > 0 && book.learnedCount >= book.wordCount;
}

async function load() {
  loading.value = true;
  try {
    const result = await wordsApi.queryWords({
      keyword: keyword.value || undefined,
      bookId: selectedBookId.value || undefined,
      page: page.value,
      size: size.value,
    });
    rows.value = result.list;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value = 1;
  load();
}

function difficultyType(difficulty: number): 'success' | 'warning' | 'danger' {
  if (difficulty <= 2) return 'success';
  if (difficulty === 3) return 'warning';
  return 'danger';
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
  transition:
    box-shadow 0.2s,
    transform 0.2s;
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
  background: linear-gradient(
    135deg,
    var(--book-color),
    color-mix(in srgb, var(--book-color) 55%, #ffffff)
  );
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

.book-badges {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
  align-items: center;
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
}

/* 每本书独立的学习进度条 */
.book-progress {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed var(--wfl-border);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.progress-label {
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.progress-percent {
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.progress-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--wfl-text-secondary);
}

.progress-stats strong {
  color: var(--wfl-text);
  font-variant-numeric: tabular-nums;
}

.book-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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
