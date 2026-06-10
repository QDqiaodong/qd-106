<template>
  <div class="home">
    <div class="search-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索学习资料..."
        size="large"
        class="search-input"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div class="filter-section">
      <div class="filter-group">
        <span class="filter-label">分类：</span>
        <el-tag
          :type="selectedCategory === null ? 'primary' : 'info'"
          class="filter-tag"
          @click="selectCategory(null)"
        >
          全部
        </el-tag>
        <el-tag
          v-for="cat in categories"
          :key="cat.id"
          :type="selectedCategory === cat.id ? 'primary' : 'info'"
          class="filter-tag"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </el-tag>
      </div>
      <div class="filter-group">
        <span class="filter-label">年级：</span>
        <el-tag
          :type="selectedGrade === null ? 'primary' : 'info'"
          class="filter-tag"
          @click="selectGrade(null)"
        >
          全部
        </el-tag>
        <el-tag
          v-for="grade in grades"
          :key="grade.id"
          :type="selectedGrade === grade.id ? 'primary' : 'info'"
          class="filter-tag"
          @click="selectGrade(grade.id)"
        >
          {{ grade.name }}
        </el-tag>
      </div>
      <div class="filter-group">
        <span class="filter-label">学科：</span>
        <el-tag
          :type="selectedSubject === null ? 'primary' : 'info'"
          class="filter-tag"
          @click="selectSubject(null)"
        >
          全部
        </el-tag>
        <el-tag
          v-for="subject in subjects"
          :key="subject.id"
          :type="selectedSubject === subject.id ? 'primary' : 'info'"
          class="filter-tag"
          @click="selectSubject(subject.id)"
        >
          {{ subject.name }}
        </el-tag>
      </div>
    </div>

    <div class="content-wrapper">
      <div class="main-content">
        <div class="list-header">
          <span class="list-count">共 {{ total }} 份资料</span>
          <div class="view-toggle">
            <el-tooltip content="卡片网格">
              <el-button
                :type="appStore.viewMode === 'grid' ? 'primary' : 'default'"
                :icon="Grid"
                circle
                size="small"
                @click="setViewMode('grid')"
              />
            </el-tooltip>
            <el-tooltip content="列表紧凑">
              <el-button
                :type="appStore.viewMode === 'list' ? 'primary' : 'default'"
                :icon="List"
                circle
                size="small"
                @click="setViewMode('list')"
              />
            </el-tooltip>
          </div>
        </div>

        <transition name="fade-move" mode="out-in">
          <div v-if="appStore.viewMode === 'grid'" key="grid" class="material-grid">
            <el-card
              v-for="item in materialList"
              :key="item.id"
              class="material-card"
              shadow="hover"
              @click="goToDetail(item.id)"
            >
              <div class="card-header">
                <el-icon :size="20" color="#409EFF"><Document /></el-icon>
                <span class="card-title">{{ item.title }}</span>
              </div>
              <div class="card-description">{{ item.description || '暂无描述' }}</div>
              <div class="card-tags">
                <el-tag size="small" type="success">{{ getCategoryName(item.categoryId) }}</el-tag>
                <el-tag size="small">{{ getGradeName(item.gradeId) }}</el-tag>
                <el-tag size="small" type="warning">{{ getSubjectName(item.subjectId) }}</el-tag>
              </div>
              <div class="card-footer">
                <div class="card-meta">
                  <span class="meta-item">
                    <el-icon><View /></el-icon>
                    {{ item.viewCount || 0 }}
                  </span>
                  <span class="meta-item">
                    <el-icon><Download /></el-icon>
                    {{ item.downloadCount || 0 }}
                  </span>
                </div>
                <div class="card-actions">
                  <span class="upload-time">{{ formatDate(item.createdAt) }}</span>
                  <el-tooltip :content="appStore.isInBasket(item.id) ? '已在资料篮中' : '加入复习资料篮'">
                    <el-button
                      :type="appStore.isInBasket(item.id) ? 'success' : 'default'"
                      size="small"
                      text
                      @click="(e) => handleToggleBasket(e, item)"
                    >
                      <el-icon>
                        <ShoppingCartFull v-if="appStore.isInBasket(item.id)" />
                        <ShoppingCart v-else />
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-button
                    :type="appStore.isFavorite(item.id) ? 'warning' : 'default'"
                    size="small"
                    text
                    @click="(e) => handleFavorite(e, item.id)"
                  >
                    <el-icon>
                      <StarFilled v-if="appStore.isFavorite(item.id)" />
                      <Star v-else />
                    </el-icon>
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>

          <div v-else key="list" class="material-list-view">
            <div
              v-for="item in materialList"
              :key="item.id"
              class="list-item"
              @click="goToDetail(item.id)"
            >
              <div class="list-thumb">
                <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                <el-icon v-else :size="32" color="#409EFF"><Document /></el-icon>
              </div>
              <div class="list-content">
                <div class="list-title">{{ item.title }}</div>
                <div class="list-description">{{ item.description || '暂无描述' }}</div>
                <div class="list-tags">
                  <el-tag size="small" type="success">{{ getCategoryName(item.categoryId) }}</el-tag>
                  <el-tag size="small">{{ getGradeName(item.gradeId) }}</el-tag>
                  <el-tag size="small" type="warning">{{ getSubjectName(item.subjectId) }}</el-tag>
                </div>
              </div>
              <div class="list-stats">
                <div class="stat-item">
                  <el-icon><View /></el-icon>
                  <span class="stat-num">{{ item.viewCount || 0 }}</span>
                  <span class="stat-label">浏览</span>
                </div>
                <div
                  class="stat-item basket-stat"
                  :class="{ 'is-in-basket': appStore.isInBasket(item.id) }"
                  @click="(e) => handleToggleBasket(e, item)"
                >
                  <el-tooltip :content="appStore.isInBasket(item.id) ? '已在资料篮中' : '加入复习资料篮'" placement="top">
                    <el-icon>
                      <ShoppingCartFull v-if="appStore.isInBasket(item.id)" />
                      <ShoppingCart v-else />
                    </el-icon>
                  </el-tooltip>
                  <span class="stat-num">-</span>
                  <span class="stat-label">资料篮</span>
                </div>
                <div
                  class="stat-item favorite-stat"
                  :class="{ 'is-favorited': appStore.isFavorite(item.id) }"
                  @click="(e) => handleFavorite(e, item.id)"
                >
                  <el-icon>
                    <StarFilled v-if="appStore.isFavorite(item.id)" />
                    <Star v-else />
                  </el-icon>
                  <span class="stat-num">{{ item.favoriteCount || 0 }}</span>
                  <span class="stat-label">收藏</span>
                </div>
                <div class="stat-item">
                  <el-icon><Download /></el-icon>
                  <span class="stat-num">{{ item.downloadCount || 0 }}</span>
                  <span class="stat-label">下载</span>
                </div>
                <div class="stat-time">
                  <span>{{ formatDate(item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </transition>

        <div v-if="materialList.length === 0" class="empty-tip">
          <el-empty description="暂无资料" />
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="appStore.viewMode === 'grid' ? [8, 16, 24, 32] : [10, 20, 30, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <div class="sidebar">
        <el-card class="hot-card">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#E6A23C"><Histogram /></el-icon>
              <span>热门资料</span>
            </div>
          </template>
          <el-tabs v-model="hotTab" class="hot-tabs" stretch>
            <el-tab-pane label="近7天" name="7d" />
            <el-tab-pane label="近30天" name="30d" />
            <el-tab-pane label="本学期" name="semester" />
          </el-tabs>
          <div class="hot-list">
            <div
              v-for="(item, index) in currentHotList"
              :key="item.id"
              class="hot-item"
              @click="goToDetail(item.id)"
            >
              <span class="hot-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
              <div class="hot-info">
                <div class="hot-title">{{ item.title }}</div>
                <div class="hot-meta">
                  <el-icon><View /></el-icon>
                  {{ item.viewCount || 0 }} 浏览
                </div>
              </div>
            </div>
            <div v-if="currentHotList.length === 0" class="hot-empty">
              <el-empty description="暂无数据" :image-size="60" />
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Document, View, Download, Histogram, Grid, List, Star, StarFilled, ShoppingCart, ShoppingCartFull } from '@element-plus/icons-vue'
import { getMaterialList, getCategoryList, getGradeList, getSubjectList, getHotMaterials } from '@/api/material'
import { useAppStore } from '@/store'

const router = useRouter()
const appStore = useAppStore()

const GRID_DEFAULT_SIZE = 8
const LIST_DEFAULT_SIZE = 10

const searchKeyword = ref('')
const selectedCategory = ref(null)
const selectedGrade = ref(null)
const selectedSubject = ref(null)
const currentPage = ref(1)
const pageSize = ref(appStore.viewMode === 'grid' ? GRID_DEFAULT_SIZE : LIST_DEFAULT_SIZE)
const total = ref(0)
const materialList = ref([])
const hotTab = ref('7d')
const hotList7d = ref([])
const hotList30d = ref([])
const hotListSemester = ref([])
const categories = ref([])
const grades = ref([])
const subjects = ref([])

const currentHotList = computed(() => {
  switch (hotTab.value) {
    case '7d': return hotList7d.value
    case '30d': return hotList30d.value
    case 'semester': return hotListSemester.value
    default: return []
  }
})

let requestSeq = 0

const categoryMap = ref({})
const gradeMap = ref({})
const subjectMap = ref({})

const setViewMode = (mode) => {
  appStore.setViewMode(mode)
  pageSize.value = mode === 'grid' ? GRID_DEFAULT_SIZE : LIST_DEFAULT_SIZE
  currentPage.value = 1
  loadMaterials()
}

const handleResize = () => {
  if (window.innerWidth <= 768 && appStore.viewMode === 'grid' && !localStorage.getItem('material_view_mode')) {
    setViewMode('list')
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
    categories.value.forEach(c => { categoryMap.value[c.id] = c.name })
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const loadGrades = async () => {
  try {
    const res = await getGradeList()
    grades.value = res.data || []
    grades.value.forEach(g => { gradeMap.value[g.id] = g.name })
  } catch (e) {
    console.error('加载年级失败', e)
  }
}

const loadSubjects = async () => {
  try {
    const res = await getSubjectList()
    subjects.value = res.data || []
    subjects.value.forEach(s => { subjectMap.value[s.id] = s.name })
  } catch (e) {
    console.error('加载学科失败', e)
  }
}

const loadMaterials = async () => {
  const seq = ++requestSeq
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      categoryId: selectedCategory.value || undefined,
      gradeId: selectedGrade.value || undefined,
      subjectId: selectedSubject.value || undefined
    }
    const res = await getMaterialList(params)
    if (seq !== requestSeq) {
      return
    }
    materialList.value = res.data?.list || []
    total.value = res.data?.total || 0
    
    materialList.value.forEach(item => {
      if (item.favorited !== undefined) {
        appStore.setFavorite(item.id, item.favorited)
      }
    })
  } catch (e) {
    if (seq !== requestSeq) {
      return
    }
    console.error('加载资料列表失败', e)
  }
}

const loadHotMaterials = async () => {
  try {
    const res = await getHotMaterials()
    const data = res.data || {}
    hotList7d.value = (data.hot7d || []).slice(0, 5)
    hotList30d.value = (data.hot30d || []).slice(0, 5)
    hotListSemester.value = (data.hotSemester || []).slice(0, 5)
  } catch (e) {
    console.error('加载热门资料失败', e)
  }
}

const getCategoryName = (id) => categoryMap.value[id] || '未分类'
const getGradeName = (id) => gradeMap.value[id] || '未指定'
const getSubjectName = (id) => subjectMap.value[id] || '未指定'

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

const handleSearch = () => {
  currentPage.value = 1
  loadMaterials()
}

const selectCategory = (id) => {
  selectedCategory.value = id
  currentPage.value = 1
  loadMaterials()
}

const selectGrade = (id) => {
  selectedGrade.value = id
  currentPage.value = 1
  loadMaterials()
}

const selectSubject = (id) => {
  selectedSubject.value = id
  currentPage.value = 1
  loadMaterials()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadMaterials()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadMaterials()
}

const goToDetail = (id) => {
  router.push(`/detail/${id}`)
}

const handleFavorite = async (e, id) => {
  e.stopPropagation()
  try {
    await appStore.toggleFavorite(id)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败，请重试')
  }
}

const handleToggleBasket = (e, item) => {
  e.stopPropagation()
  const material = {
    ...item,
    categoryName: getCategoryName(item.categoryId),
    gradeName: getGradeName(item.gradeId),
    subjectName: getSubjectName(item.subjectId)
  }
  if (appStore.isInBasket(item.id)) {
    appStore.removeFromBasket(item.id)
    ElMessage.success('已从资料篮移除')
  } else {
    const added = appStore.addToBasket(material)
    if (added) {
      ElMessage.success('已加入本轮复习资料篮')
    }
  }
}

onMounted(() => {
  loadCategories()
  loadGrades()
  loadSubjects()
  loadMaterials()
  loadHotMaterials()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.home {
  width: 100%;
}

.search-section {
  margin-bottom: 24px;
}

.search-input {
  max-width: 600px;
  margin: 0 auto;
  display: block;
}

.filter-section {
  background: #fff;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.filter-group {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-group:last-child {
  margin-bottom: 0;
}

.filter-label {
  font-weight: 500;
  color: #303133;
  margin-right: 12px;
  min-width: 50px;
  flex-shrink: 0;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.filter-tag:hover {
  transform: translateY(-1px);
}

.content-wrapper {
  display: flex;
  gap: 24px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-count {
  font-size: 14px;
  color: #606266;
}

.view-toggle {
  display: flex;
  gap: 8px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.material-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
}

.material-card:hover {
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.card-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.card-meta {
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

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-time {
  color: #c0c4cc;
  font-size: 13px;
}

.material-list-view {
  margin-bottom: 24px;
}

.list-item {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid transparent;
}

.list-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #e4e7ed;
}

.list-thumb {
  width: 120px;
  height: 90px;
  flex-shrink: 0;
  border-radius: 6px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.list-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.list-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.list-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.list-stats {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  align-items: center;
  padding-left: 16px;
  border-left: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-item .el-icon {
  color: #909399;
  font-size: 16px;
}

.stat-item.favorite-stat {
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px 8px;
  border-radius: 6px;
}

.stat-item.favorite-stat:hover {
  background: #fdf6ec;
}

.stat-item.favorite-stat.is-favorited .el-icon {
  color: #e6a23c;
}

.stat-item.favorite-stat.is-favorited .stat-num {
  color: #e6a23c;
}

.stat-item.basket-stat {
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px 8px;
  border-radius: 6px;
}

.stat-item.basket-stat:hover {
  background: #f0f9ff;
}

.stat-item.basket-stat .el-icon {
  color: #909399;
}

.stat-item.basket-stat.is-in-basket .el-icon {
  color: #67c23a;
}

.stat-item.basket-stat.is-in-basket .stat-label {
  color: #67c23a;
}

.stat-num {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-time {
  font-size: 12px;
  color: #c0c4cc;
}

.fade-move-enter-active,
.fade-move-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-move-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-move-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.empty-tip {
  padding: 40px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.sidebar {
  width: 300px;
  flex-shrink: 0;
}

.hot-card {
  border-radius: 8px;
}

.hot-tabs {
  margin-bottom: 8px;
}

.hot-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.hot-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  padding: 0 10px;
  height: 36px;
  line-height: 36px;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}

.hot-list {
  max-height: 500px;
  overflow-y: auto;
}

.hot-empty {
  padding: 20px 0;
}

.hot-empty :deep(.el-empty__description) {
  font-size: 12px;
}

.hot-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.hot-item:last-child {
  border-bottom: none;
}

.hot-item:hover {
  background: #f5f7fa;
}

.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.rank-1 {
  background: #f56c6c;
  color: #fff;
}

.rank-2 {
  background: #e6a23c;
  color: #fff;
}

.rank-3 {
  background: #67c23a;
  color: #fff;
}

.hot-info {
  flex: 1;
  min-width: 0;
}

.hot-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 960px) {
  .content-wrapper {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
  }
  
  .material-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .list-item {
    flex-wrap: wrap;
  }
  
  .list-thumb {
    width: 80px;
    height: 60px;
  }
  
  .list-stats {
    width: 100%;
    flex-direction: row;
    padding-left: 0;
    padding-top: 12px;
    border-left: none;
    border-top: 1px solid #f0f0f0;
  }
  
  .stat-item {
    flex-direction: row;
    gap: 4px;
  }
  
  .stat-label {
    display: none;
  }
  
  .stat-time {
    display: none;
  }
}
</style>
