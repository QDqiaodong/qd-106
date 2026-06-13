<template>
  <div class="topic-detail-page">
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">
        返回列表
      </el-button>
    </div>

    <div v-loading="loading" class="detail-content">
      <div v-if="folder" class="folder-info-card">
        <div class="folder-header">
          <div class="folder-cover-large">
            <div class="cover-gradient" :style="getGradientStyle(folder.id)"></div>
            <el-icon :size="56" class="folder-icon-large"><FolderOpened /></el-icon>
          </div>
          <div class="folder-info-main">
            <h1 class="folder-title">{{ folder.name }}</h1>
            <p class="folder-desc">{{ folder.description || '暂无描述' }}</p>
            <div class="folder-stats">
              <div class="stat-item">
                <el-icon><Document /></el-icon>
                <span>{{ folder.materialCount || 0 }} 份资料</span>
              </div>
              <div class="stat-item">
                <el-icon><View /></el-icon>
                <span>{{ folder.viewCount || 0 }} 次浏览</span>
              </div>
              <div class="stat-item">
                <el-icon><User /></el-icon>
                <span>{{ folder.creatorName || '管理员' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="folder" class="browse-section">
        <div class="section-header">
          <h2 class="section-title">
            <el-icon color="#409EFF"><Reading /></el-icon>
            顺序浏览
          </h2>
          <div class="browse-progress">
            <span>第 {{ currentIndex + 1 }} / {{ items.length }} 份</span>
          </div>
        </div>

        <div v-if="items.length > 0" class="browse-container">
          <div class="browse-sidebar">
            <div class="sidebar-header">
              <el-icon><List /></el-icon>
              <span>目录</span>
            </div>
            <div class="sidebar-list">
              <div
                v-for="(item, index) in items"
                :key="item.id"
                class="sidebar-item"
                :class="{ active: index === currentIndex }"
                @click="jumpToItem(index)"
              >
                <span class="item-index">{{ index + 1 }}</span>
                <div class="item-info">
                  <div class="item-title">{{ item.material?.title || '未命名资料' }}</div>
                  <div v-if="item.remark" class="item-remark">{{ item.remark }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="browse-main">
            <div v-if="currentItem" class="current-item-header">
              <div class="item-index-badge">第 {{ currentIndex + 1 }} 份</div>
              <h3 class="current-item-title">{{ currentItem.material?.title }}</h3>
              <p v-if="currentItem.remark" class="current-item-remark">
                <el-icon color="#E6A23C"><InfoFilled /></el-icon>
                {{ currentItem.remark }}
              </p>
            </div>

            <div v-if="currentItem?.material" class="preview-wrapper">
              <div v-if="previewUrl" class="preview-area">
                <iframe
                  :src="iframeSrc"
                  class="preview-iframe"
                  frameborder="0"
                ></iframe>
              </div>
              <div v-else-if="previewError" class="preview-error">
                <el-icon :size="64" color="#c0c4cc"><Document /></el-icon>
                <p>{{ previewError }}</p>
                <el-button type="primary" @click="goToDetail(currentItem.material.id)">
                  查看详情
                </el-button>
              </div>
              <div v-else class="preview-placeholder">
                <el-icon :size="72" color="#dcdfe6"><Document /></el-icon>
                <p>资料预览加载中...</p>
              </div>
            </div>

            <div class="browse-actions">
              <el-button
                :disabled="currentIndex === 0"
                @click="prevItem"
                :icon="ArrowLeft"
              >
                上一份
              </el-button>
              <el-button
                type="primary"
                @click="goToDetail(currentItem?.material?.id)"
              >
                查看详情
              </el-button>
              <el-button
                :disabled="currentIndex >= items.length - 1"
                type="primary"
                @click="nextItem"
              >
                下一份
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div v-else class="empty-folder">
          <el-empty description="该资料夹暂无资料" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ArrowRight, FolderOpened, Document, View, User,
  Reading, List, InfoFilled
} from '@element-plus/icons-vue'
import {
  getTopicFolderDetail,
  getTopicFolderItems,
  getPreviewStatus
} from '@/api/material'

const route = useRoute()
const router = useRouter()
const folderId = computed(() => Number(route.params.id))

const loading = ref(false)
const folder = ref(null)
const items = ref([])
const currentIndex = ref(0)

const previewUrl = ref('')
const previewError = ref('')
const previewLoading = ref(false)

const gradientColors = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)'
]

const getGradientStyle = (id) => {
  const index = (Number(id) - 1) % gradientColors.length
  return { background: gradientColors[index] }
}

