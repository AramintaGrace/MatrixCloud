<template>
  <div class="admin-layout">
    <!-- 左侧导航栏 -->
    <div class="admin-sidebar">
      <div class="admin-logo">
        <h3>MatrixCloud</h3>
        <p>管理后台</p>
      </div>

      <div
        v-for="item in menuItems"
        :key="item.path"
        class="admin-menu-item"
        :class="{ active: currentPath === item.path }"
        @click="navigate(item.path)"
      >
        <el-icon :size="20">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </div>

      <div style="flex: 1;"></div>

      <div
        class="admin-menu-item"
        :class="{ active: currentPath === '/admin/settings' }"
        @click="navigate('/admin/settings')"
      >
        <el-icon :size="20">
          <Setting />
        </el-icon>
        <span>设置</span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="admin-main">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { DataAnalysis, User, UserFilled, Setting } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const currentPath = computed(() => route.path)

const menuItems = [
  { path: '/admin/dashboard', label: '系统信息', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/teams', label: '团队信息', icon: UserFilled }
]

const navigate = (path) => {
  router.push(path)
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #F3F2F1;
}

.admin-sidebar {
  width: 200px;
  background-color: #464775;
  display: flex;
  flex-direction: column;
  padding: 20px 0;
}

.admin-logo {
  padding: 0 20px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 20px;
  text-align: center;
}

.admin-logo h3 {
  margin: 0;
  color: #FFFFFF;
  font-size: 18px;
}

.admin-logo p {
  margin: 5px 0 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.admin-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  color: #FFFFFF;
  transition: background-color 0.2s;
}

.admin-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.admin-menu-item.active {
  background-color: #6264A7;
  border-left: 3px solid #FFFFFF;
}

.admin-menu-item span {
  font-size: 14px;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
</style>
