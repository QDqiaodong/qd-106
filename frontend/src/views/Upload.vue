<template>
  <div class="upload-page">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="22" color="#409EFF"><Upload /></el-icon>
          <span class="header-title">上传学习资料</span>
        </div>
      </template>

      <el-dialog
        v-model="showCheckDialog"
        title="资料信息校核"
        width="560px"
        :close-on-click-modal="false"
        class="check-dialog"
      >
        <div class="check-alert" v-if="checkIssues.length > 0">
          <el-alert
            v-for="(issue, idx) in checkIssues"
            :key="idx"
            :title="issue.message"
            :type="issue.level"
            :closable="false"
            show-icon
            class="check-alert-item"
          />
        </div>
        <el-alert
          v-else
          title="资料信息校核通过，所有项均填写完整"
          type="success"
          :closable="false"
          show-icon
          class="check-success"
        />

        <el-divider content-position="left">校核清单</el-divider>

        <div class="check-list">
          <div class="check-item" :class="{ 'item-error': !checkResult.title.valid }">
            <div class="check-label">
              <el-icon :size="18"><Edit /></el-icon>
              <span>资料标题</span>
            </div>
            <div class="check-value">
              <span class="value-text">{{ form.title || '(未填写)' }}</span>
              <el-tag v-if="checkResult.title.valid" type="success" size="small">
                <el-icon><CircleCheck /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.title.text }}</span>
              </el-tag>
              <el-tag v-else type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.title.text }}</span>
              </el-tag>
            </div>
          </div>

          <div class="check-item" :class="{ 'item-error': !checkResult.category.valid }">
            <div class="check-label">
              <el-icon :size="18"><Menu /></el-icon>
              <span>资料分类</span>
            </div>
            <div class="check-value">
              <span class="value-text">{{ getCategoryName(form.categoryId) }}</span>
              <el-tag v-if="checkResult.category.valid" type="success" size="small">
                <el-icon><CircleCheck /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.category.text }}</span>
              </el-tag>
              <el-tag v-else type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.category.text }}</span>
              </el-tag>
            </div>
          </div>

          <div class="check-item" :class="{ 'item-error': !checkResult.grade.valid }">
            <div class="check-label">
              <el-icon :size="18"><CollectionTag /></el-icon>
              <span>适用年级</span>
            </div>
            <div class="check-value">
              <span class="value-text">{{ getGradeName(form.gradeId) }}</span>
              <el-tag v-if="checkResult.grade.valid" type="success" size="small">
                <el-icon><CircleCheck /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.grade.text }}</span>
              </el-tag>
              <el-tag v-else type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.grade.text }}</span>
              </el-tag>
            </div>
          </div>

          <div class="check-item" :class="{ 'item-error': !checkResult.subject.valid }">
            <div class="check-label">
              <el-icon :size="18"><Reading /></el-icon>
              <span>所属学科</span>
            </div>
            <div class="check-value">
              <span class="value-text">{{ getSubjectName(form.subjectId) }}</span>
              <el-tag v-if="checkResult.subject.valid" type="success" size="small">
                <el-icon><CircleCheck /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.subject.text }}</span>
              </el-tag>
              <el-tag v-else type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.subject.text }}</span>
              </el-tag>
            </div>
          </div>

          <div class="check-item" :class="{ 'item-error': !checkResult.file.valid }">
            <div class="check-label">
              <el-icon :size="18"><Document /></el-icon>
              <span>上传文件</span>
            </div>
            <div class="check-value">
              <div class="file-info">
                <span class="value-text">{{ checkResult.file.name || '(未上传)' }}</span>
                <div class="file-meta" v-if="checkResult.file.name">
                  <el-tag size="small" type="info">{{ checkResult.file.format }}</el-tag>
                  <el-tag size="small">{{ checkResult.file.sizeText }}</el-tag>
                </div>
              </div>
              <el-tag v-if="checkResult.file.valid" type="success" size="small">
                <el-icon><CircleCheck /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.file.text }}</span>
              </el-tag>
              <el-tag v-else type="danger" size="small">
                <el-icon><CircleClose /></el-icon>
                <span style="margin-left: 2px;">{{ checkResult.file.text }}</span>
              </el-tag>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showCheckDialog = false">返回修改</el-button>
            <el-button
              type="primary"
              :disabled="checkIssues.some(i => i.level === 'error')"
              :loading="submitting"
              @click="confirmSubmit"
            >
              <el-icon><Check /></el-icon>
              <span style="margin-left: 4px;">确认提交</span>
            </el-button>
          </div>
        </template>
      </el-dialog>

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
  Plus,
  Edit,
  Menu,
  CollectionTag,
  Reading,
  Document,
  CircleCheck,
  CircleClose,
  WarningFilled
} from '@element-plus/icons-vue'
import { getCategoryList, getGradeList, getSubjectList, getValidationRules, DEFAULT_USER_ID } from '@/api/material'
import { useChunkUpload } from '@/utils/useChunkUpload'
import ChunkUploadProgress from '@/components/ChunkUploadProgress.vue'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const submitting = ref(false)
const fileList = ref([])
const showCheckDialog = ref(false)

