import { defineStore } from 'pinia'

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
    viewMode: getDefaultViewMode()
  }),
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
    }
  }
})
