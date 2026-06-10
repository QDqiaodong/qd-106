<template>
  <div class="detail-page">
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回列表</el-button>
    </div>

    <div class="detail-content" v-loading="loading">
      <div
        v-if="readingProgress && readingProgress.pageNumber > 0"
        class="reading-progress-bar"
      >
        <div class="progress-left">
          <div class="progress-icon">
            <el-icon :size="20"><Timer /></el-icon>
          </div>
          <div class="progress-info">
            <div class="progress-title">
              <span>上次读到</span>
              <span class="page-highlight">第 {{ readingProgress.pageNumber }} 页</span>
              <span class="total-pages">
                <el-icon :size="12"><Files /></el-icon>
                共 {{ detail.totalPages || '--' }} 页
              </span>
            </div>
            <div class="progress-time">
              <el-icon :size="12"><Clock /></el-icon>
              最近阅读：{{ formatDateTime(readingProgress.lastReadAt) }}
            </div>
          </div>
        </div>
        <div class="progress-right">
          <el-button type="primary" size="default" @click="jumpToLastRead">
            <el-icon><VideoPlay /></el-icon>
            继续阅读
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>

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
              :type="isInBasket ? 'success' : 'default'"
              size="large"
              @click="handleToggleBasket"
            >
              <el-icon>
                <ShoppingCartFull v-if="isInBasket" />
                <ShoppingCart v-else />
              </el-icon>
              {{ isInBasket ? '已加入资料篮' : '加入资料篮' }}
            </el-button>
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
            :src="iframeSrc"
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

      <el-card class="bookmark-card">
        <template #header>
          <div class="bookmark-header">
            <div class="header-left">
              <el-icon :size="18" color="#E6A23C"><Collection /></el-icon>
              <span class="section-title">阅读书签</span>
              <span class="bookmark-count">{{ bookmarks.length }} 个</span>
            </div>
            <el-button type="primary" size="small" @click="showAddDialog = true">
              <el-icon><Plus /></el-icon>
              添加书签
            </el-button>
          </div>
        </template>
        <div class="bookmark-content">
          <div v-if="bookmarks.length === 0" class="bookmark-empty">
            <el-icon :size="48" color="#c0c4cc"><Notebook /></el-icon>
            <p class="empty-text">暂无书签</p>
            <p class="empty-tip">添加书签，方便复习时快速定位重点页</p>
          </div>
          <div v-else class="bookmark-list">
            <div
              v-for="item in bookmarks"
              :key="item.id"
              class="bookmark-item"
              @click="jumpToBookmark(item)"
            >
              <div class="bookmark-icon">
                <el-icon :size="20" color="#E6A23C"><Flag /></el-icon>
              </div>
              <div class="bookmark-info">
                <div class="bookmark-title">
                  <span v-if="item.pageNumber" class="page-badge">第 {{ item.pageNumber }} 页</span>
                  <span v-if="item.chapterName" class="chapter-name">{{ item.chapterName }}</span>
                </div>
                <p v-if="item.note" class="bookmark-note">{{ item.note }}</p>
                <span class="bookmark-time">{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="bookmark-actions">
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click.stop="handleDeleteBookmark(item.id)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-dialog v-model="showAddDialog" title="添加书签" width="420px">
        <el-form :model="bookmarkForm" label-width="80px">
          <el-form-item label="页码">
            <el-input-number
              v-model="bookmarkForm.pageNumber"
              :min="1"
              :max="9999"
              placeholder="请输入页码"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="章节名称">
            <el-input
              v-model="bookmarkForm.chapterName"
              placeholder="请输入章节名称（选填）"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="bookmarkForm.note"
              type="textarea"
              :rows="2"
              placeholder="简短备注，标记重点内容（选填）"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="handleAddBookmark" :loading="addingBookmark">
            保存
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Star, StarFilled, Download, User, Clock, View, Document,
  Collection, Plus, Flag, Delete, Notebook, ShoppingCart, ShoppingCartFull,
  VideoPlay, Timer, Files, ArrowRight
} from '@element-plus/icons-vue'
import {
  getMaterialDetail, getCategoryList, getGradeList, getSubjectList,
  getBookmarks, addBookmark, deleteBookmark,
  saveReadingProgress
} from '@/api/material'
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
const isInBasket = computed(() => appStore.isInBasket(materialId.value))

