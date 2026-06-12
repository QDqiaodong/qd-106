<template>
  <el-dialog
    v-model="dialogVisible"
    :title="''"
    width="900px"
    class="material-preview-dialog"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <div class="preview-container" v-loading="loading">
      <div class="preview-header">
        <div class="header-info">
          <h3 class="preview-title">{{ thumbnailData?.title || '资料预览' }}</h3>
          <div class="preview-tags" v-if="thumbnailData">
            <el-tag v-if="thumbnailData.categoryName" size="small" type="success">
              {{ thumbnailData.categoryName }}
            </el-tag>
            <el-tag v-if="thumbnailData.gradeName" size="small">
              {{ thumbnailData.gradeName }}
            </el-tag>
            <el-tag v-if="thumbnailData.subjectName" size="small" type="warning">
              {{ thumbnailData.subjectName }}
            </el-tag>
            <span v-if="thumbnailData.totalPages" class="page-info">
              <el-icon><Files /></el-icon>
              共 {{ thumbnailData.totalPages }} 页
            </span>
          </div>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="goToDetail">
            <el-icon><Right /></el-icon>
            查看详情
          </el-button>
        </div>
      </div>

      <div class="preview-body">
        <div class="main-preview">
          <div class="main-image-wrapper">
            <img
              v-if="currentThumbnail"
              :src="currentThumbnail.imageUrl"
              :alt="currentThumbnail.label"
              class="main-image"
              @error="handleImageError"
            />
            <div v-else class="main-image-placeholder">
              <el-icon :size="80" color="#c0c4cc"><Document /></el-icon>
              <span>暂无预览</span>
            </div>
          </div>
          <div class="main-image-label" v-if="currentThumbnail">
            {{ currentThumbnail.label }}
          </div>
        </div>

        <div class="thumbnail-strip">
          <div
            class="thumb-list"
            ref="thumbListRef"
          >
            <div
              v-for="(item, index) in thumbnailData?.thumbnails || []"
              :key="index"
              class="thumb-item"
              :class="{ active: currentIndex === index }"
              @click="selectThumbnail(index)"
            >
              <img
                :src="item.imageUrl"
                :alt="item.label"
                class="thumb-image"
                @error="handleThumbError($event, index)"
              />
              <div class="thumb-label">{{ item.label }}</div>
            </div>
          </div>
          <div class="thumb-nav">
            <el-button
              circle
              size="small"
              :icon="ArrowLeft"
              :disabled="currentIndex === 0"
              @click="prevThumbnail"
            />
            <span class="thumb-counter">{{ currentIndex + 1 }} / {{ thumbnailData?.thumbnails?.length || 0 }}</span>
            <el-button
              circle
              size="small"
              :icon="ArrowRight"
              :disabled="!thumbnailData?.thumbnails || currentIndex >= thumbnailData.thumbnails.length - 1"
              @click="nextThumbnail"
            />
          </div>
        </div>
      </div>

      <div v-if="thumbnailData?.description" class="preview-footer">
        <span class="desc-label">简介：</span>
        <span class="desc-text">{{ thumbnailData.description }}</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Files, Right, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getMaterialThumbnails } from '@/api/material'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  materialId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const thumbListRef = ref(null)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const thumbnailData = ref(null)
const currentIndex = ref(0)

const currentThumbnail = computed(() => {
  if (!thumbnailData.value?.thumbnails?.length) return null
  return thumbnailData.value.thumbnails[currentIndex.value]
})

const loadThumbnails = async () => {
  if (!props.materialId) return
  loading.value = true
  try {
    const res = await getMaterialThumbnails(props.materialId, 8)
    if (res.code === 200 && res.data) {
      thumbnailData.value = res.data
      currentIndex.value = 0
    } else {
      ElMessage.error(res.message || '加载预览失败')
    }
  } catch (e) {
    console.error('加载缩略图失败', e)
    ElMessage.error('加载预览失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const selectThumbnail = (index) => {
  currentIndex.value = index
  scrollToActiveThumb()
}

const prevThumbnail = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
    scrollToActiveThumb()
  }
}

