import { defineStore } from 'pinia'
import { favoriteMaterial, unfavoriteMaterial, getMyFavorites, DEFAULT_USER_ID } from '@/api/material'

const VIEW_MODE_KEY = 'material_view_mode'
const REVIEW_BASKET_KEY = 'review_basket'

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

const loadBasketFromStorage = () => {
  if (typeof window !== 'undefined') {
    try {
      const saved = localStorage.getItem(REVIEW_BASKET_KEY)
      return saved ? JSON.parse(saved) : []
    } catch (e) {
      return []
    }
  }
  return []
}

const saveBasketToStorage = (list) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem(REVIEW_BASKET_KEY, JSON.stringify(list))
  }
}

export const useAppStore = defineStore('app', {
  state: () => ({
    count: 0,
    viewMode: getDefaultViewMode(),
    favoriteMap: {},
    favoriteCount: 0,
    favoritesLoaded: false,
    reviewBasket: loadBasketFromStorage()
  }),
  getters: {
    isFavorite: (state) => (id) => {
      return !!state.favoriteMap[Number(id)]
    },
    isInBasket: (state) => (id) => {
      return state.reviewBasket.some(item => Number(item.id) === Number(id))
    },
    basketCount: (state) => {
      return state.reviewBasket.length
    },
    basketTotalSize: (state) => {
      return state.reviewBasket.reduce((sum, item) => sum + (item.fileSize || 0), 0)
    },
    basketLastAddedTime: (state) => {
      if (state.reviewBasket.length === 0) return null
      const sorted = [...state.reviewBasket].sort((a, b) => {
        return new Date(b.addedAt) - new Date(a.addedAt)
      })
      return sorted[0].addedAt
    },
    basketGroupedBySubject: (state) => {
      const groups = {}
      state.reviewBasket.forEach(item => {
        const key = item.subjectId || 'unknown'
        if (!groups[key]) {
          groups[key] = {
            subjectId: item.subjectId,
            subjectName: item.subjectName || '未指定学科',
            items: []
          }
        }
        groups[key].items.push(item)
      })
      return Object.values(groups).sort((a, b) => {
        if (a.subjectId === 'unknown') return 1
        if (b.subjectId === 'unknown') return -1
        return (a.subjectName || '').localeCompare(b.subjectName || '')
      })
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
        const map = { ...this.favoriteMap }
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
      if (isFav) {
        await this.doUnfavorite(id)
      } else {
        await this.doFavorite(id)
      }
      return !isFav
    },
    setFavoriteCount(count) {
      this.favoriteCount = count
    },
    addToBasket(material) {
      const numericId = Number(material.id)
      if (this.isInBasket(numericId)) {
        return false
      }
      const item = {
        id: numericId,
        title: material.title,
        description: material.description,
        categoryId: material.categoryId,
        categoryName: material.categoryName,
        gradeId: material.gradeId,
        gradeName: material.gradeName,
        subjectId: material.subjectId,
        subjectName: material.subjectName,
        fileSize: material.fileSize || 0,
        fileFormat: material.fileFormat || '',
        fileUrl: material.fileUrl,
        cover: material.cover,
        addedAt: new Date().toISOString()
      }
      this.reviewBasket = [...this.reviewBasket, item]
      saveBasketToStorage(this.reviewBasket)
      return true
    },
    removeFromBasket(id) {
      const numericId = Number(id)
      const idx = this.reviewBasket.findIndex(item => Number(item.id) === numericId)
      if (idx === -1) return false
      const newList = [...this.reviewBasket]
      newList.splice(idx, 1)
      this.reviewBasket = newList
      saveBasketToStorage(this.reviewBasket)
      return true
    },
    clearBasket() {
      this.reviewBasket = []
      saveBasketToStorage(this.reviewBasket)
    }
  }
})
