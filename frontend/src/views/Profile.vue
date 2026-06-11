<template>
  <div class="profile-page">
    <el-card class="user-card">
      <div class="user-info">
        <el-avatar :size="80" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
        <div class="user-details">
          <h2 class="username">管理员</h2>
          <div class="user-stats">
            <span class="stat-item">
              <el-icon color="#409EFF"><Upload /></el-icon>
              上传 {{ uploadCount }} 份资料
            </span>
            <span class="stat-item">
              <el-icon color="#E6A23C"><Star /></el-icon>
              收藏 {{ favoriteCount }} 份资料
            </span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="tabs-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的上传" name="uploads">
          <div class="list-container" v-loading="loading">
            <div class="upload-toolbar">
              <div class="toolbar-left">
                <el-radio-group v-model="uploadGroupBy" size="small" @change="handleGroupByChange">
                  <el-radio-button value="date">按日期分组</el-radio-button>
                  <el-radio-button value="subject">按学科分组</el-radio-button>
                </el-radio-group>
              </div>
              <div class="toolbar-right">
                <el-button size="small" @click="toggleAllUploadGroups">
                  {{ allUploadGroupsExpanded ? '全部收起' : '全部展开' }}
                </el-button>
              </div>
            </div>

            <el-empty v-if="uploadList.length === 0" description="暂无上传资料" />
            <div v-else class="upload-groups">
              <div
                v-for="group in uploadGroups"
                :key="group.key"
                class="upload-group"
              >
                <div class="group-header" @click="toggleUploadGroup(group.key)">
                  <div class="group-header-left">
                    <el-icon class="collapse-icon" :class="{ expanded: expandedUploadGroups[group.key] }">
                      <ArrowRight />
                    </el-icon>
                    <el-icon :size="20" :color="group.iconColor" class="group-icon">
                      <component :is="group.icon" />
                    </el-icon>
                    <span class="group-title">{{ group.title }}</span>
                    <el-tag size="small" type="info" class="group-count">
                      {{ group.items.length }} 份
                    </el-tag>
                  </div>
                  <div class="group-header-right">
                    <span class="group-meta">{{ group.meta }}</span>
                  </div>
                </div>

                <div
                  class="group-content"
                  :class="{ collapsed: !expandedUploadGroups[group.key] }"
                >
                  <div class="material-list">
                    <div
                      v-for="item in group.items"
                      :key="item.id"
                      class="list-item"
                    >
                      <div class="item-main" @click="goToDetail(item.id)">
                        <div class="item-icon">
                          <el-icon :size="24" color="#409EFF"><Document /></el-icon>
                        </div>
                        <div class="item-content">
                          <h3 class="item-title">{{ item.title }}</h3>
                          <p class="item-desc">{{ item.description || '暂无描述' }}</p>
                          <div class="item-tags">
                            <el-tag size="small" type="success">{{ getCategoryName(item.categoryId) }}</el-tag>
                            <el-tag size="small">{{ getGradeName(item.gradeId) }}</el-tag>
                            <el-tag size="small" type="warning">{{ getSubjectName(item.subjectId) }}</el-tag>
                          </div>
                        </div>
                      </div>
                      <div class="item-side">
                        <div class="item-meta">
                          <span class="meta-item">
                            <el-icon><View /></el-icon>
                            {{ item.viewCount || 0 }}
                          </span>
                          <span class="meta-item">
                            <el-icon><Download /></el-icon>
                            {{ item.downloadCount || 0 }}
                          </span>
                        </div>
                        <div class="item-actions">
                          <span class="item-time">{{ formatDate(item.createdAt) }}</span>
                          <el-button
                            v-if="item.status === 1"
                            type="danger"
                            size="small"
                            text
                            @click.stop="handleDelete(item.id)"
                          >
                            <el-icon><Delete /></el-icon>
                            下架
                          </el-button>
                          <el-tag v-else type="info" size="small">已下架</el-tag>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="pagination-wrapper" v-if="uploadList.length > 0">
              <el-pagination
                v-model:current-page="uploadPage"
                v-model:page-size="pageSize"
                :page-sizes="[5, 10, 20]"
                :total="uploadTotal"
                layout="total, prev, pager, next"
                @current-change="handleUploadPageChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="勘误管理" name="corrections">
          <div class="list-container" v-loading="correctionLoading">
            <div class="correction-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-title">勘误管理中心</span>
                <span class="toolbar-subtitle">处理用户提交的资料勘误反馈</span>
              </div>
            </div>

            <div class="correction-stats">
              <div
                v-for="stat in correctionStats"
                :key="stat.status"
                class="correction-stat-card"
                :class="{ active: activeCorrectionFilter === stat.status }"
                @click="setCorrectionFilter(stat.status)"
              >
                <div class="stat-icon" :style="{ background: stat.bgColor }">
                  <el-icon :size="20" :color="stat.iconColor">
                    <component :is="stat.icon" />
                  </el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ stat.count }}</div>
                  <div class="stat-label">{{ stat.label }}</div>
                </div>
              </div>
            </div>

            <el-empty v-if="correctionList.length === 0" description="暂无勘误记录" />
            <div v-else class="correction-sections">
              <div
                v-for="item in correctionList"
                :key="item.id"
                class="correction-card-item"
              >
                <div class="card-header">
                  <div class="card-title" @click="goToDetail(item.materialId)">
                    <el-icon :size="18" color="#409EFF"><Document /></el-icon>
                    <span class="material-name">{{ item.materialTitle || '未知资料' }}</span>
                  </div>
                  <div class="card-status">
                    <el-tag v-if="item.status === 0" type="warning" size="small">待处理</el-tag>
                    <el-tag v-else-if="item.status === 1" type="success" size="small">已采纳</el-tag>
                    <el-tag v-else type="info" size="small">已驳回</el-tag>
                  </div>
                </div>

                <div class="card-body">
                  <div class="card-meta">
                    <span v-if="item.pageNumber" class="page-badge">第 {{ item.pageNumber }} 页</span>
                    <span class="submitter">{{ item.submitterNickname || '匿名用户' }}</span>
                    <span class="submit-time">{{ formatDateTime(item.createdAt) }}</span>
                  </div>
                  <div class="card-desc">
                    <span class="label">错误说明：</span>
                    <span class="content">{{ item.errorDescription }}</span>
                  </div>
                  <div v-if="item.correctionSuggestion" class="card-suggestion">
                    <span class="label">修正建议：</span>
                    <span class="content">{{ item.correctionSuggestion }}</span>
                  </div>
                  <div v-if="item.status !== 0" class="card-handle">
                    <span class="label">{{ item.status === 1 ? '采纳说明：' : '驳回理由：' }}</span>
                    <span class="content">{{ item.handleRemark || '无' }}</span>
                    <span class="handle-time" v-if="item.handledAt">处理于 {{ formatDateTime(item.handledAt) }}</span>
                  </div>
                </div>

                <div class="card-footer" v-if="item.status === 0">
                  <el-button
                    size="small"
                    type="success"
                    @click="openHandleDialog(item, 1)"
                  >
                    <el-icon><Check /></el-icon>
                    采纳
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    @click="openHandleDialog(item, 2)"
                  >
                    <el-icon><Close /></el-icon>
                    驳回
                  </el-button>
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
        </el-tab-pane>

        <el-tab-pane label="我的收藏" name="favorites">
          <div class="list-container" v-loading="favLoading">
            <div class="favorite-toolbar">
              <div class="toolbar-left">
                <span class="toolbar-title">复习勾选板</span>
                <span class="toolbar-subtitle">点击标记管理你的复习进度</span>
              </div>
            </div>

            <div class="review-stats">
              <div
                v-for="stat in reviewStats"
                :key="stat.status"
                class="review-stat-card"
                :class="{ active: activeReviewFilter === stat.status }"
                @click="setReviewFilter(stat.status)"
              >
                <div class="stat-icon" :style="{ background: stat.bgColor }">
                  <el-icon :size="20" :color="stat.iconColor">
                    <component :is="stat.icon" />
                  </el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ stat.count }}</div>
                  <div class="stat-label">{{ stat.label }}</div>
                </div>
              </div>
            </div>

            <el-empty v-if="favoriteList.length === 0" description="暂无收藏资料" />
            <div v-else class="favorite-sections">
              <div
                v-for="section in reviewSections"
                :key="section.status"
                class="review-section"
                v-show="section.items.length > 0"
              >
                <div class="section-header">
                  <div class="section-title">
                    <el-icon :size="18" :color="section.color">
                      <component :is="section.icon" />
                    </el-icon>
                    <span>{{ section.label }}</span>
                    <el-tag size="small" type="info">
                      {{ section.items.length }} 份
                    </el-tag>
                  </div>
                </div>

                <div class="material-list">
                  <div
                    v-for="item in section.items"
                    :key="item.id"
                    class="list-item favorite-item"
                    :class="{ 'item-read': item.reviewStatus === 1, 'item-offline': item.status !== 1 }"
                  >
                    <div class="item-checkbox" @click.stop="item.status === 1 && toggleReviewStatus(item)">
                      <el-checkbox :model-value="item.reviewStatus === 1" :disabled="item.status !== 1" />
                    </div>
                    <div class="item-main" @click="item.status === 1 && goToDetail(item.id)">
                      <div class="item-icon">
                        <el-icon :size="24" :color="item.status !== 1 ? '#c0c4cc' : (item.reviewStatus === 1 ? '#909399' : '#E6A23C')">
                          <StarFilled />
                        </el-icon>
                      </div>
                      <div class="item-content">
                        <h3 class="item-title" :class="{ 'title-read': item.reviewStatus === 1, 'title-offline': item.status !== 1 }">
                          {{ item.title }}
                        </h3>
                        <p class="item-desc">
                          <el-tag v-if="item.status !== 1" type="info" size="small" class="offline-tag">已下架</el-tag>
                          {{ item.description || '暂无描述' }}
                        </p>
                        <div class="item-tags" v-if="item.status === 1">
                          <el-tag size="small" type="success">{{ getCategoryName(item.categoryId) }}</el-tag>
                          <el-tag size="small">{{ getGradeName(item.gradeId) }}</el-tag>
                          <el-tag size="small" type="warning">{{ getSubjectName(item.subjectId) }}</el-tag>
                        </div>
                      </div>
                    </div>
                    <div class="item-side">
                      <div class="item-meta">
                        <span class="meta-item">
                          <el-icon><View /></el-icon>
                          {{ item.viewCount || 0 }}
                        </span>
                        <span class="meta-item">
                          <el-icon><Download /></el-icon>
                          {{ item.downloadCount || 0 }}
                        </span>
                      </div>
                      <div class="item-actions">
                        <div class="review-buttons" v-if="item.status === 1">
                          <el-button
                            size="small"
                            :type="item.reviewStatus === 1 ? 'success' : 'default'"
                            :plain="item.reviewStatus !== 1"
                            @click.stop="setItemReviewStatus(item, 1)"
                          >
                            <el-icon><Check /></el-icon>
                            已看过
                          </el-button>
                          <el-button
                            size="small"
                            :type="item.reviewStatus === 2 ? 'warning' : 'default'"
                            :plain="item.reviewStatus !== 2"
                            @click.stop="setItemReviewStatus(item, 2)"
                          >
                            <el-icon><Reading /></el-icon>
                            待精读
                          </el-button>
                          <el-button
                            size="small"
                            :type="item.reviewStatus === 3 ? 'primary' : 'default'"
                            :plain="item.reviewStatus !== 3"
                            @click.stop="setItemReviewStatus(item, 3)"
                          >
                            <el-icon><Printer /></el-icon>
                            待打印
                          </el-button>
                        </div>
                        <div class="review-buttons" v-else>
                          <el-tag type="info" size="small">已下架</el-tag>
                        </div>
                        <div class="action-row">
                          <span class="item-time" v-if="item.reviewedAt && item.status === 1">
                            {{ formatDate(item.reviewedAt) }}
                          </span>
                          <el-button
                            type="warning"
                            size="small"
                            text
                            @click.stop="handleUnfavorite(item.id)"
                          >
                            <el-icon><StarFilled /></el-icon>
                            取消收藏
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="pagination-wrapper" v-if="favoriteList.length > 0">
              <el-pagination
                v-model:current-page="favoritePage"
                v-model:page-size="pageSize"
                :page-sizes="[5, 10, 20]"
                :total="favoriteTotal"
                layout="total, prev, pager, next"
                @current-change="handleFavoritePageChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="showHandleDialog"
      :title="handleType === 1 ? '采纳勘误' : '驳回勘误'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="3"
            :placeholder="handleType === 1 ? '请输入采纳说明（选填）' : '请输入驳回理由（选填）'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button
          :type="handleType === 1 ? 'success' : 'danger'"
          @click="confirmHandleCorrection"
          :loading="handlingCorrection"
        >
          {{ handleType === 1 ? '确认采纳' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Upload, Star, StarFilled, Document, View, Download, Delete,
  ArrowRight, Calendar, Collection, Check, Reading, Printer,
  Warning, Close
} from '@element-plus/icons-vue'
import {
  getMyUploads, getMyFavorites, deleteMaterial,
  getCategoryList, getGradeList, getSubjectList,
  updateReviewStatus,
  getUploaderCorrections, getUploaderCorrectionStats, handleCorrection
} from '@/api/material'
import { useAppStore } from '@/store'

const router = useRouter()
const appStore = useAppStore()

const activeTab = ref('uploads')
const loading = ref(false)
const favLoading = ref(false)
const pageSize = ref(10)

const uploadCount = ref(0)
const favoriteCount = computed(() => appStore.favoriteCount)

const rawUploadList = ref([])
const uploadPage = ref(1)
const uploadTotal = ref(0)
const uploadGroupBy = ref('date')
const expandedUploadGroups = ref({})
const allUploadGroupsExpanded = ref(true)

const rawFavoriteList = ref([])
const favoritePage = ref(1)
const favoriteTotal = computed(() => appStore.favoriteCount)
const activeReviewFilter = ref(-1)

const uploadList = computed(() => rawUploadList.value.map(item => appStore.getEnrichedMaterial(item)))
const favoriteList = computed(() => rawFavoriteList.value.map(item => appStore.getEnrichedMaterial(item)))

const correctionList = ref([])
const correctionPage = ref(1)
const correctionPageSize = ref(10)
const correctionTotal = ref(0)
const correctionLoading = ref(false)
const activeCorrectionFilter = ref(-1)
const showHandleDialog = ref(false)
const handlingCorrection = ref(false)
const handleType = ref(1)
const handleForm = ref({
  correctionId: null,
  handleRemark: ''
})

const categories = ref([])
const grades = ref([])
const subjects = ref([])

const categoryMap = ref({})
const gradeMap = ref({})
const subjectMap = ref({})

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

const loadUploads = async () => {
  loading.value = true
  try {
    const res = await getMyUploads({ page: uploadPage.value, size: pageSize.value })
    rawUploadList.value = res.data?.list || []
    uploadTotal.value = res.data?.total || 0
    uploadCount.value = uploadTotal.value
    
    appStore.batchSetMaterialStats(rawUploadList.value)
    
    initUploadGroups()
    
    if (rawUploadList.value.length === 0 && uploadPage.value > 1) {
      uploadPage.value--
      return loadUploads()
    }
  } catch (e) {
    console.error('加载我的上传失败', e)
    rawUploadList.value = []
    uploadTotal.value = 0
    uploadCount.value = 0
  } finally {
    loading.value = false
  }
}

const loadFavorites = async () => {
  favLoading.value = true
  try {
    const res = await getMyFavorites({ page: favoritePage.value, size: pageSize.value })
    rawFavoriteList.value = res.data?.list || []
    const total = res.data?.total || 0
    appStore.setFavoriteCount(total)
    
    appStore.batchSetMaterialStats(rawFavoriteList.value)

    if (favoritePage.value === 1) {
      await appStore.loadFavorites(true)
    }

    if (rawFavoriteList.value.length === 0 && favoritePage.value > 1) {
      favoritePage.value--
      return loadFavorites()
    }
  } catch (e) {
    console.error('加载我的收藏失败', e)
    rawFavoriteList.value = []
  } finally {
    favLoading.value = false
  }
}

const getCategoryName = (id) => categoryMap.value[id] || '未分类'
const getGradeName = (id) => gradeMap.value[id] || '未指定'
const getSubjectName = (id) => subjectMap.value[id] || '未指定'

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

const formatDateFull = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}年${month}月${day}日`
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

const correctionStats = computed(() => {
  const rawStats = correctionList.value.reduce((acc, item) => {
    const status = item.status || 0
    acc[status] = (acc[status] || 0) + 1
    return acc
  }, {})
  const total = correctionList.value.length
  return [
    { status: -1, label: '全部', icon: 'Document', bgColor: '#f0f2f5', iconColor: '#909399', count: total },
    { status: 0, label: '待处理', icon: 'Warning', bgColor: '#fdf6ec', iconColor: '#E6A23C', count: rawStats[0] || 0 },
    { status: 1, label: '已采纳', icon: 'Check', bgColor: '#f0f9eb', iconColor: '#67C23A', count: rawStats[1] || 0 },
    { status: 2, label: '已驳回', icon: 'Close', bgColor: '#fef0f0', iconColor: '#F56C6C', count: rawStats[2] || 0 }
  ]
})

const loadCorrections = async () => {
  correctionLoading.value = true
  try {
    const status = activeCorrectionFilter.value >= 0 ? activeCorrectionFilter.value : undefined
    const res = await getUploaderCorrections({
      page: correctionPage.value,
      size: correctionPageSize.value,
      status
    })
    correctionList.value = res.data?.list || []
    correctionTotal.value = res.data?.total || 0
  } catch (e) {
    console.error('加载勘误列表失败', e)
    correctionList.value = []
    correctionTotal.value = 0
  } finally {
    correctionLoading.value = false
  }
}

const setCorrectionFilter = (status) => {
  activeCorrectionFilter.value = activeCorrectionFilter.value === status ? -1 : status
  correctionPage.value = 1
  loadCorrections()
}

const handleCorrectionPageChange = (page) => {
  correctionPage.value = page
  loadCorrections()
}

const openHandleDialog = (item, type) => {
  handleType.value = type
  handleForm.value = {
    correctionId: item.id,
    handleRemark: ''
  }
  showHandleDialog.value = true
}

const confirmHandleCorrection = async () => {
  if (!handleForm.value.correctionId) return

  handlingCorrection.value = true
  try {
    await handleCorrection(handleForm.value.correctionId, handleType.value, handleForm.value.handleRemark)
    ElMessage.success(handleType.value === 1 ? '已采纳' : '已驳回')
    showHandleDialog.value = false
    loadCorrections()
  } catch (e) {
    console.error('处理勘误失败', e)
    ElMessage.error(e.response?.data?.message || '操作失败，请重试')
  } finally {
    handlingCorrection.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'uploads') {
    loadUploads()
  } else if (tab === 'favorites') {
    loadFavorites()
  } else if (tab === 'corrections') {
    correctionPage.value = 1
    activeCorrectionFilter.value = -1
    loadCorrections()
  }
}

const handleUploadPageChange = (page) => {
  uploadPage.value = page
  loadUploads()
}

const handleFavoritePageChange = (page) => {
  favoritePage.value = page
  loadFavorites()
}

const goToDetail = (id) => {
  router.push(`/detail/${id}`)
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要下架这份资料吗？下架后其他人将无法看到。',
      '下架确认',
      {
        confirmButtonText: '确定下架',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteMaterial(id)
    ElMessage.success('下架成功')
    loadUploads()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败，请重试')
    }
  }
}

const handleUnfavorite = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消收藏这份资料吗？',
      '取消收藏',
      {
        confirmButtonText: '确定',
        cancelButtonText: '再想想',
        type: 'warning'
      }
    )
    
    await appStore.doUnfavorite(id)
    rawFavoriteList.value = rawFavoriteList.value.filter(item => item.id !== id)
    ElMessage.success('已取消收藏')
    
    if (rawFavoriteList.value.length === 0 && favoritePage.value > 1) {
      favoritePage.value--
      loadFavorites()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败，请重试')
    }
  }
}

const initUploadGroups = () => {
  expandedUploadGroups.value = {}
  uploadGroups.value.forEach(group => {
    expandedUploadGroups.value[group.key] = true
  })
  allUploadGroupsExpanded.value = true
}

const uploadGroups = computed(() => {
  const groups = []
  const list = uploadList.value
  
  if (uploadGroupBy.value === 'date') {
    const dateMap = {}
    list.forEach(item => {
      const dateKey = formatDate(item.createdAt)
      if (!dateMap[dateKey]) {
        dateMap[dateKey] = []
      }
      dateMap[dateKey].push(item)
    })
    
    const sortedDates = Object.keys(dateMap).sort((a, b) => new Date(b) - new Date(a))
    sortedDates.forEach(date => {
      const items = dateMap[date]
      const today = formatDate(new Date().toISOString())
      const yesterday = formatDate(new Date(Date.now() - 86400000).toISOString())
      
      let title = formatDateFull(date)
      let meta = ''
      
      if (date === today) {
        title = '今天'
        meta = formatDateFull(date)
      } else if (date === yesterday) {
        title = '昨天'
        meta = formatDateFull(date)
      } else {
        meta = `${items.length} 份资料`
      }
      
      groups.push({
        key: 'date_' + date,
        title,
        meta,
        icon: 'Calendar',
        iconColor: '#409EFF',
        items
      })
    })
  } else {
    const subjectMapGroups = {}
    list.forEach(item => {
      const subjectId = item.subjectId || 0
      if (!subjectMapGroups[subjectId]) {
        subjectMapGroups[subjectId] = []
      }
      subjectMapGroups[subjectId].push(item)
    })
    
    Object.keys(subjectMapGroups).forEach(subjectId => {
      const items = subjectMapGroups[subjectId]
      const subjectName = getSubjectName(Number(subjectId))
      
      groups.push({
        key: 'subject_' + subjectId,
        title: subjectName,
        meta: `${items.length} 份资料`,
        icon: 'Collection',
        iconColor: '#E6A23C',
        items
      })
    })
    
    groups.sort((a, b) => a.items.length - b.items.length).reverse()
  }
  
  return groups
})

const toggleUploadGroup = (key) => {
  expandedUploadGroups.value[key] = !expandedUploadGroups.value[key]
  const allExpanded = Object.values(expandedUploadGroups.value).every(v => v)
  allUploadGroupsExpanded.value = allExpanded
}

const toggleAllUploadGroups = () => {
  const newValue = !allUploadGroupsExpanded.value
  uploadGroups.value.forEach(group => {
    expandedUploadGroups.value[group.key] = newValue
  })
  allUploadGroupsExpanded.value = newValue
}

const handleGroupByChange = () => {
  initUploadGroups()
}

const reviewSections = computed(() => {
  const sections = [
    {
      status: 2,
      label: '待精读',
      icon: 'Reading',
      color: '#E6A23C',
      items: []
    },
    {
      status: 3,
      label: '待打印',
      icon: 'Printer',
      color: '#409EFF',
      items: []
    },
    {
      status: 0,
      label: '未标记',
      icon: 'StarFilled',
      color: '#909399',
      items: []
    },
    {
      status: 1,
      label: '已看过',
      icon: 'Check',
      color: '#67C23A',
      items: []
    }
  ]
  
  let list = favoriteList.value
  if (activeReviewFilter.value >= 0) {
    list = list.filter(item => (item.reviewStatus || 0) === activeReviewFilter.value)
  }
  
  list.forEach(item => {
    const status = item.reviewStatus || 0
    const section = sections.find(s => s.status === status)
    if (section) {
      section.items.push(item)
    }
  })
  
  return sections
})

const reviewStats = computed(() => {
  const stats = [
    { status: -1, label: '全部', icon: 'StarFilled', bgColor: '#f0f2f5', iconColor: '#909399', count: favoriteList.value.length },
    { status: 2, label: '待精读', icon: 'Reading', bgColor: '#fdf6ec', iconColor: '#E6A23C', count: 0 },
    { status: 3, label: '待打印', icon: 'Printer', bgColor: '#ecf5ff', iconColor: '#409EFF', count: 0 },
    { status: 1, label: '已看过', icon: 'Check', bgColor: '#f0f9eb', iconColor: '#67C23A', count: 0 },
    { status: 0, label: '未标记', icon: 'Star', bgColor: '#f5f7fa', iconColor: '#c0c4cc', count: 0 }
  ]
  
  favoriteList.value.forEach(item => {
    const status = item.reviewStatus || 0
    const stat = stats.find(s => s.status === status)
    if (stat) {
      stat.count++
    }
  })
  
  return stats
})

const setReviewFilter = (status) => {
  activeReviewFilter.value = activeReviewFilter.value === status ? -1 : status
}

const toggleReviewStatus = async (item) => {
  const currentStatus = item.reviewStatus || 0
  const newStatus = currentStatus === 1 ? 0 : 1
  await setItemReviewStatus(item, newStatus)
}

const setItemReviewStatus = async (item, status) => {
  try {
    const oldStatus = item.reviewStatus || 0
    item.reviewStatus = status
    
    await updateReviewStatus(item.id, status)
    ElMessage.success(status === 0 ? '已取消标记' : '标记成功')
  } catch (e) {
    console.error('更新复习状态失败', e)
    ElMessage.error('更新失败，请重试')
  }
}

onMounted(() => {
  loadDicts()
  loadUploads()
})
</script>

<style scoped>
.profile-page {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
}

.user-card {
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 0;
}

.user-details {
  flex: 1;
}

.username {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.user-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 15px;
}

.tabs-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.list-container {
  padding: 8px 0;
}

.upload-toolbar,
.favorite-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.toolbar-subtitle {
  font-size: 13px;
  color: #909399;
}

.upload-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-group {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.3s;
}

.upload-group:hover {
  border-color: #d9ecff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.08);
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.group-header:hover {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
}

.group-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.collapse-icon {
  transition: transform 0.3s ease;
  color: #909399;
  font-size: 14px;
}

.collapse-icon.expanded {
  transform: rotate(90deg);
}

.group-icon {
  flex-shrink: 0;
}

.group-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.group-count {
  margin-left: 4px;
}

.group-meta {
  font-size: 13px;
  color: #909399;
}

.group-content {
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.group-content.collapsed {
  max-height: 0;
  overflow: hidden;
}

.group-content:not(.collapsed) {
  max-height: 3000px;
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  padding: 18px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f2f5;
  transition: all 0.2s;
}

.list-item:last-child {
  border-bottom: none;
}

.list-item:hover {
  background: #fafcff;
}

.item-main {
  display: flex;
  gap: 16px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
}

.item-checkbox {
  display: flex;
  align-items: flex-start;
  padding-top: 4px;
  padding-right: 8px;
  cursor: pointer;
}

.item-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  background: #ecf5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.title-read {
  color: #909399;
  text-decoration: line-through;
}

.item-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.item-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  margin-left: 20px;
  flex-shrink: 0;
  gap: 12px;
}

.item-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.item-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.review-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-time {
  font-size: 12px;
  color: #c0c4cc;
}

.item-read {
  opacity: 0.7;
}

.item-offline {
  opacity: 0.6;
  background: #fafafa;
}

.title-offline {
  color: #c0c4cc;
  text-decoration: line-through;
}

.offline-tag {
  margin-right: 6px;
}

.review-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.review-stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 2px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.review-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.review-stat-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-number {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #606266;
}

.favorite-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.review-section {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.section-header {
  margin-bottom: 12px;
  padding-left: 4px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.favorite-item {
  padding-left: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 24px;
}

.correction-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.correction-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.correction-stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 2px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.correction-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.correction-stat-card.active {
  border-color: #F56C6C;
  background: #fef0f0;
}

.correction-sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.correction-card-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.3s;
}

.correction-card-item:hover {
  border-color: #fbc4c4;
  box-shadow: 0 2px 12px rgba(245, 108, 108, 0.08);
}

.correction-card-item .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: linear-gradient(135deg, #fef0f0 0%, #fff5f5 100%);
  border-bottom: 1px solid #fde2e2;
}

.correction-card-item .card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: color 0.2s;
}

.correction-card-item .card-title:hover {
  color: #409eff;
}

.correction-card-item .material-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.correction-card-item .card-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.correction-card-item .card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.correction-card-item .card-meta .submitter {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.correction-card-item .card-meta .submit-time {
  font-size: 12px;
  color: #c0c4cc;
}

.correction-card-item .card-desc,
.correction-card-item .card-suggestion,
.correction-card-item .card-handle {
  display: flex;
  gap: 6px;
  line-height: 1.6;
}

.correction-card-item .card-desc .label,
.correction-card-item .card-suggestion .label,
.correction-card-item .card-handle .label {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.correction-card-item .card-desc .content,
.correction-card-item .card-suggestion .content {
  font-size: 14px;
  color: #303133;
  flex: 1;
}

.correction-card-item .card-handle {
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  flex-wrap: wrap;
}

.correction-card-item .card-handle .content {
  font-size: 13px;
  color: #67c23a;
  flex: 1;
}

.correction-card-item .card-handle .handle-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: auto;
}

.correction-card-item .card-footer {
  padding: 12px 20px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .list-item {
    flex-direction: column;
  }
  
  .item-side {
    flex-direction: row;
    align-items: center;
    margin-left: 0;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }
  
  .item-actions {
    flex-direction: row;
    align-items: center;
  }
  
  .user-info {
    flex-direction: column;
    text-align: center;
  }
  
  .user-stats {
    justify-content: center;
  }
  
  .review-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .correction-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .review-buttons {
    justify-content: flex-start;
  }
  
  .upload-toolbar,
  .favorite-toolbar,
  .correction-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .correction-card-item .card-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .correction-card-item .card-footer {
    justify-content: stretch;
  }

  .correction-card-item .card-footer .el-button {
    flex: 1;
  }
}
</style>
