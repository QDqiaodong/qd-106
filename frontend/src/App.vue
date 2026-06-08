<template>
  <div id="app">
    <el-container>
      <el-header class="navbar">
        <div class="navbar-content">
          <div class="logo" @click="$router.push('/')">
            <el-icon :size="28" color="#409EFF"><Reading /></el-icon>
            <span class="logo-text">校园学习资料共享</span>
          </div>
          <el-menu
            mode="horizontal"
            :default-active="$route.path"
            class="nav-menu"
            router
          >
            <el-menu-item index="/">
              <el-icon><House /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/upload">
              <el-icon><Upload /></el-icon>
              <span>上传资料</span>
            </el-menu-item>
            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { Reading, House, Upload, User } from '@element-plus/icons-vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  height: 64px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.navbar-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.nav-menu {
  background: transparent;
  border: none;
}

.nav-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
  border-bottom: 2px solid transparent;
}

.nav-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.nav-menu .el-menu-item.is-active {
  color: #fff;
  border-bottom-color: #fff;
  background-color: rgba(255, 255, 255, 0.15);
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: 24px;
}

.el-container {
  min-height: 100vh;
}

.el-header {
  --el-header-padding: 0;
}
</style>