const bookmarks = ref([])
const showAddDialog = ref(false)
const addingBookmark = ref(false)
const bookmarkForm = ref({
  pageNumber: null,
  chapterName: '',
  note: ''
})

const readingProgress = ref(null)
const currentPage = ref(1)
const savingProgress = ref(false)
const progressSaveTimer = ref(null)

const iframeSrc = computed(() => {
  if (!previewUrl.value) return ''
  return previewUrl.value
})

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
    
    if (detail.value.readingProgress) {
      readingProgress.value = detail.value.readingProgress
      currentPage.value = detail.value.readingProgress.pageNumber || 1
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

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return `${Math.floor(diff / minute)} 分钟前`
  } else if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`
  } else if (diff < 7 * day) {
    return `${Math.floor(diff / day)} 天前`
  } else {
    return dateStr.replace('T', ' ').substring(0, 16)
  }
}

const jumpToLastRead = () => {
  if (!previewUrl.value || !readingProgress.value) {
    return
  }
  const pageNumber = readingProgress.value.pageNumber
  if (!pageNumber || pageNumber <= 0) {
    ElMessage.warning('无效的页码')
    return
  }
  const separator = previewUrl.value.includes('#') ? '&' : '#'
  const newSrc = previewUrl.value + separator + 'page=' + pageNumber
  const iframe = document.querySelector('.preview-iframe')
  if (iframe) {
    iframe.src = newSrc
  }
  currentPage.value = pageNumber
  ElMessage.success(`已回到上次阅读位置：第 ${pageNumber} 页`)
}

const saveCurrentProgress = async (pageNum) => {
  if (!pageNum || pageNum <= 0) return
  if (savingProgress.value) return
  savingProgress.value = true
  try {
    await saveReadingProgress(materialId.value, pageNum)
    if (readingProgress.value) {
      readingProgress.value.pageNumber = pageNum
      readingProgress.value.lastReadAt = new Date().toISOString()
    } else {
      readingProgress.value = {
        pageNumber: pageNum,
        lastReadAt: new Date().toISOString()
      }
    }
  } catch (e) {
    console.error('保存阅读进度失败', e)
  } finally {
    savingProgress.value = false
  }
}

const handleIframePageChange = () => {
  if (progressSaveTimer.value) {
    clearTimeout(progressSaveTimer.value)
  }
  progressSaveTimer.value = setTimeout(() => {
    const iframe = document.querySelector('.preview-iframe')
    if (iframe) {
      try {
        const iframeSrc = iframe.src
        const pageMatch = iframeSrc.match(/[#&]page=(\d+)/)
        if (pageMatch && pageMatch[1]) {
          const page = parseInt(pageMatch[1])
          if (page !== currentPage.value && page > 0) {
            currentPage.value = page
            saveCurrentProgress(page)
          }
        }
      } catch (e) {
        console.error('检测页码变化失败', e)
      }
    }
  }, 1000)
}

const setupIframeListener = () => {
  const iframe = document.querySelector('.preview-iframe')
  if (iframe) {
    iframe.addEventListener('load', () => {
      try {
        const iframeSrc = iframe.src
        const pageMatch = iframeSrc.match(/[#&]page=(\d+)/)
        if (pageMatch && pageMatch[1]) {
          const page = parseInt(pageMatch[1])
          if (page > 0) {
            currentPage.value = page
          }
        }
      } catch (e) {
        console.error('获取初始页码失败', e)
      }
    })
    const checkSrcChange = setInterval(() => {
      handleIframePageChange()
    }, 2000)
    progressSaveTimer.value = checkSrcChange
  }
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

const handleToggleBasket = () => {
  const d = detail.value
  if (appStore.isInBasket(materialId.value)) {
    appStore.removeFromBasket(materialId.value)
    ElMessage.success('已从资料篮移除')
  } else {
    const material = {
      ...d,
      categoryName: getCategoryName(d.categoryId),
      gradeName: getGradeName(d.gradeId),
      subjectName: getSubjectName(d.subjectId)
    }
    const added = appStore.addToBasket(material)
    if (added) {
      ElMessage.success('已加入本轮复习资料篮')
    }
  }
}

const loadBookmarks = async () => {
  try {
    const res = await getBookmarks(materialId.value)
    bookmarks.value = res.data || []
  } catch (e) {
    console.error('加载书签失败', e)
  }
}

const handleAddBookmark = async () => {
  const form = bookmarkForm.value
  if ((!form.pageNumber || form.pageNumber <= 0) && !form.chapterName.trim()) {
    ElMessage.warning('请填写页码或章节名称')
    return
  }

  addingBookmark.value = true
  try {
    const data = {}
    if (form.pageNumber && form.pageNumber > 0) {
      data.pageNumber = form.pageNumber
    }
    if (form.chapterName.trim()) {
      data.chapterName = form.chapterName.trim()
    }
    if (form.note.trim()) {
      data.note = form.note.trim()
    }

    await addBookmark(materialId.value, data)
    ElMessage.success('书签添加成功')
    showAddDialog.value = false
    bookmarkForm.value = {
      pageNumber: null,
      chapterName: '',
      note: ''
    }
    loadBookmarks()
  } catch (e) {
    console.error('添加书签失败', e)
    ElMessage.error(e.response?.data?.message || '添加失败，请重试')
  } finally {
    addingBookmark.value = false
  }
}

const handleDeleteBookmark = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个书签吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteBookmark(materialId.value, id)
    ElMessage.success('删除成功')
    loadBookmarks()
  } catch (e) {
    console.error('删除书签失败', e)
    ElMessage.error('删除失败，请重试')
  }
}

const jumpToBookmark = (bookmark) => {
  if (!previewUrl.value) {
    ElMessage.warning('暂无预览文件')
    return
  }
  if (bookmark.pageNumber) {
    const separator = previewUrl.value.includes('#') ? '&' : '#'
    const newSrc = previewUrl.value + separator + 'page=' + bookmark.pageNumber
    const iframe = document.querySelector('.preview-iframe')
    if (iframe) {
      iframe.src = newSrc
    }
    ElMessage.success(`已跳转到第 ${bookmark.pageNumber} 页`)
  } else {
    ElMessage.info('该书签未设置页码')
  }
}

onMounted(() => {
  syncFavoriteFromStore()
  loadDicts()
  loadDetail()
  loadBookmarks()
  nextTick(() => {
    setupIframeListener()
  })
})

onUnmounted(() => {
  if (progressSaveTimer.value) {
    clearTimeout(progressSaveTimer.value)
    clearInterval(progressSaveTimer.value)
    progressSaveTimer.value = null
  }
  if (currentPage.value > 0) {
    saveCurrentProgress(currentPage.value)
  }
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

.reading-progress-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  border: 1px solid #b3d8ff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.progress-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.progress-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: #409eff;
  border-radius: 50%;
  color: #fff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.progress-info {
  flex: 1;
  min-width: 0;
}

.progress-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}

.page-highlight {
  display: inline-block;
  padding: 2px 10px;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border-radius: 4px;
}

.total-pages {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}

.progress-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.progress-right {
  flex-shrink: 0;
  margin-left: 16px;
}

.progress-right .el-button {
  border-radius: 8px;
  padding: 10px 18px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.25);
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

.bookmark-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.bookmark-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bookmark-count {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

.bookmark-content {
  min-height: 120px;
}

.bookmark-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 10px;
}

.empty-text {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

.empty-tip {
  font-size: 13px;
  color: #c0c4cc;
  margin: 0;
}

.bookmark-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bookmark-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.bookmark-item:hover {
  background: #f0f9ff;
  border-color: #b3d8ff;
}

.bookmark-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: #fdf6ec;
  border-radius: 50%;
}

.bookmark-info {
  flex: 1;
  min-width: 0;
}

.bookmark-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.page-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 13px;
  border-radius: 4px;
  font-weight: 500;
}

.chapter-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.bookmark-note {
  font-size: 13px;
  color: #606266;
  margin: 4px 0 6px 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.bookmark-time {
  font-size: 12px;
  color: #c0c4cc;
}

.bookmark-actions {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .reading-progress-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
  }

  .progress-right {
    width: 100%;
    margin-left: 0;
  }

  .progress-right .el-button {
    width: 100%;
  }

  .progress-icon {
    width: 40px;
    height: 40px;
  }

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

  .bookmark-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .bookmark-header .el-button {
    width: 100%;
  }

  .bookmark-item {
    padding: 12px;
  }

  .bookmark-icon {
    width: 32px;
    height: 32px;
  }
}
</style>
