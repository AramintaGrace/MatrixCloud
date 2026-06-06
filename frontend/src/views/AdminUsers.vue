<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>用户管理</h2>
      <el-button type="primary" icon="Plus" @click="openCreateDialog">注册用户</el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户昵称或邮箱"
        style="width: 400px;"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-card class="user-list-card">
      <el-table :data="filteredUsers" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="position" label="职位" width="120" />
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
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
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
            <el-button
              v-if="row.role !== 'ADMIN'"
              type="danger"
              size="small"
              plain
              @click="handleDeleteUser(row)"
            >
              删除
            </el-button>
            <span v-if="row.role === 'ADMIN'" style="color: #909399;">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 注册用户对话框 -->
    <el-dialog v-model="showCreateDialog" title="注册用户" width="480px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="createForm.department" placeholder="请输入部门（选填）" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="createForm.position" placeholder="请输入职位（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateUser" :loading="creating">注册</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="480px">
      <el-form :model="editForm" ref="editFormRef" label-width="80px">
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="editForm.password" type="password" placeholder="留空则不修改" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="editForm.department" placeholder="部门" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="editForm.position" placeholder="职位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleEditUser" :loading="editing">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '../api/request'

const users = ref([])
const searchKeyword = ref('')
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const creating = ref(false)
const editing = ref(false)
const createFormRef = ref(null)
const editFormRef = ref(null)
const editingUserId = ref(null)

const createForm = ref({
  email: '',
  password: '',
  nickname: '',
  department: '',
  position: ''
})

const editForm = ref({
  email: '',
  password: '',
  nickname: '',
  department: '',
  position: ''
})

const createRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ]
}

const filteredUsers = computed(() => {
  if (!searchKeyword.value) return users.value
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

const openCreateDialog = () => {
  createForm.value = { email: '', password: '', nickname: '', department: '', position: '' }
  if (createFormRef.value) createFormRef.value.resetFields()
  showCreateDialog.value = true
}

const handleCreateUser = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      await api.post('/admin/users', {
        email: createForm.value.email,
        password: createForm.value.password,
        nickname: createForm.value.nickname,
        department: createForm.value.department,
        position: createForm.value.position
      })
      ElMessage.success('用户创建成功')
      showCreateDialog.value = false
      await loadUsers()
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '创建用户失败')
    } finally {
      creating.value = false
    }
  })
}

const openEditDialog = (user) => {
  editingUserId.value = user.id
  editForm.value = {
    email: user.email,
    password: '',
    nickname: user.nickname,
    department: user.department || '',
    position: user.position || ''
  }
  showEditDialog.value = true
}

const handleEditUser = async () => {
  editing.value = true
  try {
    await api.put(`/admin/users/${editingUserId.value}`, {
      email: editForm.value.email,
      password: editForm.value.password,
      nickname: editForm.value.nickname,
      department: editForm.value.department,
      position: editForm.value.position
    })
    ElMessage.success('用户信息已更新')
    showEditDialog.value = false
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更新失败')
  } finally {
    editing.value = false
  }
}

const handleBanUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要封禁用户 ${user.nickname} 吗？`,
      '确认封禁',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await api.post(`/admin/users/${user.id}/ban`)
    ElMessage.success('用户已封禁')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('封禁失败')
  }
}

const handleUnbanUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要解封用户 ${user.nickname} 吗？`,
      '确认解封',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    await api.post(`/admin/users/${user.id}/unban`)
    ElMessage.success('用户已解封')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('解封失败')
  }
}

const handleDeleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 ${user.nickname} (${user.email}) 吗？此操作不可撤销。`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await api.delete(`/admin/users/${user.id}`)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '删除失败')
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-header h2 {
  margin: 0;
  color: #333;
}

.search-bar {
  margin-bottom: 20px;
}
</style>
