<template>
  <div class="chunk-upload-progress">
    <div class="file-info" v-if="fileName">
      <div class="file-icon">
        <el-icon :size="28" color="#409EFF"><Document /></el-icon>
      </div>
      <div class="file-detail">
        <div class="file-name">{{ fileName }}</div>
        <div class="file-meta">
          <span>{{ formatFileSize(uploadedSize) }} / {{ formatFileSize(fileSize) }}</span>
          <span class="meta-sep">|</span>
          <span>{{ progress }}%</span>
        </div>
      </div>
      <div class="file-status">
        <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
      </div>
    </div>

    <el-progress
      :percentage="progress"
      :status="progressStatus"
      :stroke-width="8"
      :text-inside="false"
      class="main-progress"
    />

    <div class="progress-stats">
      <div class="stat-item">
        <el-icon :size="16" color="#909399"><DataLine /></el-icon>
        <span class="stat-label">传输速率</span>
        <span class="stat-value">{{ formatFileSize(speed) }}/s</span>
      </div>
      <div class="stat-item">
        <el-icon :size="16" color="#909399"><Clock /></el-icon>
        <span class="stat-label">剩余时间</span>
        <span class="stat-value">{{ formatTime(remainingTime) }}</span>
      </div>
      <div class="stat-item">
        <el-icon :size="16" color="#909399"><Files /></el-icon>
        <span class="stat-label">分片进度</span>
        <span class="stat-value">{{ completedCount }} / {{ totalChunks }}</span>
      </div>
    </div>

    <div class="action-buttons" v-if="showActions">
      <el-button
        v-if="status === 'uploading'"
        size="default"
        @click="$emit('pause')"
      >
        <el-icon><VideoPause /></el-icon>
        <span>暂停</span>
      </el-button>
      <el-button
        v-if="status === 'paused'"
        type="primary"
        size="default"
        @click="$emit('resume')"
      >
        <el-icon><VideoPlay /></el-icon>
        <span>继续</span>
      </el-button>
      <el-button
        v-if="status === 'error' && failedChunks.length > 0"
        type="warning"
        size="default"
        @click="$emit('retry-all')"
      >
        <el-icon><RefreshRight /></el-icon>
        <span>重试失败分片 ({{ failedChunks.length }})</span>
      </el-button>
      <el-button
        size="default"
        @click="$emit('cancel')"
        :disabled="status === 'cancelling' || status === 'merging'"
      >
        <el-icon><Close /></el-icon>
        <span>取消</span>
      </el-button>
    </div>

    <div class="error-message" v-if="error">
      <el-alert :title="error" type="error" :closable="false" show-icon />
    </div>

    <div class="chunks-section" v-if="showChunks && totalChunks > 0">
      <div class="chunks-header">
        <span class="chunks-title">分片详情</span>
        <span class="chunks-count">共 {{ totalChunks }} 个分片</span>
      </div>
      <div class="chunks-grid">
        <div
          v-for="chunk in chunks"
          :key="chunk.index"
          :class="['chunk-item', `chunk-${chunk.status}`]"
          :title="getChunkTooltip(chunk)"
          @click="handleChunkClick(chunk)"
        >
          <div class="chunk-index">{{ chunk.index + 1 }}</div>
          <div class="chunk-status-icon">
            <el-icon v-if="chunk.status === 'completed'" :size="14" color="#67C23A">
              <Check />
            </el-icon>
            <el-icon v-else-if="chunk.status === 'uploading'" :size="14" color="#409EFF" class="spin">
              <Loading />
            </el-icon>
            <el-icon v-else-if="chunk.status === 'failed'" :size="14" color="#F56C6C">
              <Close />
            </el-icon>
            <el-icon v-else :size="14" color="#C0C4CC">
              <Clock />
            </el-icon>
          </div>
          <div class="chunk-progress-bar" v-if="chunk.status === 'uploading'">
            <div class="chunk-progress-inner" :style="{ width: chunk.progress + '%' }"></div>
          </div>
        </div>
      </div>
      <div class="chunks-legend">
        <span class="legend-item">
          <span class="legend-dot waiting"></span>等待中
        </span>
        <span class="legend-item">
          <span class="legend-dot uploading"></span>上传中
        </span>
        <span class="legend-item">
          <span class="legend-dot completed"></span>已完成
        </span>
        <span class="legend-item">
          <span class="legend-dot failed"></span>失败
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Document,
  DataLine,
  Clock,
  Files,
  VideoPause,
  VideoPlay,
  RefreshRight,
  Close,
  Check,
  Loading
} from '@element-plus/icons-vue'

