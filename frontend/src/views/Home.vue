<template>
  <div class="app-layout">
    <!-- 左侧导航栏 - Telegram风格 -->
    <div class="app-sidebar">
      <div class="sidebar-top">
        <div
          v-for="item in menuItems"
          :key="item.path"
          class="sidebar-item"
          :class="{ active: currentPath === item.path }"
          @click="navigate(item.path)"
        >
          <el-icon :size="22">
            <component :is="item.icon" />
          </el-icon>
          <span class="sidebar-label">{{ item.label }}</span>
        </div>
      </div>

      <div class="sidebar-bottom">
        <div class="sidebar-item" @click="navigate('/profile')" :class="{ active: currentPath === '/profile' }">
          <el-avatar :size="36" :src="userStore.user?.avatar" class="sidebar-avatar">
            {{ (userStore.user?.nickname || 'U').charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="sidebar-label">我的</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="app-main">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { ChatDotRound, UserFilled, VideoCamera, Calendar, User } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentPath = computed(() => route.path)

const menuItems = [
  { path: '/chat', label: '聊天', icon: ChatDotRound },
  { path: '/teams', label: '团队', icon: UserFilled },
  { path: '/meetings', label: '会议', icon: VideoCamera },
  { path: '/calendar', label: '日历', icon: Calendar },
  { path: '/people', label: '人员', icon: User }
]

const navigate = (path) => {
  router.push(path)
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  background-color: #fff;
}

.app-sidebar {
  width: 72px;
  background: linear-gradient(180deg, #1E293B 0%, #0F172A 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  flex-shrink: 0;
}

.sidebar-top {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding-top: 4px;
}

.sidebar-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 8px;
}

.sidebar-item {
  width: 56px;
  height: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 12px;
  color: #94A3B8;
  transition: all 0.15s;
  gap: 2px;
}

.sidebar-item:hover {
  background-color: rgba(255, 255, 255, 0.08);
  color: #E2E8F0;
}

.sidebar-item.active {
  background-color: rgba(51, 144, 236, 0.3);
  color: #60A5FA;
}

.sidebar-label {
  font-size: 10px;
  font-weight: 500;
}

.sidebar-avatar {
  flex-shrink: 0;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}
</style>
