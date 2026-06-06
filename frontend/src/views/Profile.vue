<template>
  <div class="page-container">
    <div class="profile-card">
      <div class="profile-header">
        <el-avatar :size="80" :src="form.avatar" class="profile-avatar">
          {{ (form.nickname || 'U').charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="profile-header-info">
          <div class="profile-name">{{ form.nickname }}</div>
          <div class="profile-email">{{ form.email }}</div>
        </div>
      </div>

      <div class="profile-form">
        <div class="form-section">
          <div class="section-title">个人信息</div>
          <div class="form-grid">
            <div class="form-field">
              <label>昵称</label>
              <el-input v-model="form.nickname" size="large" />
            </div>
            <div class="form-field">
              <label>邮箱</label>
              <el-input v-model="form.email" disabled size="large" />
            </div>
            <div class="form-field">
              <label>部门</label>
              <el-input v-model="form.department" disabled size="large" />
            </div>
            <div class="form-field">
              <label>职位</label>
              <el-input v-model="form.position" disabled size="large" />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="section-title">个人备注</div>
          <el-input v-model="form.personalNote" type="textarea" :rows="3" size="large" placeholder="写点什么..." />
        </div>

        <div class="form-actions">
          <el-button type="primary" size="large" @click="updateProfile" round>保存修改</el-button>
          <el-button size="large" @click="showChangePasswordDialog = true" round>修改密码</el-button>
          <el-button type="danger" size="large" @click="handleLogout" round plain>退出登录</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="showChangePasswordDialog" title="修改密码" width="400px" :align-center="true">
      <el-form :model="passwordForm">
        <el-form-item>
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="输入新密码" size="large" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChangePasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="changePassword" :disabled="!passwordForm.newPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'
import { useUserStore } from '../store/user'
import api from '../api/request'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  nickname: '',
  email: '',
  avatar: '',
  department: '',
  position: '',
  personalNote: ''
})

const showChangePasswordDialog = ref(false)
const passwordForm = ref({ newPassword: '' })

onMounted(async () => {
  try {
    const res = await userApi.getCurrentUser()
    form.value = { ...form.value, ...res.data }
  } catch {}
})

const updateProfile = async () => {
  try {
    await userApi.updateProfile({
      nickname: form.value.nickname,
      avatar: form.value.avatar,
      personalNote: form.value.personalNote
    })
    userStore.setUser({ ...userStore.user, ...form.value })
    ElMessage.success('更新成功')
  } catch {
    ElMessage.error('更新失败')
  }
}

const changePassword = async () => {
  try {
    await api.post('/admin/change-password', { newPassword: passwordForm.value.newPassword })
    ElMessage.success('密码修改成功')
    showChangePasswordDialog.value = false
    passwordForm.value.newPassword = ''
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.page-container {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  background: #fff;
}

.profile-card {
  max-width: 600px;
  margin: 0 auto;
}

.profile-header {
  text-align: center;
  padding: 32px 0;
}

.profile-avatar {
  margin-bottom: 12px;
}

.profile-header-info {
  text-align: center;
}

.profile-name {
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

.profile-email {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.profile-form {
  padding: 0 4px;
}

.form-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #707579;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-field label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 500;
}

.form-field :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #F4F4F5;
  box-shadow: none;
  border: none;
}

.form-field :deep(.el-input__wrapper:focus-within) {
  background: #fff;
  box-shadow: 0 0 0 2px #3390EC;
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 32px;
}

.form-actions .el-button {
  width: 100%;
}
</style>
