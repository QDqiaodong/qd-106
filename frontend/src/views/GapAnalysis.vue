<template>
  <div class="gap-analysis">
    <div class="page-header">
      <h2>学科资料缺口统计</h2>
      <p class="page-desc">按年级、学科、分类维度分析资料覆盖度，快速识别内容空白区域</p>
    </div>

    <div v-if="loading" class="loading-wrapper">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else-if="data">
      <div class="overview-cards">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ data.totalMaterials }}</div>
            <div class="overview-label">资料总数</div>
          </div>
        </el-card>
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #67c23a, #42b983)">
            <el-icon :size="28"><CircleCheck /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ data.coverageRate }}%</div>
            <div class="overview-label">覆盖度</div>
          </div>
        </el-card>
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #e6a23c, #f56c6c)">
            <el-icon :size="28"><Warning /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ data.gapCount }}</div>
            <div class="overview-label">缺口项</div>
          </div>
        </el-card>
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #409eff, #337ecc)">
            <el-icon :size="28"><Grid /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ data.coveredSlots }}/{{ data.totalSlots }}</div>
            <div class="overview-label">已覆盖/总组合</div>
          </div>
        </el-card>
      </div>

      <div class="dimension-section">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#409EFF"><Histogram /></el-icon>
              <span>各维度资料数量</span>
            </div>
          </template>
          <el-tabs v-model="dimensionTab">
            <el-tab-pane label="按年级" name="grade">
              <div class="bar-chart">
                <div
                  v-for="item in data.gradeStats"
                  :key="item.gradeId"
                  class="bar-row"
                >
                  <span class="bar-label">{{ item.gradeName }}</span>
                  <div class="bar-track">
                    <div
                      class="bar-fill grade-fill"
                      :style="{ width: getBarWidth(item.count, maxGradeCount) + '%' }"
                    >
                      <span v-if="item.count > 0" class="bar-value">{{ item.count }}</span>
                    </div>
                  </div>
                  <el-tag
                    :type="item.count === 0 ? 'danger' : item.count <= 2 ? 'warning' : 'success'"
                    size="small"
                    class="bar-tag"
                  >
                    {{ item.count === 0 ? '空白' : item.count <= 2 ? '偏少' : '充足' }}
                  </el-tag>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="按学科" name="subject">
              <div class="bar-chart">
                <div
                  v-for="item in data.subjectStats"
                  :key="item.subjectId"
                  class="bar-row"
                >
                  <span class="bar-label">{{ item.subjectName }}</span>
                  <div class="bar-track">
                    <div
                      class="bar-fill subject-fill"
                      :style="{ width: getBarWidth(item.count, maxSubjectCount) + '%' }"
                    >
                      <span v-if="item.count > 0" class="bar-value">{{ item.count }}</span>
                    </div>
                  </div>
                  <el-tag
                    :type="item.count === 0 ? 'danger' : item.count <= 2 ? 'warning' : 'success'"
                    size="small"
                    class="bar-tag"
                  >
                    {{ item.count === 0 ? '空白' : item.count <= 2 ? '偏少' : '充足' }}
                  </el-tag>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="按分类" name="category">
              <div class="bar-chart">
                <div
                  v-for="item in data.categoryStats"
                  :key="item.categoryId"
                  class="bar-row"
                >
                  <span class="bar-label">{{ item.categoryName }}</span>
                  <div class="bar-track">
                    <div
                      class="bar-fill category-fill"
                      :style="{ width: getBarWidth(item.count, maxCategoryCount) + '%' }"
                    >
                      <span v-if="item.count > 0" class="bar-value">{{ item.count }}</span>
                    </div>
                  </div>
                  <el-tag
                    :type="item.count === 0 ? 'danger' : item.count <= 2 ? 'warning' : 'success'"
                    size="small"
                    class="bar-tag"
                  >
                    {{ item.count === 0 ? '空白' : item.count <= 2 ? '偏少' : '充足' }}
                  </el-tag>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>

      <div class="matrix-section">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#E6A23C"><Grid /></el-icon>
              <span>覆盖度矩阵（年级 × 学科）</span>
              <el-tag size="small" type="info" effect="plain" style="margin-left: 8px">
                数值为该年级+学科下所有分类资料总数
              </el-tag>
            </div>
          </template>
          <div class="matrix-wrapper">
            <div class="matrix-scroll">
              <table class="coverage-matrix">
                <thead>
                  <tr>
                    <th class="corner-cell">年级\学科</th>
                    <th v-for="subject in data.subjectStats" :key="subject.subjectId" class="subject-header">
                      {{ subject.subjectName }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="gradeRow in matrixGroupedByGrade" :key="gradeRow.gradeId">
                    <td class="grade-cell">{{ gradeRow.gradeName }}</td>
                    <td
                      v-for="subject in data.subjectStats"
                      :key="subject.subjectId"
                      class="count-cell"
                      :class="getCellClass(gradeRow.subjectCounts[subject.subjectId] || 0)"
                    >
                      <span class="cell-count">{{ gradeRow.subjectCounts[subject.subjectId] || 0 }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="matrix-legend">
            <span class="legend-item">
              <span class="legend-dot" style="background: #f56c6c"></span> 空白(0)
            </span>
            <span class="legend-item">
              <span class="legend-dot" style="background: #e6a23c"></span> 偏少(1-2)
            </span>
            <span class="legend-item">
              <span class="legend-dot" style="background: #67c23a"></span> 充足(3+)
            </span>
          </div>
        </el-card>
      </div>

      <div class="gap-section">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#F56C6C"><Warning /></el-icon>
              <span>缺口清单</span>
              <el-tag size="small" type="danger" effect="dark" style="margin-left: 8px">
                共 {{ filteredGaps.length }} 项
              </el-tag>
            </div>
          </template>
          <div class="gap-filters">
            <el-select v-model="filterGrade" placeholder="筛选年级" clearable size="small" style="width: 140px">
              <el-option v-for="g in data.gradeStats" :key="g.gradeId" :label="g.gradeName" :value="g.gradeId" />
            </el-select>
            <el-select v-model="filterSubject" placeholder="筛选学科" clearable size="small" style="width: 140px">
              <el-option v-for="s in data.subjectStats" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />
            </el-select>
            <el-select v-model="filterCategory" placeholder="筛选分类" clearable size="small" style="width: 140px">
              <el-option v-for="c in data.categoryStats" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
            </el-select>
            <el-button size="small" @click="clearFilters">重置</el-button>
          </div>
          <div v-if="filteredGaps.length === 0" class="gap-empty">
            <el-empty description="暂无缺口" :image-size="80" />
          </div>
          <div v-else class="gap-list">
            <div v-for="gap in paginatedGaps" :key="gap.gradeId + '-' + gap.subjectId + '-' + gap.categoryId" class="gap-item">
              <div class="gap-tags">
                <el-tag size="small">{{ gap.gradeName }}</el-tag>
                <el-tag size="small" type="warning">{{ gap.subjectName }}</el-tag>
                <el-tag size="small" type="success">{{ gap.categoryName }}</el-tag>
              </div>
              <div class="gap-desc">{{ gap.gradeName }}{{ gap.subjectName }}{{ gap.categoryName }} — 暂无资料</div>
            </div>
          </div>
          <div v-if="filteredGaps.length > gapPageSize" class="gap-pagination">
            <el-pagination
              v-model:current-page="gapCurrentPage"
              :page-size="gapPageSize"
              :total="filteredGaps.length"
              layout="prev, pager, next"
              small
            />
          </div>
        </el-card>
      </div>
    </template>

    <div v-else class="error-wrapper">
      <el-empty description="数据加载失败，请刷新重试" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Document, CircleCheck, Warning, Grid, Histogram } from '@element-plus/icons-vue'
import { getGapStatistics } from '@/api/material'

const loading = ref(true)
const data = ref(null)

const dimensionTab = ref('grade')
const filterGrade = ref(null)
const filterSubject = ref(null)
const filterCategory = ref(null)
const gapCurrentPage = ref(1)
const gapPageSize = 20

const maxGradeCount = computed(() => {
  if (!data.value) return 1
  return Math.max(...data.value.gradeStats.map(g => g.count), 1)
})
const maxSubjectCount = computed(() => {
  if (!data.value) return 1
  return Math.max(...data.value.subjectStats.map(s => s.count), 1)
})
const maxCategoryCount = computed(() => {
  if (!data.value) return 1
  return Math.max(...data.value.categoryStats.map(c => c.count), 1)
})

const filteredGaps = computed(() => {
  if (!data.value) return []
  return data.value.gapList.filter(g => {
    if (filterGrade.value && g.gradeId !== filterGrade.value) return false
    if (filterSubject.value && g.subjectId !== filterSubject.value) return false
    if (filterCategory.value && g.categoryId !== filterCategory.value) return false
    return true
  })
})

const paginatedGaps = computed(() => {
  const start = (gapCurrentPage.value - 1) * gapPageSize
  return filteredGaps.value.slice(start, start + gapPageSize)
})

const getBarWidth = (count, max) => {
  if (max === 0) return 0
  return Math.max(count / max * 100, count > 0 ? 8 : 0)
}

const getCellClass = (count) => {
  if (count === 0) return 'cell-empty'
  if (count <= 2) return 'cell-low'
  return 'cell-good'
}

const matrixGroupedByGrade = computed(() => {
  if (!data.value) return []
  const gradeMap = {}
  for (const row of data.value.coverageMatrix) {
    if (!gradeMap[row.gradeId]) {
      gradeMap[row.gradeId] = {
        gradeId: row.gradeId,
        gradeName: row.gradeName,
        subjectCounts: {}
      }
    }
    gradeMap[row.gradeId].subjectCounts[row.subjectId] = row.total
  }
  return Object.values(gradeMap)
})

const clearFilters = () => {
  filterGrade.value = null
  filterSubject.value = null
  filterCategory.value = null
  gapCurrentPage.value = 1
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getGapStatistics()
    data.value = res.data || null
  } catch (e) {
    console.error('加载缺口统计失败', e)
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.gap-analysis {
  width: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: #909399;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  border-radius: 8px;
}

.overview-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.overview-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.overview-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.dimension-section {
  margin-bottom: 24px;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}

.bar-chart {
  padding: 8px 0;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.bar-row:last-child {
  margin-bottom: 0;
}

.bar-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 14px;
  color: #303133;
  text-align: right;
}

.bar-track {
  flex: 1;
  height: 28px;
  background: #f5f7fa;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}

.bar-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.6s ease;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 10px;
  min-width: 0;
}

.grade-fill {
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.subject-fill {
  background: linear-gradient(90deg, #409eff, #337ecc);
}

.category-fill {
  background: linear-gradient(90deg, #67c23a, #42b983);
}

.bar-value {
  font-size: 12px;
  color: #fff;
  font-weight: 600;
  white-space: nowrap;
}

.bar-tag {
  width: 48px;
  text-align: center;
  flex-shrink: 0;
}

.matrix-section {
  margin-bottom: 24px;
}

.matrix-wrapper {
  overflow-x: auto;
}

.matrix-scroll {
  min-width: 600px;
}

.coverage-matrix {
  width: 100%;
  border-collapse: separate;
  border-spacing: 4px;
}

.coverage-matrix th,
.coverage-matrix td {
  text-align: center;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
}

.coverage-matrix th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.corner-cell {
  background: #ecf5ff !important;
  color: #409eff;
  font-weight: 600;
}

.subject-header {
  min-width: 70px;
}

.grade-cell {
  background: #f5f7fa;
  color: #303133;
  font-weight: 600;
  text-align: right !important;
  white-space: nowrap;
}

.count-cell {
  min-width: 60px;
  transition: all 0.2s;
}

.count-cell:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.cell-empty {
  background: #fef0f0;
  color: #f56c6c;
}

.cell-low {
  background: #fdf6ec;
  color: #e6a23c;
}

.cell-good {
  background: #f0f9eb;
  color: #67c23a;
}

.cell-count {
  font-weight: 700;
  font-size: 16px;
}

.matrix-legend {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.gap-section {
  margin-bottom: 24px;
}

.gap-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.gap-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.gap-item {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  padding: 12px 16px;
  transition: all 0.2s;
}

.gap-item:hover {
  background: #fde2e2;
  transform: translateY(-1px);
}

.gap-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.gap-desc {
  font-size: 14px;
  color: #f56c6c;
  font-weight: 500;
}

.gap-empty {
  padding: 20px 0;
}

.gap-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.loading-wrapper {
  padding: 40px;
}

.error-wrapper {
  padding: 60px 0;
}

@media (max-width: 960px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }

  .gap-list {
    grid-template-columns: 1fr;
  }
}
</style>
