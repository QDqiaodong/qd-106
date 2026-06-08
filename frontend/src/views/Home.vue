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
        <div class="material-list">
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
              <span class="upload-time">{{ formatDate(item.createdAt) }}</span>
            </div>
          </el-card>
        </div>

        <div v-if="materialList.length === 0" class="empty-tip">
          <el-empty description="暂无资料" />
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[8, 16, 24, 32]"
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
          <div class="hot-list">
            <div
              v-for="(item, index) in hotList"
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
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Document, View, Download, Histogram } from '@element-plus/icons-vue'
import { getMaterialList, getCategoryList, getGradeList, getSubjectList, getHotMaterials } from '@/api/material'

const router = useRouter()

const searchKeyword = ref('')
const selectedCategory = ref(null)
const selectedGrade = ref(null)
const selectedSubject = ref(null)
const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)
const materialList = ref([])
const hotList = ref([])
const categories = ref([])
const grades = ref([])
const subjects = ref([])

const categoryMap = ref({})
const gradeMap = ref({})
const subjectMap = ref({})

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
    materialList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载资料列表失败', e)
  }
}

const loadHotMaterials = async () => {
  try {
    const res = await getHotMaterials()
    hotList.value = (res.data || []).slice(0, 5)
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

onMounted(() => {
  loadCategories()
  loadGrades()
  loadSubjects()
  loadMaterials()
  loadHotMaterials()
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
}

.material-list {
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

.upload-time {
  color: #c0c4cc;
  font-size: 13px;
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
  
  .material-list {
    grid-template-columns: 1fr;
  }
}
</style>
