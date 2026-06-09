<template>
  <div class="upload-page">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="22" color="#409EFF"><Upload /></el-icon>
          <span class="header-title">上传学习资料</span>
        </div>
      </template>

      <div v-if="!showProgress" class="upload-form-section">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="upload-form"
        >
          <el-form-item label="资料标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入资料标题"
              maxlength="100"
              show-word-limit
              size="large"
            />
          </el-form-item>

          <el-form-item label="资料描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="请输入资料描述，介绍资料的主要内容和用途"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="分类" prop="categoryId">
            <el-select
              v-model="form.categoryId"
              placeholder="请选择资料分类"
              size="large"
              style="width: 100%"
            >
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="年级" prop="gradeId">
            <el-select
              v-model="form.gradeId"
              placeholder="请选择适用年级"
              size="large"
              style="width: 100%"
            >
              <el-option
                v-for="item in grades"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="学科" prop="subjectId">
            <el-select
              v-model="form.subjectId"
              placeholder="请选择所属学科"
              size="large"
              style="width: 100%"
            >
              <el-option
                v-for="item in subjects"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="上传文件" prop="file">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :limit="1"
              :file-list="fileList"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt"
              drag
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 PDF、Word、PPT、Excel、TXT 格式，大文件将自动分片上传
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              <el-icon style="margin-right: 6px"><Check /></el-icon>
              提交上传
            </el-button>
            <el-button size="large" @click="handleReset">
              <el-icon style="margin-right: 6px"><RefreshLeft /></el-icon>
              重置表单
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-else class="upload-progress-section">
        <ChunkUploadProgress
          :file-name="uploadState.fileName"
          :file-size="uploadState.fileSize"
          :uploaded-size="uploadState.uploadedSize"
          :progress="uploadState.progress"
          :speed="uploadState.speed"
          :remaining-time="uploadState.remainingTime"
          :status="uploadState.status"
          :total-chunks="uploadState.totalChunks"
          :completed-count="uploadState.completedCount"
          :chunks="uploadState.chunks"
          :failed-chunks="uploadState.failedChunks"
          :error="uploadState.error"
          @pause="handlePause"
          @resume="handleResume"
          @cancel="handleCancel"
          @retry-all="handleRetryAll"
          @retry-chunk="handleRetryChunk"
        />

        <div class="completion-actions" v-if="uploadState.status === 'completed'">
          <el-button type="primary" size="large" @click="goToDetail">
            <el-icon><View /></el-icon>
            查看资料详情
          </el-button>
          <el-button size="large" @click="handleResetAll">
            <el-icon><Plus /></el-icon>
            继续上传
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Upload,
  UploadFilled,
  Check,
  RefreshLeft,
  View,
  Plus
} from '@element-plus/icons-vue'
import { getCategoryList, getGradeList, getSubjectList, DEFAULT_USER_ID } from '@/api/material'
import { useChunkUpload } from '@/utils/useChunkUpload'
import ChunkUploadProgress from '@/components/ChunkUploadProgress.vue'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const submitting = ref(false)
const fileList = ref([])

const form = reactive({
  title: '',
  description: '',
  categoryId: null,
  gradeId: null,
  subjectId: null,
  file: null
})

const rules = {
  title: [
    { required: true, message: '请输入资料标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入资料描述', trigger: 'blur' },
    { min: 10, max: 500, message: '描述长度在 10 到 500 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择资料分类', trigger: 'change' }
  ],
  gradeId: [
    { required: true, message: '请选择适用年级', trigger: 'change' }
  ],
  subjectId: [
    { required: true, message: '请选择所属学科', trigger: 'change' }
  ],
  file: [
    { required: true, message: '请上传资料文件', trigger: 'change' }
  ]
}

const categories = ref([])
const grades = ref([])
const subjects = ref([])

const chunkUpload = useChunkUpload()

const uploadState = reactive({
  fileName: '',
  fileSize: 0,
  uploadedSize: 0,
  progress: 0,
  speed: 0,
  remainingTime: 0,
  status: 'idle',
  totalChunks: 0,
  completedCount: 0,
  chunks: [],
  failedChunks: [],
  error: ''
})

const showProgress = computed(() => {
  return uploadState.status !== 'idle'
})

const syncUploadState = () => {
  uploadState.fileName = chunkUpload.fileName.value
  uploadState.fileSize = chunkUpload.fileSize.value
  uploadState.uploadedSize = chunkUpload.uploadedSize.value
  uploadState.progress = chunkUpload.progress.value
  uploadState.speed = chunkUpload.speed.value
  uploadState.remainingTime = chunkUpload.remainingTime.value
  uploadState.status = chunkUpload.status.value
  uploadState.totalChunks = chunkUpload.totalChunks.value
  uploadState.completedCount = chunkUpload.completedCount.value
  uploadState.chunks = [...chunkUpload.chunks]
  uploadState.failedChunks = [...chunkUpload.failedChunks.value]
  uploadState.error = chunkUpload.error.value || ''
}

let stateWatcher = null

const startWatching = () => {
  if (stateWatcher) return
  stateWatcher = setInterval(syncUploadState, 100)
}

