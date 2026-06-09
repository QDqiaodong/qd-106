import request from './request'

export const DEFAULT_USER_ID = 1

export function getMaterialList(params) {
  return request({
    url: '/materials',
    method: 'get',
    params
  })
}

export function getMaterialDetail(id) {
  return request({
    url: `/materials/${id}`,
    method: 'get'
  })
}

export function uploadMaterial(data) {
  const formData = new FormData()
  Object.keys(data).forEach(key => {
    if (data[key] !== undefined && data[key] !== null) {
      formData.append(key, data[key])
    }
  })
  return request({
    url: '/materials',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deleteMaterial(id) {
  return request({
    url: `/materials/${id}`,
    method: 'delete',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function getMyUploads(params) {
  return request({
    url: '/materials/my',
    method: 'get',
    params: { ...params, userId: DEFAULT_USER_ID }
  })
}

export function getCategoryList() {
  return request({
    url: '/categories',
    method: 'get'
  })
}

export function getGradeList() {
  return request({
    url: '/grades',
    method: 'get'
  })
}

export function getSubjectList() {
  return request({
    url: '/subjects',
    method: 'get'
  })
}

export function favoriteMaterial(id) {
  return request({
    url: `/materials/${id}/favorite`,
    method: 'post',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function unfavoriteMaterial(id) {
  return request({
    url: `/materials/${id}/favorite`,
    method: 'delete',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function getMyFavorites(params) {
  return request({
    url: '/favorites',
    method: 'get',
    params: { ...params, userId: DEFAULT_USER_ID }
  })
}

export function getHotMaterials() {
  return request({
    url: '/materials/hot',
    method: 'get'
  })
}

export function initChunkUpload(fileName, fileSize, chunkSize = 5 * 1024 * 1024, fileMd5 = '') {
  return request({
    url: '/materials/chunk/init',
    method: 'post',
    params: { fileName, fileSize, chunkSize, fileMd5 }
  })
}

export function uploadChunk(uploadId, chunkIndex, chunk, onUploadProgress) {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', chunkIndex)
  formData.append('chunk', chunk)
  return request({
    url: '/materials/chunk/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 0,
    onUploadProgress
  })
}

export function getChunkStatus(uploadId) {
  return request({
    url: '/materials/chunk/status',
    method: 'get',
    params: { uploadId }
  })
}

export function mergeChunks(uploadId, data) {
  return request({
    url: '/materials/chunk/merge',
    method: 'post',
    params: { uploadId, ...data }
  })
}

export function cancelChunkUpload(uploadId) {
  return request({
    url: '/materials/chunk/cancel',
    method: 'delete',
    params: { uploadId }
  })
}
