<template>
  <div class="page-container">
    <div class="page-header">
      <h2>人员</h2>
      <el-input
        v-model="searchText"
        placeholder="搜索人员..."
        :prefix-icon="Search"
        size="default"
        clearable
        class="search-input"
        @input="handleSearch"
      />
    </div>

    <div class="people-list" v-loading="loading">
      <div v-if="sortedUsers.length === 0 && !loading" class="empty-state">
        <el-empty description="未找到用户" />
      </div>
      <div v-for="user in sortedUsers" :key="user.id" class="person-card">
        <el-avatar :size="48" :style="{ borderRadius: '50%' }">
          {{ (user.nickname || '').charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="person-info" @click="startChat(user)">
          <div class="person-name">
            {{ user.nickname }}
            <el-tag v-if="isSelf(user)" size="small" type="info" class="self-tag">我</el-tag>
          </div>
          <div class="person-meta">
            <span class="person-email">{{ user.email }}</span>
            <span v-if="user.department" class="person-dept">· {{ user.department }}</span>
          </div>
        </div>
        <el-button v-if="!isSelf(user)" type="primary" size="small" round @click.stop="startChat(user)">
          <el-icon><ChatDotRound /></el-icon> 发消息
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { userApi } from '../api'
import { Search, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const searchText = ref('')
const users = ref([])
const loading = ref(false)

const sortedUsers = computed(() => {
  const list = [...users.value]
  const selfIdx = list.findIndex(u => u.id === userStore.user?.id)
  if (selfIdx > 0) {
    const self = list.splice(selfIdx, 1)[0]
    list.unshift(self)
  }
  return list
})

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.searchUsers(searchText.value)
    users.value = res.data || []
  } catch {
    ElMessage.error('加载用户列表失败')
    users.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadUsers()
}

const isSelf = (user) => {
  return user.id === userStore.user?.id
}

const startChat = (user) => {
  if (isSelf(user)) return
  router.push(`/chat?with=${user.id}&name=${encodeURIComponent(user.nickname)}`)
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.page-container {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  background: #fff;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

.search-input {
  width: 320px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 22px;
  background: #F4F4F5;
  box-shadow: none;
  border: none;
}

.search-input :deep(.el-input__wrapper:focus-within) {
  background: #fff;
  box-shadow: 0 0 0 2px #3390EC;
}

.people-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.person-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.person-card:hover {
  background-color: #F8FAFC;
}

.person-info {
  flex: 1;
  min-width: 0;
}

.person-name {
  font-weight: 600;
  font-size: 15px;
  color: #222;
}

.person-meta {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.empty-state {
  padding: 40px 0;
}
</style>
