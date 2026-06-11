import request from './request'

export const DEFAULT_USER_ID = 1

export function getMaterialList(params) {
  return request({
    url: '/materials',
    method: 'get',
    params: { ...params, userId: DEFAULT_USER_ID }
  })
}

export function getMaterialDetail(id) {
  return request({
    url: `/materials/${id}`,
    method: 'get',
    params: { userId: DEFAULT_USER_ID }
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

export function updateReviewStatus(materialId, status) {
  return request({
    url: `/favorites/${materialId}/review-status`,
    method: 'put',
    params: { userId: DEFAULT_USER_ID, status }
  })
}

export function getReviewStatus(materialId) {
  return request({
    url: `/favorites/${materialId}/review-status`,
    method: 'get',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function getHotMaterials(range) {
  return request({
    url: '/materials/hot',
    method: 'get',
    params: range ? { range } : {}
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

export function getBookmarks(materialId) {
  return request({
    url: `/materials/${materialId}/bookmarks`,
    method: 'get',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function addBookmark(materialId, data) {
  return request({
    url: `/materials/${materialId}/bookmarks`,
    method: 'post',
    params: { userId: DEFAULT_USER_ID, ...data }
  })
}

export function updateBookmark(materialId, id, data) {
  return request({
    url: `/materials/${materialId}/bookmarks/${id}`,
    method: 'put',
    params: { userId: DEFAULT_USER_ID, ...data }
  })
}

export function deleteBookmark(materialId, id) {
  return request({
    url: `/materials/${materialId}/bookmarks/${id}`,
    method: 'delete',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function getReadingProgress(materialId) {
  return request({
    url: `/materials/${materialId}/progress`,
    method: 'get',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function saveReadingProgress(materialId, pageNumber) {
  return request({
    url: `/materials/${materialId}/progress`,
    method: 'post',
    params: { userId: DEFAULT_USER_ID, pageNumber }
  })
}

export function deleteReadingProgress(materialId) {
  return request({
    url: `/materials/${materialId}/progress`,
    method: 'delete',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function getFilterSnapshots() {
  return request({
    url: '/filter-snapshots',
    method: 'get',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function createFilterSnapshot(data) {
  return request({
    url: '/filter-snapshots',
    method: 'post',
    params: { userId: DEFAULT_USER_ID, ...data }
  })
}

export function updateFilterSnapshot(id, data) {
  return request({
    url: `/filter-snapshots/${id}`,
    method: 'put',
    params: { userId: DEFAULT_USER_ID, ...data }
  })
}

export function deleteFilterSnapshot(id) {
  return request({
    url: `/filter-snapshots/${id}`,
    method: 'delete',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function submitCorrection(data) {
  return request({
    url: '/corrections',
    method: 'post',
    params: { userId: DEFAULT_USER_ID, ...data }
  })
}

export function getCorrectionsByMaterial(materialId, params = {}) {
  return request({
    url: `/corrections/material/${materialId}`,
    method: 'get',
    params
  })
}

export function getMyCorrections(params = {}) {
  return request({
    url: '/corrections/my',
    method: 'get',
    params: { userId: DEFAULT_USER_ID, ...params }
  })
}

export function getUploaderCorrections(params = {}) {
  return request({
    url: '/corrections/uploader',
    method: 'get',
    params: { userId: DEFAULT_USER_ID, ...params }
  })
}

export function getUploaderCorrectionStats() {
  return request({
    url: '/corrections/uploader/stats',
    method: 'get',
    params: { userId: DEFAULT_USER_ID }
  })
}

export function handleCorrection(id, status, handleRemark = '') {
  return request({
    url: `/corrections/${id}/handle`,
    method: 'put',
    params: { userId: DEFAULT_USER_ID, status, handleRemark }
  })
}

export function getValidationRules() {
  return request({
    url: '/materials/validation-rules',
    method: 'get'
  })
}
