<template>
  <div class="detail-page">
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回列表</el-button>
    </div>

    <div class="detail-content" v-loading="loading">
      <el-card class="info-card">
        <div class="info-header">
          <div class="title-section">
            <h1 class="material-title">{{ detail.title }}</h1>
            <div class="title-tags">
              <el-tag size="small" type="success">{{ getCategoryName(detail.categoryId) }}</el-tag>
              <el-tag size="small">{{ getGradeName(detail.gradeId) }}</el-tag>
              <el-tag size="small" type="warning">{{ getSubjectName(detail.subjectId) }}</el-tag>
            </div>
          </div>
          <div class="action-buttons">
            <el-button
              :type="isFavorited ? 'warning' : 'default'"
              size="large"
              @click="handleFavorite"
            >
              <el-icon>
                <StarFilled v-if="isFavorited" />
                <Star v-else />
              </el-icon>
              {{ isFavorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button type="primary" size="large" @click="handleDownload">
              <el-icon><Download /></el-icon>
              下载资料
            </el-button>
          </div>
        </div>

        <div class="info-meta">
          <div class="meta-item">
            <el-icon color="#909399"><User /></el-icon>
            <span>上传者：管理员</span>
          </div>
          <div class="meta-item">
            <el-icon color="#909399"><Clock /></el-icon>
            <span>上传时间：{{ formatDate(detail.createdAt) }}</span>
          </div>
          <div class="meta-item">
            <el-icon color="#909399"><View /></el-icon>
            <span>浏览次数：{{ detail.viewCount || 0 }}</span>
          </div>
          <div class="meta-item">
            <el-icon color="#909399"><Download /></el-icon>
            <span>下载次数：{{ detail.downloadCount || 0 }}</span>
          </div>
        </div>

        <el-divider />

        <div class="description-section">
          <h3 class="section-title">资料描述</h3>
          <p class="description-text">{{ detail.description || '暂无描述' }}</p>
        </div>
      </el-card>

      <el-card class="preview-card">
        <template #header>
          <div class="preview-header">
            <el-icon :size="18" color="#409EFF"><View /></el-icon>
            <span class="section-title">资料预览</span>
          </div>
        </template>
        <div class="preview-container">
          <iframe
            v-if="previewUrl"
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
          ></iframe>
          <div v-else class="preview-placeholder">
            <el-icon :size="80" color="#c0c4cc"><Document /></el-icon>
            <p class="placeholder-text">暂无预览内容</p>
            <p class="placeholder-tip">上传的资料支持在线预览</p>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Star, StarFilled, Download, User, Clock, View, Document } from '@element-plus/icons-vue'
import { getMaterialDetail, getCategoryList, getGradeList, getSubjectList } from '@/api/material'
import { useAppStore } from '@/store'

const route = useRoute()
const appStore = useAppStore()
const materialId = computed(() => Number(route.params.id))

const detail = ref({})
const previewUrl = ref('')
const loading = ref(false)
const categories = ref([])
const grades = ref([])
const subjects = ref([])

const categoryMap = ref({})
const gradeMap = ref({})
const subjectMap = ref({})

const isFavorited = ref(false)

const syncFavoriteFromStore = () => {
  isFavorited.value = appStore.isFavorite(materialId.value)
}

const loadDicts = async () => {
  try {
    const [catRes, gradeRes, subRes] = await Promise.all([
      getCategoryList(),
      getGradeList(),
      getSubjectList()
    ])
    categories.value = catRes.data || []
    grades.value = gradeRes.data || []
    subjects.value = subRes.data || []
    categories.value.forEach(c => { categoryMap.value[c.id] = c.name })
    grades.value.forEach(g => { gradeMap.value[g.id] = g.name })
    subjects.value.forEach(s => { subjectMap.value[s.id] = s.name })
  } catch (e) {
    console.error('加载字典失败', e)
  }
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getMaterialDetail(materialId.value)
    detail.value = res.data || {}
    
    if (detail.value.favorited !== undefined) {
      isFavorited.value = detail.value.favorited
      appStore.setFavorite(materialId.value, detail.value.favorited)
    }
    
    if (detail.value.fileUrl) {
      previewUrl.value = detail.value.fileUrl
    }
  } catch (e) {
    console.error('加载详情失败', e)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const getCategoryName = (id) => categoryMap.value[id] || '未分类'
const getGradeName = (id) => gradeMap.value[id] || '未指定'
const getSubjectName = (id) => subjectMap.value[id] || '未指定'

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

const handleFavorite = async () => {
  const oldValue = isFavorited.value
  isFavorited.value = !oldValue
  try {
    const newFavorited = await appStore.toggleFavorite(materialId.value)
    isFavorited.value = newFavorited
    ElMessage.success(newFavorited ? '收藏成功' : '已取消收藏')
  } catch (e) {
    isFavorited.value = oldValue
    ElMessage.error(e.response?.data?.message || '操作失败，请重试')
  }
}

const handleDownload = () => {
  if (detail.value.fileUrl) {
    window.open(detail.value.fileUrl, '_blank')
  } else {
    ElMessage.warning('该资料暂无可下载文件')
  }
}

onMounted(() => {
  syncFavoriteFromStore()
  loadDicts()
  loadDetail()
})
</script>

<style scoped>
.detail-page {
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

.info-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}

.title-section {
  flex: 1;
}

.material-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.4;
}

.title-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.info-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 16px 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.description-section {
  padding-top: 8px;
}

.description-text {
  color: #606266;
  font-size: 15px;
  line-height: 1.8;
  text-indent: 2em;
}

.preview-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-container {
  min-height: 600px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.preview-iframe {
  width: 100%;
  height: 600px;
  border: none;
}

.preview-placeholder {
  height: 600px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #fafafa;
}

.placeholder-text {
  font-size: 16px;
  color: #909399;
}

.placeholder-tip {
  font-size: 14px;
  color: #c0c4cc;
}

@media (max-width: 768px) {
  .info-header {
    flex-direction: column;
  }
  
  .action-buttons {
    width: 100%;
    justify-content: flex-start;
  }
  
  .info-meta {
    gap: 12px;
  }
  
  .preview-iframe,
  .preview-placeholder {
    height: 400px;
  }
}
</style>
