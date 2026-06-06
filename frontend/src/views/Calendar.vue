<template>
  <div class="calendar-page">
    <div class="calendar-layout">
      <!-- 左侧日历面板 -->
      <div class="calendar-panel">
        <!-- 头部月份切换 -->
        <div class="calendar-header">
          <div class="month-title">{{ year }}年 {{ month }}月</div>
          <div class="month-nav">
            <el-button circle size="small" @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
            <el-button size="small" @click="goToday">今天</el-button>
            <el-button circle size="small" @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
          </div>
        </div>

        <!-- 星期头部 -->
        <div class="weekday-row">
          <div v-for="d in weekDays" :key="d" class="weekday-cell">{{ d }}</div>
        </div>

        <!-- 日期网格 -->
        <div class="date-grid">
          <div
            v-for="(day, idx) in calendarDays"
            :key="idx"
            class="date-cell"
            :class="{
              'other-month': !day.currentMonth,
              'is-today': day.isToday,
              'is-selected': day.date === selectedDate
            }"
            @click="selectDate(day)"
          >
            <span class="date-num">{{ day.day }}</span>
            <div class="date-dots">
              <span v-if="todoDates.has(day.date)" class="todo-dot"></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧待办面板 -->
      <div class="todo-panel">
        <div class="todo-header">
          <span class="todo-date-title">{{ selectedDate }}</span>
          <span v-if="selectedDayOfWeek" class="todo-weekday">{{ selectedDayOfWeek }}</span>
        </div>

        <div class="todo-list">
          <div v-if="todos.length === 0" class="todo-empty">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#C7CDD5" stroke-width="1.5" stroke-linecap="round">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
            </div>
            <span>暂无待办事项</span>
          </div>

          <div v-for="todo in todos" :key="todo.id" class="todo-item">
            <div class="todo-content">
              <el-checkbox
                :model-value="todo.done"
                @change="handleToggleDone(todo)"
                class="todo-checkbox"
              />
              <span class="todo-text" :class="{ 'todo-done': todo.done }">{{ todo.content }}</span>
            </div>
            <el-button
              text
              type="danger"
              size="small"
              @click="handleDeleteTodo(todo)"
              :icon="Delete"
            />
          </div>
        </div>

        <div class="todo-input-area">
          <el-input
            v-model="newTodoContent"
            placeholder="添加待办事项..."
            @keyup.enter="handleAddTodo"
            maxlength="200"
            show-word-limit
          >
            <template #append>
              <el-button @click="handleAddTodo" :disabled="!newTodoContent.trim()" type="primary">
                添加
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight, Delete } from '@element-plus/icons-vue'
import { calendarApi } from '../api'
import { ElMessage } from 'element-plus'

const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const selectedDate = ref(formatDate(now))
const todos = ref([])
const newTodoContent = ref('')
const todoDates = ref(new Set())

const selectedDayOfWeek = computed(() => {
  const d = new Date(selectedDate.value)
  if (isNaN(d.getTime())) return ''
  return '周' + weekDays[d.getDay()]
})

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const calendarDays = computed(() => {
  const firstDay = new Date(year.value, month.value - 1, 1)
  const startDayOfWeek = firstDay.getDay()
  const daysInMonth = new Date(year.value, month.value, 0).getDate()
  const daysInPrevMonth = new Date(year.value, month.value - 1, 0).getDate()

  const today = formatDate(now)
  const days = []

  // 上月末尾
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const d = new Date(year.value, month.value - 2, daysInPrevMonth - i)
    days.push({ day: daysInPrevMonth - i, date: formatDate(d), currentMonth: false, isToday: false })
  }

  // 本月
  for (let i = 1; i <= daysInMonth; i++) {
    const d = new Date(year.value, month.value - 1, i)
    const dateStr = formatDate(d)
    days.push({ day: i, date: dateStr, currentMonth: true, isToday: dateStr === today })
  }

  // 下月
  const remaining = 7 - (days.length % 7)
  if (remaining < 7) {
    for (let i = 1; i <= remaining; i++) {
      const d = new Date(year.value, month.value, i)
      days.push({ day: i, date: formatDate(d), currentMonth: false, isToday: false })
    }
  }

  return days
})

function prevMonth() {
  if (month.value === 1) { year.value--; month.value = 12 }
  else month.value--
  loadMonthDots()
}