const currentItem = computed(() => {
  return items.value[currentIndex.value] || null
})

const iframeSrc = computed(() => {
  if (!previewUrl.value) return ''
  return previewUrl.value
})

const loadFolderDetail = async () => {
  loading.value = true
  try {
    const [folderRes, itemsRes] = await Promise.all([
      getTopicFolderDetail(folderId.value),
      getTopicFolderItems(folderId.value)
    ])
    if (folderRes.code === 200) {
      folder.value = folderRes.data
    } else {
      ElMessage.error(folderRes.message || '加载失败')
    }
    if (itemsRes.code === 200) {
      items.value = itemsRes.data || []
      if (items.value.length > 0) {
        loadPreviewForCurrent()
      }
    }
  } catch (e) {
    console.error('加载专题详情失败', e)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadPreviewForCurrent = async () => {
  if (!currentItem.value?.material) {
    return
  }
  previewLoading.value = true
  previewError.value = ''
  previewUrl.value = ''
  try {
    const res = await getPreviewStatus(currentItem.value.material.id)
    if (res.code === 200 && res.data?.previewable && res.data?.previewUrl) {
      previewUrl.value = res.data.previewUrl
    } else {
      previewError.value = res.data?.message || '该资料暂不可预览'
    }
  } catch (e) {
    previewError.value = '预览加载失败'
  } finally {
    previewLoading.value = false
  }
}

const prevItem = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const nextItem = () => {
  if (currentIndex.value < items.value.length - 1) {
    currentIndex.value++
  }
}

const jumpToItem = (index) => {
  currentIndex.value = index
}

const goToDetail = (id) => {
  if (id) {
    router.push(`/detail/${id}`)
  }
}

watch(currentIndex, () => {
  loadPreviewForCurrent()
})

onMounted(() => {
  loadFolderDetail()
})
</script>

<style scoped>
.topic-detail-page {
  width: 100%;
}

.detail-header {
  margin-bottom: 20px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.folder-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.folder-header {
  display: flex;
  gap: 24px;
  align-items: center;
}

.folder-cover-large {
  position: relative;
  width: 140px;
  height: 140px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.cover-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.folder-icon-large {
  position: relative;
  z-index: 1;
  color: #fff;
}

.folder-info-main {
  flex: 1;
  min-width: 0;
}

.folder-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 10px 0;
}

.folder-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  margin: 0 0 14px 0;
}

.folder-stats {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #909399;
}

.browse-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.browse-progress {
  font-size: 14px;
  color: #909399;
  background: #f5f7fa;
  padding: 4px 12px;
  border-radius: 12px;
}

.browse-container {
  display: flex;
  gap: 24px;
}

.browse-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  padding-right: 20px;
  max-height: 700px;
  overflow-y: auto;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sidebar-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-item:hover {
  background: #f5f7fa;
}

.sidebar-item.active {
  background: #ecf5ff;
}

.sidebar-item.active .item-index {
  background: #409eff;
  color: #fff;
}

.item-index {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #e4e7ed;
  color: #909399;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}

.item-remark {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.browse-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.current-item-header {
  margin-bottom: 16px;
}

.item-index-badge {
  display: inline-block;
  background: #ecf5ff;
  color: #409eff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 10px;
}

.current-item-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px 0;
  line-height: 1.4;
}

.current-item-remark {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 14px;
  color: #906900;
  background: #fdf6ec;
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.6;
  margin: 0;
}

.preview-wrapper {
  flex: 1;
  min-height: 400px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.preview-area {
  width: 100%;
  height: 500px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-placeholder,
.preview-error {
  height: 500px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: #909399;
}

.preview-error p,
.preview-placeholder p {
  margin: 0;
  font-size: 14px;
}

.browse-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.empty-folder {
  padding: 60px 0;
}

@media (max-width: 960px) {
  .browse-container {
    flex-direction: column;
  }

  .browse-sidebar {
    width: 100%;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid #ebeef5;
    padding-right: 0;
    padding-bottom: 16px;
  }

  .folder-header {
    flex-direction: column;
    text-align: center;
  }

  .folder-stats {
    justify-content: center;
  }
}

@media (max-width: 600px) {
  .folder-cover-large {
    width: 100px;
    height: 100px;
  }

  .folder-title {
    font-size: 20px;
  }

  .preview-area,
  .preview-placeholder,
  .preview-error {
    height: 350px;
  }

  .browse-actions {
    flex-direction: column;
  }

  .browse-actions .el-button {
    width: 100%;
  }
}
</style>
