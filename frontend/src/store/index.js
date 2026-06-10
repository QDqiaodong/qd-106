import { defineStore } from 'pinia'
import { favoriteMaterial, unfavoriteMaterial, getMyFavorites, DEFAULT_USER_ID } from '@/api/material'

const VIEW_MODE_KEY = 'material_view_mode'

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

export const useAppStore = defineStore('app', {
  state: () => ({
    count: 0,
    viewMode: getDefaultViewMode(),
    favoriteMap: {},
    favoriteCount: 0,
    favoritesLoaded: false
  }),
  getters: {
    isFavorite: (state) => (id) => {
      return !!state.favoriteMap[Number(id)]
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
      if (isFav) {
        await this.doUnfavorite(id)
      } else {
        await this.doFavorite(id)
      }
      return !isFav
    },
    setFavoriteCount(count) {
      this.favoriteCount = count
    }
  }
})
