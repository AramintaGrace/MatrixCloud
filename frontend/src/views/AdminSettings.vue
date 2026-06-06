<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>管理员设置</h2>
    </div>

    <el-card class="settings-card">
      <template #header>
        <span>修改密码</span>
      </template>
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="120px"
        style="max-width: 500px;"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleChangePassword">
            修改密码
          </el-button>
          <el-button @click="resetPasswordForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="settings-card" style="margin-top: 20px;">
      <template #header>
        <span>修改邮箱</span>
      </template>
      <el-form
        ref="emailFormRef"
        :model="emailForm"
        :rules="emailRules"
        label-width="120px"
        style="max-width: 500px;"
      >
        <el-form-item label="当前邮箱">
          <el-input :value="currentEmail" disabled />
        </el-form-item>
        <el-form-item label="新邮箱" prop="newEmail">
          <el-input
            v-model="emailForm.newEmail"
            placeholder="请输入新邮箱"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="password">
          <el-input
            v-model="emailForm.password"
            type="password"
            placeholder="请输入密码以确认修改"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleChangeEmail">
            修改邮箱
          </el-button>
          <el-button @click="resetEmailForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import api from '../api/request'

const userStore = useUserStore()
const currentEmail = ref(userStore.user?.email || '')

const passwordFormRef = ref(null)
const emailFormRef = ref(null)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const emailForm = reactive({
  newEmail: '',
  password: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const emailRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()

    await api.post('/admin/change-password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码修改成功')
    resetPasswordForm()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.response?.data?.message || '密码修改失败')
    }
  }
}

const handleChangeEmail = async () => {
  try {
    await emailFormRef.value.validate()

    await api.post('/admin/change-email', {
      newEmail: emailForm.newEmail,
      password: emailForm.password
    })

    ElMessage.success('邮箱修改成功')
    currentEmail.value = emailForm.newEmail
    userStore.user.email = emailForm.newEmail
    resetEmailForm()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.response?.data?.message || '邮箱修改失败')
    }
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const resetEmailForm = () => {
  emailForm.newEmail = ''
  emailForm.password = ''
  emailFormRef.value?.clearValidate()
}
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

.settings-card {
  max-width: 800px;
}
</style>
