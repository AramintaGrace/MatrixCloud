<template>
  <div class="admin-dashboard">
    <h2>系统信息概览</h2>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #409eff;">
            <el-icon :size="28"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #67c23a;">
            <el-icon :size="28"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.activeUsers }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #e6a23c;">
            <el-icon :size="28"><Connection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.onlineUsers }}</div>
            <div class="stat-label">在线用户</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #f56c6c;">
            <el-icon :size="28"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.bannedUsers }}</div>
            <div class="stat-label">封禁用户</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #909399;">
            <el-icon :size="28"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalTeams }}</div>
            <div class="stat-label">团队数量</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #5470c6;">
            <el-icon :size="28"><Cpu /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.cpuCores }}</div>
            <div class="stat-label">CPU核心数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #91cc75;">
            <el-icon :size="28"><Monitor /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatMemory(statistics.usedMemory) }}</div>
            <div class="stat-label">已用内存</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #fac858;">
            <el-icon :size="28"><Odometer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.systemLoadAverage.toFixed(2) }}</div>
            <div class="stat-label">系统负载</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <el-card class="chart-card">
        <template #header>
          <span>用户状态分布</span>
        </template>
        <div ref="userStatusChart" style="width: 100%; height: 300px;"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header>
          <span>用户角色分布</span>
        </template>
        <div ref="userRoleChart" style="width: 100%; height: 300px;"></div>
      </el-card>
    </div>

    <div class="charts-grid">
      <el-card class="chart-card full-width">
        <template #header>
          <span>系统资源使用情况</span>
        </template>
        <div ref="systemResourceChart" style="width: 100%; height: 300px;"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, CircleCheck, CircleClose, Connection, UserFilled, Cpu, Monitor, Odometer } from '@element-plus/icons-vue'
import api from '../api/request'
import * as echarts from 'echarts'

const statistics = ref({
  totalUsers: 0,
  activeUsers: 0,
  bannedUsers: 0,
  onlineUsers: 0,
  totalTeams: 0,
  cpuCores: 0,
  systemLoadAverage: 0,
  totalMemory: 0,
  usedMemory: 0,
  freeMemory: 0
})

const userStatusChart = ref(null)
const userRoleChart = ref(null)
const systemResourceChart = ref(null)

let statusChartInstance = null
let roleChartInstance = null
let resourceChartInstance = null

const loadStatistics = async () => {
  try {
    const response = await api.get('/admin/system-info')
    statistics.value = response.data
    initCharts()
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

const initCharts = () => {
  // 用户状态分布饼图
  if (userStatusChart.value) {
    statusChartInstance = echarts.init(userStatusChart.value)
    statusChartInstance.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: '用户状态',
          type: 'pie',
          radius: '50%',
          data: [
            { value: statistics.value.activeUsers, name: '活跃用户', itemStyle: { color: '#67c23a' } },
            { value: statistics.value.bannedUsers, name: '封禁用户', itemStyle: { color: '#f56c6c' } }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    })
  }

  // 用户角色分布饼图
  if (userRoleChart.value) {
    roleChartInstance = echarts.init(userRoleChart.value)
    roleChartInstance.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: '用户角色',
          type: 'pie',
          radius: '50%',
          data: [
            { value: statistics.value.totalUsers - 1, name: '普通用户', itemStyle: { color: '#409eff' } },
            { value: 1, name: '管理员', itemStyle: { color: '#e6a23c' } }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    })
  }

  // 系统资源使用情况柱状图
  if (systemResourceChart.value) {
    resourceChartInstance = echarts.init(systemResourceChart.value)
    resourceChartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['总用户', '活跃用户', '在线用户', '封禁用户']
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '数量',
          type: 'bar',
          data: [
            { value: statistics.value.totalUsers, itemStyle: { color: '#409eff' } },
            { value: statistics.value.activeUsers, itemStyle: { color: '#67c23a' } },
            { value: statistics.value.onlineUsers, itemStyle: { color: '#e6a23c' } },
            { value: statistics.value.bannedUsers, itemStyle: { color: '#f56c6c' } }
          ],
          barWidth: '60%'
        }
      ]
    })
  }
}

onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (statusChartInstance) statusChartInstance.dispose()
  if (roleChartInstance) roleChartInstance.dispose()
  if (resourceChartInstance) resourceChartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
  if (statusChartInstance) statusChartInstance.resize()
  if (roleChartInstance) roleChartInstance.resize()
  if (resourceChartInstance) resourceChartInstance.resize()
}

const formatMemory = (bytes) => {
  if (!bytes) return '0 MB'
  const mb = bytes / (1024 * 1024)
  return mb.toFixed(2) + ' MB'
}
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.admin-dashboard h2 {
  margin: 0 0 20px 0;
  color: #333;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  cursor: default;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