function nextMonth() {
  if (month.value === 12) { year.value++; month.value = 1 }
  else month.value++
  loadMonthDots()
}

function goToday() {
  year.value = now.getFullYear()
  month.value = now.getMonth() + 1
  selectDate({ date: formatDate(now) })
}

async function selectDate(day) {
  if (!day.currentMonth) return
  selectedDate.value = day.date
  await loadTodos()
}

async function loadTodos() {
  try {
    const res = await calendarApi.getTodos(selectedDate.value)
    todos.value = Array.isArray(res) ? res : []
  } catch {
    todos.value = []
  }
}

async function loadMonthDots() {
  try {
    const res = await calendarApi.getMonth(year.value, month.value)
    const set = new Set()
    const data = res || {}
    for (const [date, items] of Object.entries(data)) {
      if (Array.isArray(items) && items.length > 0) {
        set.add(date)
      }
    }
    todoDates.value = set
  } catch {
    todoDates.value = new Set()
  }
}

async function handleAddTodo() {
  const content = newTodoContent.value.trim()
  if (!content) return
  try {
    const res = await calendarApi.addTodo(selectedDate.value, content)
    todos.value.push(res)
    newTodoContent.value = ''
    todoDates.value.add(selectedDate.value)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '添加失败')
  }
}

async function handleDeleteTodo(todo) {
  try {
    await calendarApi.deleteTodo(todo.id)
    todos.value = todos.value.filter(t => t.id !== todo.id)
    if (todos.value.length === 0) {
      todoDates.value.delete(selectedDate.value)
      todoDates.value = new Set(todoDates.value)
    }
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

async function handleToggleDone(todo) {
  try {
    await calendarApi.toggleTodo(todo.id)
    todo.done = !todo.done
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  selectDate({ date: formatDate(now), currentMonth: true })
  loadMonthDots()
})

watch([year, month], () => {
  loadMonthDots()
})
</script>

<style scoped>
.calendar-page {
  height: 100%;
  background: #F5F5F5;
  display: flex;
  flex-direction: column;
}

.calendar-layout {
  display: flex;
  height: 100%;
  gap: 0;
}

/* 左侧日历面板 */
.calendar-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  background: #fff;
  border-right: 1px solid #E8ECEF;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.month-title {
  font-size: 20px;
  font-weight: 700;
  color: #1A1A1A;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 4px;
}

.weekday-cell {
  font-size: 12px;
  font-weight: 600;
  color: #8A8E96;
  padding: 8px 0;
}

.date-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-template-rows: repeat(6, 1fr);
  flex: 1;
  gap: 2px;
}

.date-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 4px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.15s;
  min-height: 48px;
}

.date-cell:hover {
  background: #F0F3F5;
}

.date-cell.other-month {
  cursor: default;
  visibility: hidden;
}

.date-cell.is-today {
  background: #E3F2FD;
}

.date-cell.is-today .date-num {
  color: #1976D2;
  font-weight: 700;
}

.date-cell.is-selected {
  background: #1976D2;
}

.date-cell.is-selected .date-num {
  color: #fff;
  font-weight: 700;
}

.date-num {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.date-dots {
  display: flex;
  gap: 2px;
  margin-top: 2px;
}

.todo-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1976D2;
}

.date-cell.is-selected .todo-dot {
  background: #fff;
}

/* 右侧待办面板 */
.todo-panel {
  width: 360px;
  display: flex;
  flex-direction: column;
  background: #fff;
  flex-shrink: 0;
}

.todo-header {
  padding: 24px 20px 16px;
  border-bottom: 1px solid #E8ECEF;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.todo-date-title {
  font-size: 18px;
  font-weight: 700;
  color: #1A1A1A;
}

.todo-weekday {
  font-size: 13px;
  color: #8A8E96;
}

.todo-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.todo-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;
  color: #A0A5AD;
  font-size: 14px;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  background: #F8F9FA;
  margin-bottom: 8px;
  transition: background 0.15s;
}

.todo-item:hover {
  background: #F0F3F5;
}

.todo-content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.todo-text {
  font-size: 14px;
  color: #333;
  word-break: break-all;
}

.todo-done {
  text-decoration: line-through;
  color: #A0A5AD;
}

.todo-checkbox {
  flex-shrink: 0;
}

.todo-input-area {
  padding: 16px 20px 24px;
  border-top: 1px solid #E8ECEF;
}
</style>
