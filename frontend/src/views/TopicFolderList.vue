<template>
  <div class="topic-folders-page">
    <div class="page-header">
      <h1 class="page-title">专题资料夹</h1>
      <p class="page-subtitle">精选学习资料合集，按主题系统整理，高效学习更轻松</p>
    </div>

    <div class="folder-grid" v-loading="loading">
      <div
        v-for="folder in folderList"
        :key="folder.id"
        class="folder-card"
        @click="goToDetail(folder.id)"
      >
        <div class="folder-cover">
          <div class="cover-gradient" :style="getGradientStyle(folder.id)"></div>
          <div class="folder-icon">
            <el-icon :size="48"><FolderOpened /></el-icon>
          </div>
          <div class="folder-meta-top">
            <el-tag size="small" type="primary" effect="dark">
              {{ folder.materialCount }} 份资料
            </el-tag>
          </div>
        </div>
        <div class="folder-body">
          <h3 class="folder-title">{{ folder.name }}</h3>
          <p class="folder-desc">{{ folder.description || '暂无描述' }}</p>
          <div class="folder-footer">
            <div class="footer-left">
              <el-icon><User /></el-icon>
              <span>{{ folder.creatorName || '管理员' }}</span>
            </div>
            <div class="footer-right">
              <el-icon><View /></el-icon>
              <span>{{ folder.viewCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="folderList.length === 0 && !loading" class="empty-wrapper">
        <el-empty description="暂无专题资料夹" />
      </div>
    </div>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[8, 12, 16, 24]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { FolderOpened, User, View } from '@element-plus/icons-vue'
import { getTopicFolderList } from '@/api/material'

const router = useRouter()

const loading = ref(false)
const folderList = ref([])
const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)

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

const loadFolders = async () => {
  loading.value = true
  try {
    const res = await getTopicFolderList({
      page: currentPage.value,
      size: pageSize.value
    })
    folderList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载专题资料夹失败', e)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/topic-folders/${id}`)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadFolders()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadFolders()
}

onMounted(() => {
  loadFolders()
})
</script>

<style scoped>
.topic-folders-page {
  width: 100%;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
  padding: 32px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 15px;
  opacity: 0.9;
  margin: 0;
}

.folder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.folder-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.folder-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
}

.folder-cover {
  position: relative;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.cover-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.folder-icon {
  position: relative;
  z-index: 1;
  color: #fff;
  opacity: 0.95;
}

.folder-meta-top {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 1;
}

.folder-body {
  padding: 16px 18px 14px;
}

.folder-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 14px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.folder-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #909399;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-wrapper {
  grid-column: 1 / -1;
  padding: 60px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

@media (max-width: 768px) {
  .page-header {
    padding: 24px 16px;
  }

  .page-title {
    font-size: 22px;
  }

  .folder-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 560px) {
  .folder-grid {
    grid-template-columns: 1fr;
  }
}
</style>