const stopWatching = () => {
  if (stateWatcher) {
    clearInterval(stateWatcher)
    stateWatcher = null
  }
}

const getFormData = () => {
  return {
    title: form.title,
    description: form.description,
    categoryId: form.categoryId,
    gradeId: form.gradeId,
    subjectId: form.subjectId,
    userId: DEFAULT_USER_ID
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (e) {
    categories.value = [
      { id: 1, name: '试卷' },
      { id: 2, name: '课件' },
      { id: 3, name: '笔记' },
      { id: 4, name: '作业' }
    ]
  }
}

const loadGrades = async () => {
  try {
    const res = await getGradeList()
    grades.value = res.data || []
  } catch (e) {
    grades.value = [
      { id: 1, name: '高一' },
      { id: 2, name: '高二' },
      { id: 3, name: '高三' },
      { id: 4, name: '大一' },
      { id: 5, name: '大二' }
    ]
  }
}

const loadSubjects = async () => {
  try {
    const res = await getSubjectList()
    subjects.value = res.data || []
  } catch (e) {
    subjects.value = [
      { id: 1, name: '语文' },
      { id: 2, name: '数学' },
      { id: 3, name: '英语' },
      { id: 4, name: '物理' },
      { id: 5, name: '化学' }
    ]
  }
}

const handleFileChange = (uploadFile, uploadFiles) => {
  form.file = uploadFile.raw
  fileList.value = uploadFiles
}

const handleFileRemove = () => {
  form.file = null
  fileList.value = []
}

const handleSubmit = async () => {
  if (!form.file) {
    ElMessage.warning('请上传资料文件')
    return
  }

  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  startWatching()

  try {
    const formData = getFormData()
    const success = await chunkUpload.startUpload(form.file, formData)

    if (success) {
      ElMessage.success('资料上传成功！即将跳转到详情页...')
      setTimeout(() => {
        if (chunkUpload.mergedData.value && chunkUpload.mergedData.value.id) {
          router.push(`/detail/${chunkUpload.mergedData.value.id}`)
        }
      }, 1500)
    } else if (chunkUpload.status.value === 'error') {
      ElMessage.error(chunkUpload.error.value || '上传失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '上传失败，请重试')
  } finally {
    submitting.value = false
  }
}

const handlePause = () => {
  chunkUpload.pauseUpload()
  syncUploadState()
  ElMessage.info('已暂停上传')
}

const handleResume = async () => {
  const formData = getFormData()
  try {
    const success = await chunkUpload.resumeUpload(formData)
    if (success) {
      ElMessage.success('上传完成！即将跳转到详情页...')
      setTimeout(() => {
        if (chunkUpload.mergedData.value && chunkUpload.mergedData.value.id) {
          router.push(`/detail/${chunkUpload.mergedData.value.id}`)
        }
      }, 1500)
    }
  } catch (e) {
    ElMessage.error('继续上传失败')
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要取消上传吗？已上传的分片将被清除。',
      '取消上传',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '继续上传',
        type: 'warning'
      }
    )
    await chunkUpload.cancelUpload()
    syncUploadState()
    stopWatching()
    ElMessage.info('已取消上传')
  } catch (e) {
    // 用户取消了确认
  }
}

const handleRetryAll = async () => {
  const formData = getFormData()
  try {
    const success = await chunkUpload.retryFailed(formData)
    if (success) {
      ElMessage.success('上传完成！即将跳转到详情页...')
      setTimeout(() => {
        if (chunkUpload.mergedData.value && chunkUpload.mergedData.value.id) {
          router.push(`/detail/${chunkUpload.mergedData.value.id}`)
        }
      }, 1500)
    }
  } catch (e) {
    ElMessage.error('重试失败')
  }
}

const handleRetryChunk = async (index) => {
  await chunkUpload.retryChunk(index)
  syncUploadState()
}

const handleReset = () => {
  formRef.value?.resetFields()
  uploadRef.value?.clearFiles()
  form.file = null
  fileList.value = []
}

const handleResetAll = () => {
  chunkUpload.reset()
  syncUploadState()
  stopWatching()
  handleReset()
}

const goToDetail = () => {
  if (chunkUpload.mergedData.value && chunkUpload.mergedData.value.id) {
    router.push(`/detail/${chunkUpload.mergedData.value.id}`)
  }
}

onMounted(() => {
  loadCategories()
  loadGrades()
  loadSubjects()
})
</script>

<style scoped>
.upload-page {
  max-width: 800px;
  margin: 0 auto;
}

.upload-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.upload-form {
  padding: 20px 0;
}

.upload-progress-section {
  padding: 10px 0;
}

.completion-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-upload-dragger) {
  padding: 40px 20px;
  transition: all 0.3s;
}

:deep(.el-upload-dragger:hover) {
  border-color: #409EFF;
  background: #f0f9ff;
}

:deep(.el-icon--upload) {
  font-size: 60px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

:deep(.el-upload__text) {
  font-size: 15px;
  color: #606266;
}

:deep(.el-upload__text em) {
  color: #409EFF;
  font-style: normal;
}

:deep(.el-upload__tip) {
  font-size: 13px;
  color: #909399;
  margin-top: 12px;
}
</style>
