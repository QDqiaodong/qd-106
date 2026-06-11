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
            <el-icon :size="20" color="#409EFF"><Clock /></el-icon>
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
            <el-icon><Right /></el-icon>
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

      <el-card class="correction-card">
        <template #header>
          <div class="correction-header">
            <div class="header-left">
              <el-icon :size="18" color="#F56C6C"><Warning /></el-icon>
              <span class="section-title">资料勘误</span>
              <span class="correction-count">{{ correctionList.length }} 条</span>
            </div>
            <el-button type="danger" size="small" @click="showCorrectionDialog = true">
              <el-icon><Edit /></el-icon>
              提交勘误
            </el-button>
          </div>
        </template>
        <div class="correction-content" v-loading="correctionLoading">
          <div v-if="correctionList.length === 0" class="correction-empty">
            <el-icon :size="48" color="#c0c4cc"><DocumentDelete /></el-icon>
            <p class="empty-text">暂无勘误记录</p>
            <p class="empty-tip">发现资料有错误？点击右上角"提交勘误"帮助我们改进</p>
          </div>
          <div v-else class="correction-list">
            <div
              v-for="item in correctionList"
              :key="item.id"
              class="correction-item"
            >
              <div class="correction-status">
                <el-tag v-if="item.status === 0" type="warning" size="small">待处理</el-tag>
                <el-tag v-else-if="item.status === 1" type="success" size="small">已采纳</el-tag>
                <el-tag v-else type="info" size="small">已驳回</el-tag>
              </div>
              <div class="correction-body">
                <div class="correction-meta">
                  <span v-if="item.pageNumber" class="page-badge">第 {{ item.pageNumber }} 页</span>
                  <span class="submitter">{{ item.submitterNickname || '匿名用户' }}</span>
                  <span class="submit-time">{{ formatDateTime(item.createdAt) }}</span>
                </div>
                <div class="correction-desc">
                  <span class="label">错误说明：</span>
                  <span class="content">{{ item.errorDescription }}</span>
                </div>
                <div v-if="item.correctionSuggestion" class="correction-suggestion">
                  <span class="label">修正建议：</span>
                  <span class="content">{{ item.correctionSuggestion }}</span>
                </div>
                <div v-if="item.status !== 0 && item.handleRemark" class="correction-handle">
                  <span class="label">{{ item.status === 1 ? '采纳说明：' : '驳回理由：' }}</span>
                  <span class="content">{{ item.handleRemark }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="pagination-wrapper" v-if="correctionTotal > correctionPageSize">
            <el-pagination
              v-model:current-page="correctionPage"
              v-model:page-size="correctionPageSize"
              :page-sizes="[5, 10, 20]"
              :total="correctionTotal"
              layout="total, prev, pager, next"
              @current-change="handleCorrectionPageChange"
            />
          </div>
        </div>
      </el-card>

      <el-dialog v-model="showCorrectionDialog" title="提交勘误" width="520px" :close-on-click-modal="false">
        <el-form :model="correctionForm" label-width="90px" ref="correctionFormRef">
          <el-form-item label="错误页码" prop="pageNumber">
            <el-input-number
              v-model="correctionForm.pageNumber"
              :min="1"
              :max="99999"
              placeholder="请输入错误所在页码"
              style="width: 100%"
              :controls="false"
            />
          </el-form-item>
          <el-form-item label="错误说明" prop="errorDescription">
            <el-input
              v-model="correctionForm.errorDescription"
              type="textarea"
              :rows="3"
              placeholder="请详细描述错误内容，如错别字、数据错误、内容缺失等"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="修正建议" prop="correctionSuggestion">
            <el-input
              v-model="correctionForm.correctionSuggestion"
              type="textarea"
              :rows="3"
              placeholder="请提供您认为正确的内容或修改建议（选填）"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCorrectionDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitCorrection" :loading="submittingCorrection">
            提交勘误
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Star, StarFilled, Download, User, Clock, View, Document,
  Collection, Plus, Flag, Delete, Notebook, ShoppingCart, ShoppingCartFull,
  VideoPlay, Files, Right, Warning, Edit, DocumentDelete
} from '@element-plus/icons-vue'
import {
  getMaterialDetail, getCategoryList, getGradeList, getSubjectList,
  getBookmarks, addBookmark, deleteBookmark,
  saveReadingProgress,
  submitCorrection, getCorrectionsByMaterial
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

const correctionList = ref([])
const correctionLoading = ref(false)
const correctionPage = ref(1)
const correctionPageSize = ref(10)
const correctionTotal = ref(0)
const showCorrectionDialog = ref(false)
const submittingCorrection = ref(false)
const correctionForm = ref({
  pageNumber: null,
  errorDescription: '',
  correctionSuggestion: ''
})

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
  return dateStr.replace('T', ' ').substring(0, 16)
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

const loadCorrections = async () => {
  correctionLoading.value = true
  try {
    const res = await getCorrectionsByMaterial(materialId.value, {
      page: correctionPage.value,
      size: correctionPageSize.value
    })
    correctionList.value = res.data?.list || []
    correctionTotal.value = res.data?.total || 0
  } catch (e) {
    console.error('加载勘误列表失败', e)
  } finally {
    correctionLoading.value = false
  }
}

const handleCorrectionPageChange = (page) => {
  correctionPage.value = page
  loadCorrections()
}

const handleSubmitCorrection = async () => {
  const form = correctionForm.value
  if (!form.errorDescription || !form.errorDescription.trim()) {
    ElMessage.warning('请填写错误说明')
    return
  }

  submittingCorrection.value = true
  try {
    const data = {
      materialId: materialId.value,
      errorDescription: form.errorDescription.trim()
    }
    if (form.pageNumber && form.pageNumber > 0) {
      data.pageNumber = form.pageNumber
    }
    if (form.correctionSuggestion && form.correctionSuggestion.trim()) {
      data.correctionSuggestion = form.correctionSuggestion.trim()
    }

    await submitCorrection(data)
    ElMessage.success('勘误提交成功，感谢您的反馈！')
    showCorrectionDialog.value = false
    correctionForm.value = {
      pageNumber: null,
      errorDescription: '',
      correctionSuggestion: ''
    }
    correctionPage.value = 1
    loadCorrections()
  } catch (e) {
    console.error('提交勘误失败', e)
    ElMessage.error(e.response?.data?.message || '提交失败，请重试')
  } finally {
    submittingCorrection.value = false
  }
}

onMounted(() => {
  syncFavoriteFromStore()
  loadDicts()
  loadDetail()
  loadBookmarks()
  loadCorrections()
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

.correction-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.correction-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.correction-count {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

.correction-content {
  min-height: 120px;
}

.correction-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 10px;
}

.correction-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.correction-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  transition: all 0.2s ease;
}

.correction-item:hover {
  background: #fff5f5;
  border-color: #fbc4c4;
}

.correction-status {
  margin-bottom: 10px;
}

.correction-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.correction-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.correction-meta .submitter {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.correction-meta .submit-time {
  font-size: 12px;
  color: #c0c4cc;
}

.correction-desc,
.correction-suggestion,
.correction-handle {
  display: flex;
  gap: 6px;
  line-height: 1.6;
}

.correction-desc .label,
.correction-suggestion .label,
.correction-handle .label {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.correction-desc .content,
.correction-suggestion .content {
  font-size: 14px;
  color: #303133;
  flex: 1;
}

.correction-handle {
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}

.correction-handle .content {
  font-size: 13px;
  color: #67c23a;
  flex: 1;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 20px;
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

  .correction-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .correction-header .el-button {
    width: 100%;
  }

  .correction-meta {
    gap: 6px;
  }
}
</style>
