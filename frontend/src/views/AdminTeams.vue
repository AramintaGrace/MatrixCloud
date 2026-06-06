<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>团队信息</h2>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索团队名称"
        style="width: 400px;"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 团队列表 -->
    <el-card class="teams-list-card">
      <el-table :data="filteredTeams" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="团队名称" width="200" />
        <el-table-column prop="creatorId" label="创建者ID" width="120" />
        <el-table-column prop="memberCount" label="成员数" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="viewTeamChat(row)"
            >
              查看聊天
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 聊天记录对话框 -->
    <el-dialog
      v-model="chatDialogVisible"
      :title="`${currentTeam?.name} - 聊天记录`"
      width="70%"
      top="5vh"
    >
      <div class="chat-history">
        <div
          v-for="message in chatMessages"
          :key="message.id"
          class="message-item"
        >
          <div class="message-header">
            <span class="sender-name">{{ message.senderName }}</span>
            <span class="message-time">{{ message.createdAt }}</span>
          </div>
          <div class="message-content">{{ message.content }}</div>
        </div>
        <div v-if="chatMessages.length === 0" class="empty-message">
          暂无聊天记录
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '../api/request'

const searchKeyword = ref('')
const teams = ref([])
const chatDialogVisible = ref(false)
const currentTeam = ref(null)
const chatMessages = ref([])

const filteredTeams = computed(() => {
  if (!searchKeyword.value) {
    return teams.value
  }
  return teams.value.filter(team =>
    team.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const loadTeams = async () => {
  try {
    const response = await api.get('/admin/teams')
    teams.value = response.data
  } catch (error) {
    ElMessage.error('加载团队列表失败')
  }
}

const viewTeamChat = async (team) => {
  currentTeam.value = team
  chatDialogVisible.value = true

  try {
    const response = await api.get(`/admin/teams/${team.id}/messages`)
    chatMessages.value = response.data
  } catch (error) {
    ElMessage.error('加载聊天记录失败')
    chatMessages.value = []
  }
}

onMounted(() => {
  loadTeams()
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

.chat-history {
  max-height: 60vh;
  overflow-y: auto;
  padding: 10px;
}

.message-item {
  margin-bottom: 15px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.sender-name {
  font-weight: bold;
  color: #333;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-content {
  color: #666;
  line-height: 1.5;
}

.empty-message {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
