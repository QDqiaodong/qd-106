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
            <el-empty v-if="uploadList.length === 0" description="暂无上传资料" />
            <div v-else class="material-list">
              <div
                v-for="item in uploadList"
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

        <el-tab-pane label="我的收藏" name="favorites">
          <div class="list-container" v-loading="favLoading">
            <el-empty v-if="favoriteList.length === 0" description="暂无收藏资料" />
            <div v-else class="material-list">
              <div
                v-for="item in favoriteList"
                :key="item.id"
                class="list-item"
              >
                <div class="item-main" @click="goToDetail(item.id)">
                  <div class="item-icon">
                    <el-icon :size="24" color="#E6A23C"><StarFilled /></el-icon>
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
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Star, StarFilled, Document, View, Download, Delete } from '@element-plus/icons-vue'
import { getMyUploads, getMyFavorites, deleteMaterial, getCategoryList, getGradeList, getSubjectList } from '@/api/material'
import { useAppStore } from '@/store'

const router = useRouter()
const appStore = useAppStore()

const activeTab = ref('uploads')
const loading = ref(false)
const favLoading = ref(false)
const pageSize = ref(10)

const uploadCount = ref(0)
const favoriteCount = computed(() => appStore.favoriteCount)

const uploadList = ref([])
const uploadPage = ref(1)
const uploadTotal = ref(0)

const favoriteList = ref([])
const favoritePage = ref(1)
const favoriteTotal = computed(() => appStore.favoriteCount)

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
    uploadList.value = res.data?.list || []
    uploadTotal.value = res.data?.total || 0
    uploadCount.value = uploadTotal.value
  } catch (e) {
    console.error('加载我的上传失败', e)
    uploadList.value = []
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
    favoriteList.value = res.data?.list || []
    const total = res.data?.total || 0
    appStore.setFavoriteCount(total)
    
    if (favoritePage.value === 1) {
      const map = {}
      favoriteList.value.forEach(item => {
        map[Number(item.id)] = true
      })
      appStore.favoriteMap = map
      appStore.favoritesLoaded = true
    }
  } catch (e) {
    console.error('加载我的收藏失败', e)
    favoriteList.value = []
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

const handleTabChange = (tab) => {
  if (tab === 'uploads') {
    loadUploads()
  } else {
    loadFavorites()
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
    favoriteList.value = favoriteList.value.filter(item => item.id !== id)
    ElMessage.success('已取消收藏')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败，请重试')
    }
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
  max-width: 1000px;
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

.material-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  transition: all 0.3s;
  border: 1px solid #ebeef5;
}

.list-item:hover {
  background: #f0f9ff;
  border-color: #d9ecff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.item-main {
  display: flex;
  gap: 16px;
  cursor: pointer;
  flex: 1;
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

.item-time {
  font-size: 12px;
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 24px;
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
}
</style>