const props = defineProps({
  fileName: { type: String, default: '' },
  fileSize: { type: Number, default: 0 },
  uploadedSize: { type: Number, default: 0 },
  progress: { type: Number, default: 0 },
  speed: { type: Number, default: 0 },
  remainingTime: { type: Number, default: 0 },
  status: { type: String, default: 'idle' },
  totalChunks: { type: Number, default: 0 },
  completedCount: { type: Number, default: 0 },
  chunks: { type: Array, default: () => [] },
  failedChunks: { type: Array, default: () => [] },
  error: { type: String, default: '' },
  showActions: { type: Boolean, default: true },
  showChunks: { type: Boolean, default: true }
})

const emit = defineEmits(['pause', 'resume', 'cancel', 'retry-all', 'retry-chunk'])

const statusText = computed(() => {
  const map = {
    idle: '等待上传',
    initializing: '初始化中',
    uploading: '上传中',
    paused: '已暂停',
    merging: '合并中',
    completed: '上传完成',
    error: '上传失败',
    cancelled: '已取消',
    cancelling: '取消中'
  }
  return map[props.status] || props.status
})

const statusType = computed(() => {
  const map = {
    idle: 'info',
    initializing: 'info',
    uploading: 'primary',
    paused: 'warning',
    merging: 'warning',
    completed: 'success',
    error: 'danger',
    cancelled: 'info',
    cancelling: 'info'
  }
  return map[props.status] || 'info'
})

const progressStatus = computed(() => {
  if (props.status === 'completed') return 'success'
  if (props.status === 'error') return 'exception'
  return ''
})

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

const formatTime = (seconds) => {
  if (seconds <= 0 || !isFinite(seconds)) return '计算中...'
  if (seconds < 60) return Math.round(seconds) + ' 秒'
  if (seconds < 3600) {
    const mins = Math.floor(seconds / 60)
    const secs = Math.round(seconds % 60)
    return `${mins} 分 ${secs} 秒`
  }
  const hours = Math.floor(seconds / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  return `${hours} 小时 ${mins} 分`
}

const getChunkTooltip = (chunk) => {
  const sizeStr = formatFileSize(chunk.size)
  const statusMap = {
    waiting: '等待上传',
    uploading: `上传中 ${chunk.progress}%`,
    completed: '上传完成',
    failed: '上传失败 - 点击重试'
  }
  return `分片 ${chunk.index + 1} (${sizeStr}) - ${statusMap[chunk.status] || chunk.status}`
}

const handleChunkClick = (chunk) => {
  if (chunk.status === 'failed') {
    emit('retry-chunk', chunk.index)
  }
}
</script>

<style scoped>
.chunk-upload-progress {
  width: 100%;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.file-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ecf5ff;
  border-radius: 8px;
}

.file-detail {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 13px;
  color: #909399;
}

.meta-sep {
  margin: 0 8px;
  color: #dcdfe6;
}

.file-status {
  flex-shrink: 0;
}

.main-progress {
  margin-bottom: 16px;
}

.progress-stats {
  display: flex;
  gap: 24px;
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-left: auto;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.error-message {
  margin-bottom: 16px;
}

.chunks-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.chunks-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chunks-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.chunks-count {
  font-size: 12px;
  color: #909399;
}

.chunks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(50px, 1fr));
  gap: 6px;
  margin-bottom: 12px;
}

.chunk-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50px;
  border-radius: 6px;
  background: #f5f7fa;
  cursor: default;
  transition: all 0.2s;
  overflow: hidden;
}

.chunk-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chunk-waiting {
  background: #f5f7fa;
}

.chunk-uploading {
  background: #ecf5ff;
}

.chunk-completed {
  background: #f0f9eb;
}

.chunk-failed {
  background: #fef0f0;
  cursor: pointer;
}

.chunk-failed:hover {
  background: #fde2e2;
}

.chunk-index {
  font-size: 11px;
  color: #606266;
  font-weight: 500;
  margin-bottom: 2px;
}

.chunk-status-icon {
  position: relative;
  z-index: 1;
}

.chunk-progress-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #d9ecff;
  overflow: hidden;
}

.chunk-progress-inner {
  height: 100%;
  background: #409eff;
  transition: width 0.2s;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.chunks-legend {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.waiting {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
}

.legend-dot.uploading {
  background: #409eff;
}

.legend-dot.completed {
  background: #67c23a;
}

.legend-dot.failed {
  background: #f56c6c;
}

@media (max-width: 600px) {
  .progress-stats {
    flex-direction: column;
    gap: 10px;
  }

  .chunks-grid {
    grid-template-columns: repeat(auto-fill, minmax(40px, 1fr));
    gap: 4px;
  }

  .chunk-item {
    height: 40px;
  }
}
</style>
