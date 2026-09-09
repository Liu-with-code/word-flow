<template>
  <div class="wfl-page settings-page">
    <h1 class="wfl-page-title">设置</h1>
    <p class="wfl-page-subtitle">管理个人资料与每日学习计划，随时可按需调整。</p>

    <!-- 个人资料 -->
    <div class="wfl-card profile-card">
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        :http-request="customUpload"
        :before-upload="beforeAvatarUpload"
        accept="image/png,image/jpeg,image/webp,image/gif"
      >
        <div class="avatar-wrap">
          <el-avatar :size="84" :src="userStore.user?.avatarUrl || undefined">
            {{ avatarText }}
          </el-avatar>
          <div class="avatar-mask">
            <el-icon :size="18"><Camera /></el-icon>
            <span>更换头像</span>
          </div>
        </div>
      </el-upload>
      <div class="profile-meta">
        <div class="profile-name">{{ userStore.user?.nickname || '未设置昵称' }}</div>
        <div class="profile-username">@{{ userStore.user?.username }}</div>
        <div class="profile-stats">
          <el-tag size="small" effect="plain">已学 {{ stats?.totalLearned ?? 0 }} 词</el-tag>
          <el-tag size="small" type="success" effect="plain">
            连续 {{ stats?.streakDays ?? 0 }} 天
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 学习计划 -->
    <div class="wfl-card section-card">
      <div class="section-title">学习计划</div>
      <div class="form-grid">
        <div class="form-item">
          <label>昵称</label>
          <el-input v-model="form.nickname" placeholder="你希望别人怎么称呼你" />
        </div>
        <div class="form-item">
          <label>每日新词</label>
          <el-input-number v-model="form.dailyWordGoal" :min="1" :max="100" controls-position="right" />
          <p class="item-note">每天计划学习的新单词数</p>
        </div>
        <div class="form-item">
          <label>每日复习</label>
          <el-input-number v-model="form.dailyReviewGoal" :min="1" :max="100" controls-position="right" />
          <p class="item-note">每次复习短文最多包含的单词数</p>
        </div>
        <div class="form-item">
          <label>复习日边界（夜猫子设置）</label>
          <el-select v-model="form.dayBoundaryHour" class="boundary-select">
            <el-option :value="0" label="0 点（默认）：凌晨背的词次日 0 点到期，白天登录会智能询问" />
            <el-option :value="2" label="2 点：凌晨 0-2 点背的词算前一天" />
            <el-option :value="3" label="3 点：凌晨 0-3 点背的词算前一天" />
            <el-option :value="4" label="4 点：凌晨 0-4 点背的词算前一天（推荐夜猫子）" />
            <el-option :value="5" label="5 点：凌晨 0-5 点背的词算前一天" />
            <el-option :value="6" label="6 点：凌晨 0-6 点背的词算前一天" />
          </el-select>
          <p class="item-note">设为 0 时，系统会在白天自动检测凌晨背词并弹窗询问，无需手动配置。</p>
        </div>
      </div>
      <div class="save-row">
        <el-button type="primary" size="large" :loading="saving" @click="save">保存修改</el-button>
      </div>
    </div>

    <!-- 账号 -->
    <div class="wfl-card danger-card">
      <div class="section-title">账号</div>
      <div class="danger-row">
        <div>
          <div class="danger-title">退出登录</div>
          <div class="danger-desc">退出后本地将清除登录状态，学习数据仍保存在云端。</div>
        </div>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import * as learningApi from '@/api/learning'
import * as statisticsApi from '@/api/statistics'
import { useUserStore } from '@/stores/user'
import type { DashboardStats } from '@/types/models'

const router = useRouter()
const userStore = useUserStore()

const saving = ref(false)
const uploading = ref(false)
const stats = ref<DashboardStats | null>(null)

const form = reactive({
  nickname: userStore.user?.nickname || '',
  dailyWordGoal: userStore.user?.dailyWordGoal || 20,
  dailyReviewGoal: userStore.user?.dailyReviewGoal || 20,
  dayBoundaryHour: userStore.user?.dayBoundaryHour ?? 0,
})

const avatarText = computed(() => {
  const nickname = userStore.user?.nickname || '词'
  return nickname.slice(0, 1).toUpperCase()
})

onMounted(async () => {
  try {
    stats.value = await statisticsApi.getDashboard()
  } catch {
    // 统计加载失败不影响设置页
  }
})

async function save() {
  saving.value = true
  try {
    await userStore.updateProfile({
      nickname: form.nickname,
      dailyWordGoal: form.dailyWordGoal,
      dailyReviewGoal: form.dailyReviewGoal,
      dayBoundaryHour: form.dayBoundaryHour,
    })
    try {
      await learningApi.reconcileToday()
    } catch {
      // 计划同步失败不影响保存结果，下次进入学习时会再同步
    }
    ElMessage.success('设置已保存')
  } finally {
    saving.value = false
  }
}

function beforeAvatarUpload(file: File) {
  const allowed = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG / WebP / GIF 格式图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

async function customUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    await userStore.uploadAvatar(options.file as File)
    ElMessage.success('头像已更新')
  } finally {
    uploading.value = false
  }
}

function handleLogout() {
  userStore.logout()
  router.replace('/login')
}
</script>

<style scoped>
.settings-page {
  max-width: 760px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  margin-bottom: 16px;
}

.avatar-uploader {
  flex-shrink: 0;
}

.avatar-wrap {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  font-size: 12px;
  background: rgba(31, 35, 51, 0.55);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-wrap:hover .avatar-mask {
  opacity: 1;
}

.profile-meta {
  min-width: 0;
}

.profile-name {
  font-size: 20px;
  font-weight: 800;
}

.profile-username {
  font-size: 13px;
  color: var(--wfl-text-secondary);
  margin: 4px 0 10px;
}

.profile-stats {
  display: flex;
  gap: 8px;
}

.section-card,
.danger-card {
  margin-bottom: 16px;
  padding: 22px 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 18px;
  padding-left: 10px;
  border-left: 3px solid var(--wfl-primary);
  line-height: 1.2;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 13px;
  font-weight: 600;
  color: var(--wfl-text);
}

.form-item .el-input,
.form-item .el-input-number,
.boundary-select {
  width: 100%;
}

.item-note {
  margin: 0;
  font-size: 12px;
  color: var(--wfl-text-secondary);
  line-height: 1.6;
}

.save-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--wfl-border);
}

.danger-card {
  background: #fffafa;
}

.danger-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.danger-title {
  font-weight: 700;
  font-size: 15px;
}

.danger-desc {
  margin-top: 4px;
  font-size: 13px;
  color: var(--wfl-text-secondary);
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .profile-card {
    flex-direction: column;
    text-align: center;
  }

  .profile-stats {
    justify-content: center;
  }

  .danger-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