const nextThumbnail = () => {
  if (thumbnailData.value?.thumbnails && currentIndex.value < thumbnailData.value.thumbnails.length - 1) {
    currentIndex.value++
    scrollToActiveThumb()
  }
}

const scrollToActiveThumb = () => {
  setTimeout(() => {
    if (!thumbListRef.value) return
    const activeEl = thumbListRef.value.querySelector('.thumb-item.active')
    if (activeEl) {
      activeEl.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' })
    }
  }, 50)
}

const handleImageError = () => {
  console.warn('主预览图加载失败')
}

const handleThumbError = (e, index) => {
  console.warn('缩略图加载失败', index)
}

const goToDetail = () => {
  if (props.materialId) {
    dialogVisible.value = false
    router.push(`/detail/${props.materialId}`)
  }
}

const handleClose = () => {
  thumbnailData.value = null
  currentIndex.value = 0
}

const handleKeydown = (e) => {
  if (!dialogVisible.value) return
  if (e.key === 'ArrowLeft') {
    prevThumbnail()
  } else if (e.key === 'ArrowRight') {
    nextThumbnail()
  } else if (e.key === 'Enter') {
    goToDetail()
  }
}

watch(() => [dialogVisible.value, props.materialId], ([visible, id]) => {
  if (visible && id) {
    loadThumbnails()
  }
})

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.material-preview-dialog :deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

.material-preview-dialog :deep(.el-dialog__header) {
  display: none;
}

.material-preview-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.preview-container {
  background: #fff;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f7 100%);
  border-bottom: 1px solid #ebeef5;
}

.header-info {
  flex: 1;
  min-width: 0;
}

.preview-title {
  margin: 0 0 10px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.page-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

.header-actions {
  flex-shrink: 0;
  margin-left: 16px;
}

.preview-body {
  padding: 24px;
}

.main-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.main-image-wrapper {
  width: 100%;
  max-width: 420px;
  height: 560px;
  background: #f5f7fa;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.main-image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #c0c4cc;
}

.main-image-label {
  margin-top: 12px;
  padding: 4px 14px;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  border-radius: 20px;
}

.thumbnail-strip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.thumb-list {
  display: flex;
  gap: 12px;
  padding: 8px;
  overflow-x: auto;
  max-width: 100%;
  scroll-behavior: smooth;
}

.thumb-list::-webkit-scrollbar {
  height: 6px;
}

.thumb-list::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

.thumb-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.thumb-list::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

.thumb-item {
  flex-shrink: 0;
  width: 90px;
  height: 120px;
  background: #f5f7fa;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  position: relative;
}

.thumb-item:hover {
  border-color: #b3d8ff;
  transform: translateY(-2px);
}

.thumb-item.active {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.thumb-image {
  flex: 1;
  width: 100%;
  object-fit: cover;
}

.thumb-label {
  padding: 3px 0;
  text-align: center;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.thumb-item.active .thumb-label {
  background: #409eff;
  color: #fff;
  border-top-color: #409eff;
}

.thumb-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb-counter {
  font-size: 14px;
  color: #606266;
  min-width: 60px;
  text-align: center;
}

.preview-footer {
  padding: 16px 24px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 14px;
}

.desc-label {
  color: #909399;
  flex-shrink: 0;
}

.desc-text {
  color: #606266;
  line-height: 1.6;
  flex: 1;
}

@media (max-width: 768px) {
  .material-preview-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin: 5vh auto !important;
  }

  .preview-header {
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }

  .header-actions {
    margin-left: 0;
    width: 100%;
  }

  .header-actions .el-button {
    width: 100%;
  }

  .preview-title {
    font-size: 17px;
  }

  .preview-body {
    padding: 16px;
  }

  .main-image-wrapper {
    max-width: 280px;
    height: 380px;
  }

  .thumb-item {
    width: 70px;
    height: 95px;
  }

  .preview-footer {
    padding: 12px 16px;
    flex-direction: column;
  }
}
</style>
