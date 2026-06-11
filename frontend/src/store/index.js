import { defineStore } from 'pinia'
import { favoriteMaterial, unfavoriteMaterial, getMyFavorites, DEFAULT_USER_ID, recordDownload } from '@/api/material'

const VIEW_MODE_KEY = 'material_view_mode'
const BASKET_KEY = 'material_review_basket'

const getDefaultViewMode = () => {
  if (typeof window !== 'undefined') {
    const saved = localStorage.getItem(VIEW_MODE_KEY)
    if (saved === 'grid' || saved === 'list') {
      return saved
    }
    return window.innerWidth <= 768 ? 'list' : 'grid'
  }
  return 'grid'
}

const getDefaultBasket = () => {
  if (typeof window !== 'undefined') {
    try {
      const saved = localStorage.getItem(BASKET_KEY)
      return saved ? JSON.parse(saved) : []
    } catch (e) {
      return []
    }
  }
  return []
}

const persistBasket = (basket) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem(BASKET_KEY, JSON.stringify(basket))
  }
}

export const useAppStore = defineStore('app', {
  state: () => ({
    count: 0,
    viewMode: getDefaultViewMode(),
    favoriteMap: {},
    favoriteCount: 0,
    favoritesLoaded: false,
    reviewBasket: getDefaultBasket(),
    materialStats: {}
  }),
  getters: {
    isFavorite: (state) => (id) => {
      return !!state.favoriteMap[Number(id)]
    },
    isInBasket: (state) => (id) => {
      return state.reviewBasket.some(item => Number(item.id) === Number(id))
    },
    basketCount: (state) => state.reviewBasket.length,
    basketTotalSize: (state) => {
      return state.reviewBasket.reduce((sum, item) => sum + (Number(item.fileSize) || 0), 0)
    },
    basketLastAddedTime: (state) => {
      if (state.reviewBasket.length === 0) return null
      const sorted = [...state.reviewBasket].sort((a, b) => {
        const ta = a.addedAt ? new Date(a.addedAt).getTime() : 0
        const tb = b.addedAt ? new Date(b.addedAt).getTime() : 0
        return tb - ta
      })
      return sorted[0].addedAt || null
    },
    basketGroupedBySubject: (state) => {
      const groups = {}
      state.reviewBasket.forEach(item => {
        const key = item.subjectId || 'unknown'
        if (!groups[key]) {
          groups[key] = {
            subjectId: item.subjectId || null,
            subjectName: item.subjectName || '未分类',
            items: []
          }
        }
        groups[key].items.push(item)
      })
      return Object.values(groups)
    },
    getMaterialDownloadCount: (state) => (id) => {
      const numericId = Number(id)
      return state.materialStats[numericId]?.downloadCount ?? null
    },
    getMaterialViewCount: (state) => (id) => {
      const numericId = Number(id)
      return state.materialStats[numericId]?.viewCount ?? null
    },
    getEnrichedMaterial: (state) => (material) => {
      if (!material) return material
      const numericId = Number(material.id)
      const stats = state.materialStats[numericId]
      if (!stats) return material
      return {
        ...material,
        downloadCount: stats.downloadCount !== undefined ? stats.downloadCount : material.downloadCount,
        viewCount: stats.viewCount !== undefined ? stats.viewCount : material.viewCount
      }
    }
  },
  actions: {
    increment() {
      this.count++
    },
    setViewMode(mode) {
      this.viewMode = mode
      if (typeof window !== 'undefined') {
        localStorage.setItem(VIEW_MODE_KEY, mode)
      }
    },
    toggleViewMode() {
      const newMode = this.viewMode === 'grid' ? 'list' : 'grid'
      this.setViewMode(newMode)
      return newMode
    },
    async loadFavorites(force = false) {
      if (this.favoritesLoaded && !force) {
        return
      }
      try {
        const res = await getMyFavorites({ page: 1, size: 1000 })
        const list = res.data?.list || []
        const map = {}
        list.forEach(item => {
          map[Number(item.id)] = true
        })
        this.favoriteMap = map
        this.favoriteCount = res.data?.total || list.length
        this.favoritesLoaded = true
      } catch (e) {
        console.error('加载收藏列表失败', e)
      }
    },
    setFavorite(id, favorited) {
      const numericId = Number(id)
      if (favorited) {
        if (!this.favoriteMap[numericId]) {
          this.favoriteMap = { ...this.favoriteMap, [numericId]: true }
          this.favoriteCount++
        }
      } else {
        if (this.favoriteMap[numericId]) {
          const newMap = { ...this.favoriteMap }
          delete newMap[numericId]
          this.favoriteMap = newMap
          this.favoriteCount = Math.max(0, this.favoriteCount - 1)
        }
      }
    },
    async doFavorite(id) {
      try {
        await favoriteMaterial(id)
        this.setFavorite(id, true)
        return true
      } catch (e) {
        throw e
      }
    },
    async doUnfavorite(id) {
      try {
        await unfavoriteMaterial(id)
        this.setFavorite(id, false)
        return true
      } catch (e) {
        throw e
      }
    },
    async toggleFavorite(id) {
      const isFav = this.isFavorite(id)
      this.setFavorite(id, !isFav)
      try {
        if (isFav) {
          await unfavoriteMaterial(id)
        } else {
          await favoriteMaterial(id)
        }
        return !isFav
      } catch (e) {
        this.setFavorite(id, isFav)
        throw e
      }
    },
    setFavoriteCount(count) {
      this.favoriteCount = count
    },
    addToBasket(material) {
      if (this.isInBasket(material.id)) {
        return false
      }
      const itemWithMeta = {
        ...material,
        addedAt: new Date().toISOString()
      }
      this.reviewBasket = [...this.reviewBasket, itemWithMeta]
      persistBasket(this.reviewBasket)
      return true
    },
    removeFromBasket(id) {
      this.reviewBasket = this.reviewBasket.filter(item => Number(item.id) !== Number(id))
      persistBasket(this.reviewBasket)
    },
    clearBasket() {
      this.reviewBasket = []
      persistBasket(this.reviewBasket)
    },
    getBasketList() {
      return this.reviewBasket
    },
    setMaterialStats(material) {
      if (!material || material.id === undefined || material.id === null) return
      const numericId = Number(material.id)
      const existing = this.materialStats[numericId] || {}
      this.materialStats = {
        ...this.materialStats,
        [numericId]: {
          viewCount: material.viewCount !== undefined ? material.viewCount : existing.viewCount,
          downloadCount: material.downloadCount !== undefined ? material.downloadCount : existing.downloadCount
        }
      }
    },
    batchSetMaterialStats(list) {
      if (!Array.isArray(list) || list.length === 0) return
      const newStats = { ...this.materialStats }
      list.forEach(item => {
        if (item && item.id !== undefined && item.id !== null) {
          const numericId = Number(item.id)
          const existing = newStats[numericId] || {}
          newStats[numericId] = {
            viewCount: item.viewCount !== undefined ? item.viewCount : existing.viewCount,
            downloadCount: item.downloadCount !== undefined ? item.downloadCount : existing.downloadCount
          }
        }
      })
      this.materialStats = newStats
    },
    setMaterialDownloadCount(id, count) {
      const numericId = Number(id)
      const existing = this.materialStats[numericId] || {}
      this.materialStats = {
        ...this.materialStats,
        [numericId]: {
          ...existing,
          downloadCount: count
        }
      }
    },
    setMaterialViewCount(id, count) {
      const numericId = Number(id)
      const existing = this.materialStats[numericId] || {}
      this.materialStats = {
        ...this.materialStats,
        [numericId]: {
          ...existing,
          viewCount: count
        }
      }
    },
    incrementDownloadCount(id) {
      const numericId = Number(id)
      const existing = this.materialStats[numericId] || { downloadCount: 0, viewCount: 0 }
      const currentDownload = existing.downloadCount ?? 0
      this.materialStats = {
        ...this.materialStats,
        [numericId]: {
          ...existing,
          downloadCount: currentDownload + 1
        }
      }
      return currentDownload + 1
    },
    incrementViewCount(id) {
      const numericId = Number(id)
      const existing = this.materialStats[numericId] || { downloadCount: 0, viewCount: 0 }
      const currentView = existing.viewCount ?? 0
      this.materialStats = {
        ...this.materialStats,
        [numericId]: {
          ...existing,
          viewCount: currentView + 1
        }
      }
      return currentView + 1
    },
    async doRecordDownload(id) {
      const numericId = Number(id)
      try {
        const res = await recordDownload(numericId)
        const serverCount = res.data?.downloadCount
        if (serverCount !== undefined && serverCount !== null) {
          this.setMaterialDownloadCount(numericId, serverCount)
          return serverCount
        }
        return this.incrementDownloadCount(numericId)
      } catch (e) {
        return this.incrementDownloadCount(numericId)
      }
    }
  }
})