const form = reactive({
  title: '',
  description: '',
  categoryId: null,
  gradeId: null,
  subjectId: null,
  file: null
})

const ALLOWED_EXTENSIONS = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt']
const MAX_FILE_SIZE = 500 * 1024 * 1024

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getFileExtension = (name) => {
  if (!name) return ''
  const idx = name.lastIndexOf('.')
  return idx > -1 ? name.substring(idx + 1).toLowerCase() : ''
}

const getCategoryName = (id) => {
  const cat = categories.value.find(c => c.id === id)
  return cat ? cat.name : '(未选择)'
}

const getGradeName = (id) => {
  const g = grades.value.find(x => x.id === id)
  return g ? g.name : '(未选择)'
}

const getSubjectName = (id) => {
  const s = subjects.value.find(x => x.id === id)
  return s ? s.name : '(未选择)'
}

const checkResult = computed(() => {
  const title = form.title.trim()
  const titleValid = title.length >= 5
  const titleLen = title.length

  const categoryValid = form.categoryId !== null && form.categoryId !== undefined
  const gradeValid = form.gradeId !== null && form.gradeId !== undefined
  const subjectValid = form.subjectId !== null && form.subjectId !== undefined

  let fileValid = false
  let fileName = ''
  let fileFormat = ''
  let fileSize = 0
  let fileSizeText = ''
  let fileText = '未上传文件'
  let fileHasWarning = false

  if (form.file) {
    fileName = form.file.name
    fileSize = form.file.size
    fileSizeText = formatFileSize(fileSize)
    fileFormat = getFileExtension(fileName)
    const extOk = ALLOWED_EXTENSIONS.includes(fileFormat)
    const sizeOk = fileSize > 0 && fileSize <= MAX_FILE_SIZE
    fileValid = extOk && sizeOk
    if (!extOk) {
      fileText = '格式不支持'
    } else if (!sizeOk) {
      fileText = '文件异常'
      fileHasWarning = true
    } else {
      fileText = '格式大小正常'
    }
  }

  return {
    title: {
      valid: titleValid,
      text: titleValid ? (titleLen >= 10 ? '标题完整' : '标题偏短') : '标题过短'
    },
    category: {
      valid: categoryValid,
      text: categoryValid ? '已选择' : '未选择'
    },
    grade: {
      valid: gradeValid,
      text: gradeValid ? '已选择' : '未选择'
    },
    subject: {
      valid: subjectValid,
      text: subjectValid ? '已选择' : '未选择'
    },
    file: {
      valid: fileValid,
      name: fileName,
      format: fileFormat.toUpperCase(),
      size: fileSize,
      sizeText: fileSizeText,
      text: fileText,
      hasWarning: fileHasWarning
    }
  }
})

