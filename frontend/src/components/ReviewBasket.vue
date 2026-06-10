<template>
  <el-drawer
    v-model="visible"
    title="本轮复习资料篮"
    direction="rtl"
    size="420px"
    class="review-basket-drawer"
  >
    <template #header>
      <div class="drawer-header">
        <div class="header-left">
            <el-icon :size="20" color="#409EFF"><ShoppingCart /></el-icon>
            <span class="drawer-title">本轮复习资料篮</span>
            <el-badge :value="appStore.basketCount" :max="99" class="count-badge" />
          </div>
      </div>
    </template>

    <div class="basket-content">
      <div class="stats-section">
        <div class="stat-card">
          <div class="stat-num">{{ appStore.basketCount }}</div>
          <div class="stat-label">已选资料</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ totalSizeText }}</div>
          <div class="stat-label">总文件体量</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ lastAddedText }}</div>
          <div class="stat-label">最近加入</div>
        </div>
      </div>

      <el-divider v-if="appStore.basketCount > 0" />

      <div v-if="appStore.basketCount === 0" class="empty-section">
        <el-empty description="资料篮为空" :image-size="100">
          <template #description>
            <div class="empty-desc">
              <p>还没有添加资料哦</p>
              <p class="empty-tip">在首页或详情页将感兴趣的资料加入资料篮</p>
            </div>
          </template>
        </el-empty>
      </div>

      <div v-else class="groups-section">
        <div
          v-for="group in appStore.basketGroupedBySubject"
          :key="group.subjectId || 'unknown'"
          class="subject-group"
        >
          <div class="group-header">
            <div class="group-title">
              <el-tag :type="getSubjectTagType(group.subjectId)" size="small" effect="dark">
                {{ group.subjectName }}
              </el-tag>
              <span class="group-count">{{ group.items.length }} 份</span>
            </div>
            <el-button
              link
              type="danger"
              size="small"
              @click="removeGroup(group)"
            >
              移除本组
            </el-button>
          </div>

          <div class="group-items">
            <div
              v-for="item in sortedItems(group.items)"
              :key="item.id"
              class="basket-item"
            >
              <div class="item-main" @click="goDetail(item.id)">
                <div class="item-icon">
                  <el-icon :size="22" color="#409EFF"><Document /></el-icon>
                </div>
                <div class="item-info">
                  <div class="item-title">{{ item.title }}</div>
                  <div class="item-meta">
                    <span v-if="item.categoryName" class="meta-tag">{{ item.categoryName }}</span>
                    <span v-if="item.gradeName" class="meta-tag">{{ item.gradeName }}</span>
                    <span v-if="item.fileFormat" class="meta-format">{{ item.fileFormat }}</span>
                    <span v-if="item.fileSize > 0" class="meta-size">{{ formatSize(item.fileSize) }}</span>
                  </div>
                </div>
              </div>
              <div class="item-right">
                <div class="item-time">{{ formatTime(item.addedAt) }}</div>
                <el-button
                  type="danger"
                  link
                  size="small"
                  @click.stop="handleRemove(item.id)"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div v-if="appStore.basketCount > 0" class="drawer-footer">
        <el-button @click="handleClearAll">
          <el-icon><Delete /></el-icon>
          <span style="margin-left: 4px;">清空资料篮</span>
        </el-button>
        <el-button type="primary">
          <el-icon><Reading /></el-icon>
          <span style="margin-left: 4px;">开始本轮复习</span>
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ShoppingCart,
  Document,
  Close,
  Delete,
  Reading
} from '@element-plus/icons-vue'
import { useAppStore } from '@/store'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const appStore = useAppStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '-'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const totalSizeText = computed(() => {
  const total = appStore.basketTotalSize
  if (total === 0) return '-'
  return formatSize(total)
})

const lastAddedText = computed(() => {
  const t = appStore.basketLastAddedTime
  if (!t) return '-'
  return formatRelativeTime(t)
})

const formatTime = (isoStr) => {
  if (!isoStr) return ''
  const date = new Date(isoStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / (60 * 1000)) + ' 分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / (60 * 60 * 1000)) + ' 小时前'
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${m}-${d} ${h}:${mi}`
}

const formatRelativeTime = (isoStr) => {
  if (!isoStr) return '-'
  const date = new Date(isoStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / (60 * 1000)) + '分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / (60 * 60 * 1000)) + '小时前'
  if (diff < 7 * 24 * 60 * 60 * 1000) return Math.floor(diff / (24 * 60 * 60 * 1000)) + '天前'
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${m}-${d}`
}

const subjectColorMap = {
  1: 'danger',
  2: 'primary',
  3: 'success',
  4: 'warning',
  5: 'info'
}

const subjectColors = ['primary', 'success', 'warning', 'danger', 'info']

const getSubjectTagType = (id) => {
  if (subjectColorMap[id]) return subjectColorMap[id]
  const idx = (Number(id) || 0) % subjectColors.length
  return subjectColors[idx]
}

const sortedItems = (items) => {
  return [...items].sort((a, b) => new Date(b.addedAt) - new Date(a.addedAt))
}

const goDetail = (id) => {
  visible.value = false
  router.push(`/detail/${id}`)
}

const handleRemove = async (id) => {
  try {
    await ElMessageBox.confirm('确定要从资料篮中移除这份资料吗？', '移除确认', {
      confirmButtonText: '确认移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    appStore.removeFromBasket(id)
    ElMessage.success('已从资料篮移除')
  } catch (e) {}
}

const removeGroup = async (group) => {
  try {
    await ElMessageBox.confirm(`确定要移除「${group.subjectName}」分组下的所有资料吗？`, '移除确认', {
      confirmButtonText: '确认移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    group.items.forEach(item => appStore.removeFromBasket(item.id))
    ElMessage.success(`已移除 ${group.items.length} 份资料`)
  } catch (e) {}
}

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空本轮复习资料篮吗？此操作不可恢复。', '清空确认', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    appStore.clearBasket()
    ElMessage.success('资料篮已清空')
  } catch (e) {}
}
</script>

<style scoped>
.review-basket-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.review-basket-drawer :deep(.el-drawer__title) {
  color: #fff;
}

.review-basket-drawer :deep(.el-drawer__headerbtn .el-drawer__close) {
  color: rgba(255, 255, 255, 0.9);
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-title {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
}

.count-badge {
  margin-left: 0;
}

.basket-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 4px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  padding: 16px 0;
}

.stat-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 10px;
  padding: 14px 10px;
  text-align: center;
  border: 1px solid #bae6fd;
}

.stat-card:nth-child(2) {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-color: #bbf7d0;
}

.stat-card:nth-child(3) {
  background: linear-gradient(135deg, #fefce8 0%, #fef9c3 100%);
  border-color: #fde68a;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.empty-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-desc p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.empty-desc .empty-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 6px;
}

.groups-section {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.subject-group {
  margin-bottom: 20px;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 0 2px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-count {
  font-size: 12px;
  color: #909399;
}

.group-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.basket-item {
  display: flex;
  align-items: stretch;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  transition: all 0.2s;
}

.basket-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.1);
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 10px;
  cursor: pointer;
}

.item-icon {
  width: 40px;
  height: 40px;
  background: #ecf5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.meta-tag {
  font-size: 11px;
  color: #606266;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

.meta-format {
  font-size: 11px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.meta-size {
  font-size: 11px;
  color: #909399;
}

.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  padding-left: 10px;
  margin-left: 10px;
  border-left: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.item-time {
  font-size: 11px;
  color: #c0c4cc;
}

.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
}
</style>
