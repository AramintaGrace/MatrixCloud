<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>用户管理</h2>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户昵称或邮箱"
        style="width: 400px;"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 用户列表 -->
    <el-card class="user-list-card">
      <el-table :data="filteredUsers" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="email" label="邮箱" width="220" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="department" label="部门" width="150" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 'ADMIN' && row.status === 'ACTIVE'"
              type="danger"
              size="small"
              @click="handleBanUser(row)"
            >
              封禁
            </el-button>
            <el-button
              v-if="row.role !== 'ADMIN' && row.status === 'BANNED'"
              type="success"
              size="small"
              @click="handleUnbanUser(row)"
            >
              解封
            </el-button>
            <span v-if="row.role === 'ADMIN'" style="color: #909399;">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import axios from 'axios'

const users = ref([])
const searchKeyword = ref('')

const filteredUsers = computed(() => {
  if (!searchKeyword.value) {
    return users.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return users.value.filter(user =>
    user.nickname.toLowerCase().includes(keyword) ||
    user.email.toLowerCase().includes(keyword)
  )
})

const loadUsers = async () => {
  try {
    const response = await api.get('/admin/users')
    users.value = response.data
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  }
}

const handleSearch = () => {
  // 搜索由 computed 自动处理
}

const handleBanUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要封禁用户 ${user.nickname} 吗？`,
      '确认封禁',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await axios.post(`/api/admin/users/${user.id}/ban`)
    ElMessage.success('用户已封禁')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('封禁失败')
    }
  }
}

const handleUnbanUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要解封用户 ${user.nickname} 吗？`,
      '确认解封',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await axios.post(`/api/admin/users/${user.id}/unban`)
    ElMessage.success('用户已解封')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('解封失败')
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.admin-container {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.admin-header {
  margin-bottom: 20px;
}

.admin-header h2 {
  margin: 0;
  color: #333;
}

.search-bar {
  margin-bottom: 20px;
}

.user-list-card {
  /* 统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #409eff;">
            <el-icon :size="24"><User /></el-icon>
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
            <el-icon :size="24"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.activeUsers }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #f56c6c;">
            <el-icon :size="24"><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.bannedUsers }}</div>
            <div class="stat-label">封禁用户</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" style="background: #e6a23c;">
            <el-icon :size="24"><Connection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.onlineUsers }}</div>
            <div class="stat-label">在线用户</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 用户列表 -->
    <el-card class="user-list-card">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户昵称"
            style="width: 300px;"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="filteredUsers" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="department" label="部门" width="150" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 'ADMIN' && row.status === 'ACTIVE'"
              type="danger"
              size="small"
              @click="handleBanUser(row)"
            >
              封禁
            </el-button>
            <el-button
              v-if="row.role !== 'ADMIN' && row.status === 'BANNED'"
              type="success"
              size="small"
              @click="handleUnbanUser(row)"
            >
              解封
            </el-button>
            <span v-if="row.role === 'ADMIN'" style="color: #909399;">管理员</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, CircleCheck, CircleClose, Connection, Search } from '@element-plus/icons-vue'
import api from '../api/request'

const statistics = ref({
  totalUsers: 0,
  activeUsers: 0,
  bannedUsers: 0,
  onlineUsers: 0
})

const users = ref([])
const searchKeyword = ref('')

const filteredUsers = computed(() => {
  if (!searchKeyword.value) {
    return users.value
  }
  return users.value.filter(user =>
    user.nickname.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const loadStatistics = async () => {
  try {
    const response = await api.get('/admin/statistics')
    statistics.value = response.data
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

const loadUsers = async () => {
  try {
    const response = await api.get('/admin/users')
    users.value = response.data
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  }
}

const handleSearch = () => {
  // 搜索由 computed 自动处理
}

const handleBanUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要封禁用户 ${user.nickname} 吗？`,
      '确认封禁',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.post(`/admin/users/${user.id}/ban`)
    ElMessage.success('用户已封禁')
    await loadUsers()
    await loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('封禁失败')
    }
  }
}

const handleUnbanUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要解封用户 ${user.nickname} 吗？`,
      '确认解封',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await api.post(`/admin/users/${user.id}/unban`)
    ElMessage.success('用户已解封')
    await loadUsers()
    await loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('解封失败')
    }
  }
}

onMounted(() => {
  loadStatistics()
  loadUsers()
})
</script>

<style scoped>
.admin-container {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.admin-header {
  margin-bottom: 20px;
}

.admin-header h2 {
  margin: 0;
  color: #333;
}

.stats-cards {
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
  width: 50px;
  height: 50px;
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
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.user-list-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