const checkIssues = computed(() => {
  const issues = []
  const cr = checkResult.value
  if (!cr.title.valid) {
    issues.push({
      level: 'error',
      message: `标题过短（当前 ${form.title.trim().length} 字符），建议至少 5 个字符以上，标题完整度更高`
    })
  } else if (form.title.trim().length < 10) {
    issues.push({
      level: 'warning',
      message: '标题长度偏短，建议补充更多信息便于检索'
    })
  }
  if (!cr.category.valid) {
    issues.push({
      level: 'error',
      message: '请选择资料分类'
    })
  }
  if (!cr.grade.valid) {
    issues.push({
      level: 'warning',
      message: '建议选择适用年级，方便同学检索'
    })
  }
  if (!cr.subject.valid) {
    issues.push({
      level: 'warning',
      message: '建议选择所属学科，方便同学检索'
    })
  }
  if (!cr.file.valid) {
    if (!form.file) {
      issues.push({
        level: 'error',
        message: '请上传资料文件'
      })
    } else {
      const ext = getFileExtension(form.file.name)
      if (!ALLOWED_EXTENSIONS.includes(ext)) {
        issues.push({
          level: 'error',
          message: `文件格式 .${ext} 不支持，请上传 PDF、Word、PPT、Excel、TXT 格式`
        })
      } else if (form.file.size > MAX_FILE_SIZE) {
        issues.push({
          level: 'error',
          message: `文件大小超出限制（${formatFileSize(form.file.size)} > 500MB）`
        })
      } else if (form.file.size === 0) {
        issues.push({
          level: 'error',
          message: '文件为空，请重新选择'
        })
      }
    }
  }

  if (form.gradeId && form.subjectId) {
    const allowedSubjects = validationRules.value.gradeSubjectMap[form.gradeId]
    if (allowedSubjects && !allowedSubjects.includes(form.subjectId)) {
      const gradeName = getGradeName(form.gradeId)
      const subjectName = getSubjectName(form.subjectId)
      issues.push({
        level: 'error',
        message: `分类组合不合法：${gradeName}不开设${subjectName}，请检查年级与学科是否匹配`
      })
    }
  }

  if (form.title && form.title.trim() && form.gradeId) {
    const lowerTitle = form.title.toLowerCase()
    for (const rule of validationRules.value.keywordRules || []) {
      let matchedKeyword = null
      for (const keyword of rule.keywords || []) {
        if (lowerTitle.includes(keyword.toLowerCase())) {
          matchedKeyword = keyword
          break
        }
      }
      if (matchedKeyword && !(rule.allowedGradeIds || []).includes(form.gradeId)) {
        issues.push({
          level: 'warning',
          message: `标题含有"${matchedKeyword}"字样，更适合${rule.suitableGradeText}，当前选择的是${getGradeName(form.gradeId)}，请确认分类是否正确`
        })
        break
      }
    }
  }

  return issues
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
const validationRules = ref({
  gradeSubjectMap: {},
  keywordRules: []
})

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

const loadValidationRules = async () => {
  try {
    const res = await getValidationRules()
    if (res.data) {
      validationRules.value = res.data
    }
  } catch (e) {
    validationRules.value = {
      gradeSubjectMap: {
        1: [1, 2, 3, 10], 2: [1, 2, 3, 10], 3: [1, 2, 3, 10],
        4: [1, 2, 3, 10], 5: [1, 2, 3, 10], 6: [1, 2, 3, 10],
        7: [1, 2, 3, 4, 5, 6, 7, 8, 9], 8: [1, 2, 3, 4, 5, 6, 7, 8, 9], 9: [1, 2, 3, 4, 5, 6, 7, 8, 9],
        10: [1, 2, 3, 4, 5, 6, 7, 8, 9], 11: [1, 2, 3, 4, 5, 6, 7, 8, 9], 12: [1, 2, 3, 4, 5, 6, 7, 8, 9]
      },
      keywordRules: [
        { keywords: ['识字', '拼音', '口算', '加减法', '乘法口诀', '生字', '看图写话', '三字经', '弟子规', '笔画', '笔顺'], allowedGradeIds: [1, 2, 3], suitableGradeText: '小学低年级（一至三年级）' },
        { keywords: ['小学奥数', '小升初', '小学数学思维'], allowedGradeIds: [4, 5, 6], suitableGradeText: '小学高年级（四至六年级）' },
        { keywords: ['初中', '中考', '八年级', '九年级', '初一', '初二', '初三'], allowedGradeIds: [7, 8, 9], suitableGradeText: '初中（七至九年级）' },
        { keywords: ['高中', '高考', '高一', '高二', '高三', '必修一', '必修二', '必修三', '选择性必修', '一轮复习', '二轮复习', '三轮复习'], allowedGradeIds: [10, 11, 12], suitableGradeText: '高中（高一至高三）' }
      ]
    }
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
  try {
    await formRef.value.validate()
  } catch (e) {
    ElMessage.warning('请先完善表单必填项')
    return
  }
  showCheckDialog.value = true
}

const confirmSubmit = async () => {
  if (checkIssues.value.some(i => i.level === 'error')) {
    ElMessage.warning('请先修复校核中的错误项')
    return
  }
  showCheckDialog.value = false
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
  loadValidationRules()
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

.check-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 0;
  padding: 16px 20px;
}

.check-dialog :deep(.el-dialog__title) {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.check-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: rgba(255, 255, 255, 0.9);
}

.check-dialog :deep(.el-dialog__body) {
  padding: 24px 24px 12px 24px;
}

.check-alert {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.check-alert-item {
  margin: 0 !important;
}

.check-success {
  margin-bottom: 16px;
}

.check-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.check-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.check-item:hover {
  background: #f5f7fa;
  border-color: #e4e7ed;
}

.check-item.item-error {
  background: #fef0f0;
  border-color: #fbc4c4;
}

.check-label {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 90px;
  font-weight: 500;
  color: #303133;
  flex-shrink: 0;
}

.check-label .el-icon {
  color: #409eff;
}

.check-item.item-error .check-label .el-icon {
  color: #f56c6c;
}

.check-value {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.value-text {
  color: #606266;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}

.check-item.item-error .value-text {
  color: #f56c6c;
  font-weight: 500;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-meta {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
