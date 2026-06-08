<template>
  <div class="upload-page">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="22" color="#409EFF"><Upload /></el-icon>
          <span class="header-title">上传学习资料</span>
        </div>
      </template>

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
                支持 PDF、Word、PPT、Excel、TXT 格式，单个文件不超过 50MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
            <el-icon style="margin-right: 6px"><Check /></el-icon>
            提交上传
          </el-button>
          <el-button size="large" @click="handleReset">
            <el-icon style="margin-right: 6px"><RefreshLeft /></el-icon>
            重置表单
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadMaterial, getCategoryList, getGradeList, getSubjectList, DEFAULT_USER_ID } from '@/api/material'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
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

  loading.value = true
  try {
    const uploadData = {
      title: form.title,
      description: form.description,
      categoryId: form.categoryId,
      gradeId: form.gradeId,
      subjectId: form.subjectId,
      userId: DEFAULT_USER_ID,
      file: form.file
    }

    await uploadMaterial(uploadData)
    ElMessage.success('资料上传成功！')
    
    ElMessageBox.confirm(
      '资料上传成功，是否前往个人中心查看？',
      '上传成功',
      {
        confirmButtonText: '去查看',
        cancelButtonText: '继续上传',
        type: 'success'
      }
    ).then(() => {
      router.push('/profile')
    }).catch(() => {
      handleReset()
    })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '上传失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formRef.value?.resetFields()
  uploadRef.value?.clearFiles()
  form.file = null
  fileList.value = []
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
