import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  initChunkUpload,
  uploadChunk,
  getChunkStatus,
  mergeChunks,
  cancelChunkUpload
} from '@/api/material'

const CHUNK_SIZE = 5 * 1024 * 1024
const CONCURRENT_COUNT = 3

export function useChunkUpload() {
  const uploadId = ref('')
  const file = ref(null)
  const fileName = ref('')
  const fileSize = ref(0)
  const totalChunks = ref(0)
  const chunkSize = ref(CHUNK_SIZE)
  const status = ref('idle')
  const uploadedSize = ref(0)
  const speed = ref(0)
  const remainingTime = ref(0)
  const error = ref(null)
  const mergedData = ref(null)

  const chunks = reactive([])
  const failedChunks = ref([])
  const uploadingChunkIndices = ref(new Set())

  let startTimestamp = 0
  let lastUploadedSize = 0
  let speedTimer = null
  let uploadAbortController = null
  let isPaused = false

  const progress = computed(() => {
    if (fileSize.value === 0) return 0
    return Math.min(100, Math.round((uploadedSize.value / fileSize.value) * 100))
  })

  const completedCount = computed(() => {
    return chunks.filter(c => c.status === 'completed').length
  })

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  }

  const formatTime = (seconds) => {
    if (seconds <= 0 || !isFinite(seconds)) return '计算中...'
    if (seconds < 60) return Math.round(seconds) + ' 秒'
    if (seconds < 3600) {
      const mins = Math.floor(seconds / 60)
      const secs = Math.round(seconds % 60)
      return `${mins} 分 ${secs} 秒`
    }
    const hours = Math.floor(seconds / 3600)
    const mins = Math.floor((seconds % 3600) / 60)
    return `${hours} 小时 ${mins} 分`
  }

  const createChunks = (fileObj) => {
    const chunksList = []
    const total = Math.ceil(fileObj.size / CHUNK_SIZE)
    for (let i = 0; i < total; i++) {
      const start = i * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, fileObj.size)
      chunksList.push({
        index: i,
        start,
        end,
        size: end - start,
        status: 'waiting',
        progress: 0,
        error: null
      })
    }
    return chunksList
  }

  const startSpeedMonitor = () => {
    stopSpeedMonitor()
    startTimestamp = Date.now()
    lastUploadedSize = uploadedSize.value
    speedTimer = setInterval(() => {
      const elapsed = (Date.now() - startTimestamp) / 1000
      const uploaded = uploadedSize.value - lastUploadedSize
      if (elapsed > 0) {
        const currentSpeed = uploadedSize.value / elapsed
        speed.value = currentSpeed
        if (currentSpeed > 0) {
          remainingTime.value = (fileSize.value - uploadedSize.value) / currentSpeed
        }
      }
    }, 1000)
  }

  const stopSpeedMonitor = () => {
    if (speedTimer) {
      clearInterval(speedTimer)
      speedTimer = null
    }
  }

  const uploadSingleChunk = async (chunkData) => {
    if (isPaused || status.value === 'cancelled') return false

    chunkData.status = 'uploading'
    uploadingChunkIndices.value.add(chunkData.index)

    const blob = file.value.slice(chunkData.start, chunkData.end)
    const chunkBlob = new Blob([blob], { type: file.value.type })

    try {
      await uploadChunk(uploadId.value, chunkData.index, chunkBlob, (progressEvent) => {
        if (progressEvent.total > 0) {
          chunkData.progress = Math.round((progressEvent.loaded / progressEvent.total) * 100)
          recalcTotalProgress()
        }
      })

      chunkData.status = 'completed'
      chunkData.progress = 100
      chunkData.error = null
      failedChunks.value = failedChunks.value.filter(idx => idx !== chunkData.index)
      return true
    } catch (err) {
      chunkData.status = 'failed'
      chunkData.error = err.message || '上传失败'
      if (!failedChunks.value.includes(chunkData.index)) {
        failedChunks.value.push(chunkData.index)
      }
      return false
    } finally {
      uploadingChunkIndices.value.delete(chunkData.index)
      recalcTotalProgress()
    }
  }

  const recalcTotalProgress = () => {
    let totalUploaded = 0
    chunks.forEach(chunk => {
      if (chunk.status === 'completed') {
        totalUploaded += chunk.size
      } else if (chunk.status === 'uploading') {
        totalUploaded += chunk.size * (chunk.progress / 100)
      }
    })
    uploadedSize.value = totalUploaded
  }

  const uploadChunksConcurrent = async () => {
    const waitingChunks = chunks.filter(c => c.status === 'waiting' || c.status === 'failed')
    const chunkQueue = [...waitingChunks]

    const workers = []
    for (let i = 0; i < CONCURRENT_COUNT; i++) {
      workers.push(
        (async () => {
          while (chunkQueue.length > 0 && !isPaused && status.value !== 'cancelled') {
            const chunk = chunkQueue.shift()
            if (chunk) {
              await uploadSingleChunk(chunk)
            }
          }
        })()
      )
    }

    await Promise.all(workers)
  }

  const checkResume = async (existingUploadId) => {
    try {
      const res = await getChunkStatus(existingUploadId)
      if (res.code === 200 && res.data) {
        const statusData = res.data
        if (statusData.chunks && statusData.chunks.length > 0) {
          statusData.chunks.forEach(chunkInfo => {
            if (chunkInfo.uploaded && chunks[chunkInfo.index]) {
              chunks[chunkInfo.index].status = 'completed'
              chunks[chunkInfo.index].progress = 100
            }
          })
          recalcTotalProgress()
          return true
        }
      }
    } catch (e) {
      // ignore
    }
    return false
  }

  const startUpload = async (fileObj, formData) => {
    if (!fileObj) {
      ElMessage.error('请选择文件')
      return false
    }

    file.value = fileObj
    fileName.value = fileObj.name
    fileSize.value = fileObj.size
    status.value = 'initializing'
    error.value = null
    isPaused = false
    mergedData.value = null

    const chunksList = createChunks(fileObj)
    chunks.splice(0, chunks.length, ...chunksList)
    totalChunks.value = chunksList.length
    uploadedSize.value = 0
    failedChunks.value = []

    try {
      const initRes = await initChunkUpload(fileObj.name, fileObj.size, CHUNK_SIZE)
      if (initRes.code !== 200) {
        throw new Error(initRes.message || '初始化失败')
      }

      uploadId.value = initRes.data.uploadId
      totalChunks.value = initRes.data.totalChunks
      chunkSize.value = initRes.data.chunkSize

      status.value = 'uploading'
      startSpeedMonitor()

      await uploadChunksConcurrent()

      if (isPaused || status.value === 'cancelled') {
        stopSpeedMonitor()
        return false
      }

      if (failedChunks.value.length > 0) {
        status.value = 'error'
        error.value = '部分分片上传失败，请重试'
        stopSpeedMonitor()
        speed.value = 0
        remainingTime.value = 0
        return false
      }

      status.value = 'merging'
      stopSpeedMonitor()
      speed.value = 0
      remainingTime.value = 0

      const mergeRes = await mergeChunks(uploadId.value, formData)
      if (mergeRes.code !== 200) {
        throw new Error(mergeRes.message || '合并失败')
      }

      status.value = 'completed'
      mergedData.value = mergeRes.data
      uploadedSize.value = fileSize.value
      return true
    } catch (err) {
      status.value = 'error'
      error.value = err.message || '上传失败'
      stopSpeedMonitor()
      speed.value = 0
      remainingTime.value = 0
      return false
    }
  }

  const pauseUpload = () => {
    if (status.value !== 'uploading') return
    isPaused = true
    status.value = 'paused'
    stopSpeedMonitor()
  }

  const resumeUpload = async (formData) => {
    if (status.value !== 'paused' && status.value !== 'error') return

    isPaused = false
    status.value = 'uploading'
    startSpeedMonitor()

    try {
      await uploadChunksConcurrent()

      if (isPaused || status.value === 'cancelled') {
        stopSpeedMonitor()
        return false
      }

      if (failedChunks.value.length > 0) {
        status.value = 'error'
        error.value = '部分分片上传失败，请重试'
        stopSpeedMonitor()
        speed.value = 0
        remainingTime.value = 0
        return false
      }

      status.value = 'merging'
      stopSpeedMonitor()
      speed.value = 0
      remainingTime.value = 0

      const mergeRes = await mergeChunks(uploadId.value, formData)
      if (mergeRes.code !== 200) {
        throw new Error(mergeRes.message || '合并失败')
      }

      status.value = 'completed'
      mergedData.value = mergeRes.data
      uploadedSize.value = fileSize.value
      return true
    } catch (err) {
      status.value = 'error'
      error.value = err.message || '上传失败'
      stopSpeedMonitor()
      speed.value = 0
      remainingTime.value = 0
      return false
    }
  }

  const retryChunk = async (index) => {
    const chunk = chunks[index]
    if (!chunk || chunk.status === 'uploading') return

    chunk.status = 'waiting'
    chunk.error = null
    chunk.progress = 0
    failedChunks.value = failedChunks.value.filter(idx => idx !== index)

    if (status.value !== 'uploading') {
      status.value = 'uploading'
      startSpeedMonitor()
    }

    await uploadSingleChunk(chunk)
  }

  const retryFailed = async (formData) => {
    if (failedChunks.value.length === 0) return

    failedChunks.value.forEach(index => {
      if (chunks[index]) {
        chunks[index].status = 'waiting'
        chunks[index].error = null
        chunks[index].progress = 0
      }
    })
    failedChunks.value = []

    await resumeUpload(formData)
  }

  const cancelUpload = async () => {
    if (status.value === 'idle' || status.value === 'completed') return

    isPaused = true
    status.value = 'cancelling'

    try {
      if (uploadId.value) {
        await cancelChunkUpload(uploadId.value)
      }
    } catch (e) {
      // ignore
    }

    stopSpeedMonitor()
    status.value = 'cancelled'
    speed.value = 0
    remainingTime.value = 0
    uploadId.value = ''
    file.value = null
  }

  const reset = () => {
    stopSpeedMonitor()
    uploadId.value = ''
    file.value = null
    fileName.value = ''
    fileSize.value = 0
    totalChunks.value = 0
    chunkSize.value = CHUNK_SIZE
    status.value = 'idle'
    uploadedSize.value = 0
    speed.value = 0
    remainingTime.value = 0
    error.value = null
    mergedData.value = null
    chunks.splice(0, chunks.length)
    failedChunks.value = []
    uploadingChunkIndices.value.clear()
    isPaused = false
  }

  return {
    uploadId,
    file,
    fileName,
    fileSize,
    totalChunks,
    chunkSize,
    status,
    uploadedSize,
    speed,
    remainingTime,
    progress,
    completedCount,
    chunks,
    failedChunks,
    error,
    mergedData,
    formatFileSize,
    formatTime,
    startUpload,
    pauseUpload,
    resumeUpload,
    retryChunk,
    retryFailed,
    cancelUpload,
    reset
  }
}
