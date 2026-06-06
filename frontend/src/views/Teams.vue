<template>
  <div class="teams-content">
    <!-- 左侧团队列表 - Telegram风格 -->
    <div class="teams-sidebar" :style="{ width: sidebarWidth + 'px' }">
      <div class="sidebar-header">
        <div class="sidebar-search">
          <el-input
            v-model="searchText"
            placeholder="搜索聊天内容"
            :prefix-icon="Search"
            size="default"
            clearable
            @keyup.enter="handleSearch"
            @clear="clearSearch"
          />
        </div>
      </div>
      <div class="team-list">
        <div
          v-for="team in filteredTeams"
          :key="team.id"
          class="team-item"
          :class="{ active: selectedTeam?.id === team.id }"
          @click="selectTeam(team)"
        >
          <el-avatar :size="48" :style="{ backgroundColor: team.color, borderRadius: '50%' }" class="team-avatar">
            {{ team.name.charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="team-info">
            <div class="team-top">
              <span class="team-name">{{ team.name }}</span>
            </div>
            <div class="team-bottom">
              <span class="team-preview">{{ team.lastMessage || team.memberCount + ' 位成员' }}</span>
            </div>
          </div>
        </div>
        <div v-if="filteredTeams.length === 0 && teams.length > 0" style="padding: 16px; text-align: center; color: #707579;">
          未找到匹配的团队
        </div>
      </div>
      <div class="sidebar-footer">
        <el-button text @click="showCreateDialog = true" class="create-team-btn">
          <el-icon><Plus /></el-icon> 创建团队
        </el-button>
      </div>
    </div>

    <div class="resize-handle" @mousedown="startResize"></div>

    <!-- 右侧聊天区域 - Telegram风格 -->
    <div class="teams-chat" v-if="selectedTeam">
      <div class="chat-header">
        <el-avatar :size="40" :style="{ backgroundColor: selectedTeam.color, borderRadius: '50%' }">
          {{ selectedTeam.name.charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="chat-header-info">
          <div class="chat-header-name">{{ selectedTeam.name }}</div>
          <div class="chat-header-subtitle">{{ members.length }} 位成员</div>
        </div>
        <div class="chat-header-actions">
          <el-select v-model="msgTypeFilter" size="small" class="filter-select" @change="onFilterChange">
            <el-option label="全部" value="all" />
            <el-option label="媒体" value="media" />
            <el-option label="文件" value="file" />
          </el-select>
          <el-tooltip content="搜索消息" placement="bottom">
            <el-button :icon="Search" circle @click="showMessageSearch = !showMessageSearch" />
          </el-tooltip>
          <el-tooltip content="邀请成员" placement="bottom">
            <el-button :icon="Plus" circle @click="showInviteDialog = true" />
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleMenuCommand">
            <el-button :icon="More" circle />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="members">查看成员</el-dropdown-item>
                <el-dropdown-item v-if="isTeamCreator" command="disband" divided style="color: #e53935;">
                  解散团队
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 消息搜索栏 -->
      <div v-if="showMessageSearch" class="message-search-bar">
        <el-input
          v-model="msgSearchKeyword"
          placeholder="搜索消息..."
          :prefix-icon="Search"
          size="default"
          clearable
          @input="debounceSearchMessages"
        />
        <el-button v-if="isSearching" text size="small" @click="clearMessageSearch">取消搜索</el-button>
      </div>

      <!-- 聊天消息区域 -->
      <div class="chat-messages" ref="messagesContainer" @scroll="onScroll">
        <!-- 置顶消息 -->
        <div v-if="pinnedMessages.length > 0 && !isSearching" class="pinned-section">
          <div class="pinned-header" @click="showPinned = !showPinned">
            <el-icon :size="16"><ArrowUp /></el-icon>
            <span>置顶消息 {{ pinnedMessages.length }}</span>
            <el-icon class="pinned-chevron" :class="{ rotated: showPinned }"><ArrowDown /></el-icon>
          </div>
          <div v-show="showPinned" class="pinned-list">
            <div v-for="pm in pinnedMessages" :key="pm.id" class="pinned-item">
              <div class="pinned-item-content">
                <span class="pinned-item-author">{{ pm.senderName }}</span>
                <span class="pinned-item-text">{{ pm.content || pm.fileName || '[文件]' }}</span>
              </div>
              <el-button text size="small" @click="handleUnpinMsg(pm)" class="pinned-unpin-btn">
                <el-icon :size="14"><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="displayedMessages.length === 0 && !isSearching" class="empty-chat">
          <div class="empty-chat-content">
            <div class="empty-chat-icon">{{ selectedTeam.name.charAt(0).toUpperCase() }}</div>
            <div class="empty-chat-title">{{ selectedTeam.name }}</div>
            <div class="empty-chat-text">还没有消息，发送第一条吧</div>
          </div>
        </div>
        <div v-if="isSearching && searchResults.length === 0 && msgSearchKeyword" class="empty-chat">
          <el-empty description="未找到相关消息" />
        </div>

        <div v-for="(msg, idx) in displayedMessages" :key="msg.id">
          <!-- 日期分隔符 -->
          <div v-if="showDateSeparator(idx, msg)" class="date-separator">
            <span>{{ formatDate(msg.createdAt) }}</span>
          </div>
          <div
            class="message"
            :class="{ 'message-self': msg.senderId === userStore.user?.id }"
          >
            <el-avatar v-if="msg.senderId !== userStore.user?.id" :size="36" class="msg-avatar">
              {{ (msg.senderName || '').charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="message-body" :class="{ 'message-body-self': msg.senderId === userStore.user?.id }">
              <div class="message-bubble" :class="{
                'bubble-self': msg.senderId === userStore.user?.id,
                'bubble-other': msg.senderId !== userStore.user?.id,
                'bubble-image': msg.messageType === 'IMAGE',
                'bubble-video': msg.messageType === 'VIDEO' || msg.messageType === 'AUDIO',
                'bubble-file': msg.messageType === 'FILE'
              }">
                <!-- 文本消息 -->
                <div v-if="msg.messageType === 'TEXT' || msg.messageType === 'EMOJI'" class="msg-text">
                  {{ msg.content }}
                </div>

                <!-- 图片消息 -->
                <div v-else-if="msg.messageType === 'IMAGE'" class="msg-image-wrapper" @click="previewImage(msg)">
                  <img :src="getMediaUrl(msg)" :alt="msg.fileName" class="msg-image" loading="lazy" />
                </div>

                <!-- 视频消息 -->
                <div v-else-if="msg.messageType === 'VIDEO'" class="msg-media-wrapper">
                  <video :src="getMediaUrl(msg)" controls class="msg-video" preload="metadata">
                    您的浏览器不支持视频播放
                  </video>
                </div>

                <!-- 音频消息 -->
                <div v-else-if="msg.messageType === 'AUDIO'" class="msg-media-wrapper">
                  <div class="msg-audio-wrapper" @click="togglePlayAudio(msg)">
                    <el-icon :size="22" class="audio-play-icon" :class="{ 'is-playing': playingAudioId === msg.id }">
                      <VideoPlay />
                    </el-icon>
                    <div class="audio-waveform">
                      <span v-for="h in getWaveHeights(msg.id)" :key="h.i" class="audio-bar" :style="{ height: h.h + 'px' }"></span>
                    </div>
                    <span class="audio-duration">{{ formatDuration(msg.fileSize) }}</span>
                  </div>
                </div>

                <!-- 普通文件消息 -->
                <div v-else-if="msg.messageType === 'FILE'" class="msg-file-wrapper" @click="downloadFile(msg)">
                  <div class="file-icon-box">
                    <el-icon :size="28"><Document /></el-icon>
                  </div>
                  <div class="file-info">
                    <div class="file-name">{{ msg.fileName || '未知文件' }}</div>
                    <div class="file-size">{{ formatFileSize(msg.fileSize) }}</div>
                  </div>
                  <el-icon :size="20" class="file-download"><Download /></el-icon>
                </div>
              </div>
              <div class="message-time" :class="{ 'time-right': msg.senderId === userStore.user?.id }">
                <el-button
                  v-if="isCurrentUserAdmin && !isSearching && (msg.messageType === 'TEXT' || msg.messageType === 'EMOJI' || msg.messageType === 'FILE')"
                  text
                  size="small"
                  class="pin-btn"
                  @click="handlePinMsg(msg)"
                >
                  <el-icon :size="12"><ArrowUp /></el-icon>
                </el-button>
                {{ msg.time }}
              </div>
            </div>
          </div>
        </div>
        <div ref="messagesEnd"></div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="chat-input-toolbar">
          <el-upload
            ref="fileUploadRef"
            :show-file-list="false"
            :http-request="handleFileUpload"
            accept="*"
          >
            <el-tooltip content="发送文件" placement="top">
              <el-button :icon="FolderOpened" circle size="default" class="toolbar-btn" />
            </el-tooltip>
          </el-upload>
          <el-tooltip content="语音消息" placement="top">
            <el-button
              :icon="Microphone"
              circle
              size="default"
              class="toolbar-btn"
              :class="{ 'recording-active': isRecording }"
              @click="toggleRecording"
            />
          </el-tooltip>
          <EmojiPicker @emoji-select="insertEmoji" />
        </div>
        <div v-if="isRecording" class="recording-bar">
          <span class="recording-dot"></span>
          <span class="recording-timer">{{ recordingTimer }}s</span>
          <span class="recording-hint">正在录音，再次点击停止</span>
          <el-button size="small" type="danger" @click="stopRecording" round>停止并发送</el-button>
        </div>
        <div class="chat-input-row" v-if="!isRecording">
          <el-input
            v-model="messageText"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="输入消息..."
            class="msg-input"
            @keydown.enter.exact.prevent="sendChatMessage"
            resize="none"
          />
          <el-button
            type="primary"
            :disabled="!messageText.trim()"
            @click="sendChatMessage"
            class="send-btn"
            :icon="Promotion"
            circle
          />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-main">
      <div class="empty-main-content">
        <el-icon :size="64" color="#C4C9CC"><ChatDotRound /></el-icon>
        <div class="empty-main-text">选择一个团队开始聊天</div>
      </div>
    </div>

    <!-- 成员列表侧边栏 -->
    <el-drawer v-model="showMembersDrawer" title="团队成员" direction="rtl" size="340px">
      <div class="members-list">
        <div v-for="member in members" :key="member.id" class="member-item">
          <el-avatar :size="44" :style="{ borderRadius: '50%' }">
            {{ (member.nickname || '').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="member-info">
            <div class="member-name">
              {{ member.nickname }}
              <el-tag v-if="selectedTeam?.creatorId === member.userId" size="small" type="danger" class="creator-tag">创建者</el-tag>
            </div>
            <div class="member-email">{{ member.email }}</div>
          </div>
          <div v-if="isCurrentUserAdmin && selectedTeam?.creatorId !== member.userId" class="member-actions">
            <el-dropdown trigger="click">
              <el-button text size="small" class="member-more-btn"><el-icon><More /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="member.role === 'MEMBER'" @click="handleSetAdmin(member)">
                    设为管理员
                  </el-dropdown-item>
                  <el-dropdown-item v-else @click="handleDemoteAdmin(member)">
                    取消管理员
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleRemoveMember(member)" divided style="color: #e53935;">
                    移出团队
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <el-tag v-else-if="member.role === 'ADMIN'" type="warning" size="small">管理员</el-tag>
          <el-tag v-else size="small" type="info">成员</el-tag>
        </div>
      </div>
    </el-drawer>

    <!-- 创建团队对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建团队" width="380px" :align-center="true">
      <el-form :model="createForm" @submit.prevent="createTeam">
        <el-form-item>
          <el-input v-model="createForm.name" placeholder="团队名称" size="large" @keyup.enter="createTeam" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createTeam" :disabled="!createForm.name.trim()">创建</el-button>
      </template>
    </el-dialog>

    <!-- 邀请成员对话框 -->
    <el-dialog v-model="showInviteDialog" title="邀请成员" width="440px" :align-center="true">
      <el-input
        v-model="searchNickname"
        placeholder="搜索用户昵称..."
        :prefix-icon="Search"
        size="large"
        @input="searchUsers"
      />
      <div class="search-results" v-if="userSearchResults.length > 0">
        <div v-for="user in userSearchResults" :key="user.id" class="search-result-item">
          <el-avatar :size="40" :style="{ borderRadius: '50%' }">
            {{ (user.nickname || '').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="search-result-info">
            <div class="search-result-name">{{ user.nickname }}</div>
            <div class="search-result-email">{{ user.email }}</div>
          </div>
          <el-button size="small" type="primary" @click="inviteMember(user.id)" plain>邀请</el-button>
        </div>
      </div>
      <div v-if="searchNickname && userSearchResults.length === 0 && !searchingUsers" style="text-align: center; color: #999; padding: 20px;">
        未找到匹配的用户
      </div>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="showImageViewer" :align-center="true" width="auto" class="image-viewer-dialog">
      <img :src="previewImageUrl" :alt="previewImageName" class="preview-image-full" @click="showImageViewer = false" />
      <div class="preview-image-name">{{ previewImageName }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Plus, More, ChatDotRound, Promotion, FolderOpened,
  VideoPlay, Document, Download, ArrowUp, ArrowDown, Close, Microphone
} from '@element-plus/icons-vue'
import { teamApi, userApi, chatApi } from '../api'
import { useUserStore } from '../store/user'
import EmojiPicker from '../components/EmojiPicker.vue'

const userStore = useUserStore()
const selectedTeam = ref(null)
const showCreateDialog = ref(false)
const showInviteDialog = ref(false)
const showMembersDrawer = ref(false)
const showMessageSearch = ref(false)
const showImageViewer = ref(false)
const previewImageUrl = ref('')
const previewImageName = ref('')
const searchNickname = ref('')
const userSearchResults = ref([])
const searchingUsers = ref(false)
const members = ref([])
const teams = ref([])
const chatMessages = ref([])
const messageText = ref('')
const messagesContainer = ref(null)
const messagesEnd = ref(null)
const chatSessionId = ref(null)
const sidebarWidth = ref(320)
const searchText = ref('')
const msgSearchKeyword = ref('')
const isSearching = ref(false)
const searchResults = ref([])
const msgTypeFilter = ref('all')
const fileUploadRef = ref(null)
const pinnedMessages = ref([])
const showPinned = ref(true)
const isRecording = ref(false)
const recordingTimer = ref(0)
const playingAudioId = ref(null)
let mediaRecorder = null
let recordingInterval = null
let audioChunks = []
let audioEl = null

let ws = null
let wsReconnectTimer = null
let isResizing = false
let searchTimer = null

const createForm = reactive({ name: '' })

// Computed
const isTeamCreator = computed(() => {
  if (!selectedTeam.value) return false
  return selectedTeam.value.creatorId === userStore.user?.id
})

const isCurrentUserAdmin = computed(() => {
  if (!selectedTeam.value) return false
  if (selectedTeam.value.creatorId === userStore.user?.id) return true
  return selectedTeam.value.role === 'ADMIN'
})

const filteredTeams = computed(() => {
  if (!searchText.value) return teams.value
  const kw = searchText.value.toLowerCase()
  return teams.value.filter(t => t.name.toLowerCase().includes(kw))
})

const displayedMessages = computed(() => {
  if (isSearching.value) return searchResults.value
  let msgs = chatMessages.value
  if (msgTypeFilter.value === 'media') {
    msgs = msgs.filter(m => ['IMAGE', 'VIDEO', 'AUDIO'].includes(m.messageType))
  } else if (msgTypeFilter.value === 'file') {
    msgs = msgs.filter(m => ['FILE'].includes(m.messageType))
  }
  return msgs
})

// Resize
const startResize = (e) => {
  isResizing = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
}

const onResize = (e) => {
  if (!isResizing) return
  sidebarWidth.value = Math.min(500, Math.max(220, e.clientX))
}

const stopResize = () => {
  isResizing = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

// Teams
const loadTeams = async () => {
  try {
    const res = await teamApi.getMyTeams()
    teams.value = res.data.map(item => ({
      id: item.teamId,
      name: item.teamName,
      creatorId: item.creatorId,
      memberCount: item.memberCount,
      role: item.role,
      color: getColor(item.teamName),
      lastMessage: item.lastMessage || ''
    }))
  } catch {
    // silently fail
  }
}

const getColor = (name) => {
  const colors = ['#4E9AF1', '#50C878', '#FF6B6B', '#FFA726', '#AB47BC', '#26C6DA', '#EC407A', '#7E57C2', '#66BB6A']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

const selectTeam = async (team) => {
  if (selectedTeam.value?.id === team.id) return
  selectedTeam.value = team
  chatMessages.value = []
  isSearching.value = false
  msgSearchKeyword.value = ''
  searchResults.value = []
  userSearchResults.value = []
  msgTypeFilter.value = 'all'
  showMessageSearch.value = false
  await loadMembers()
  await loadChatSession()
  await loadMessages()
  await loadPinnedMessages()
}

const loadMembers = async () => {
  if (!selectedTeam.value) return
  try {
    const res = await teamApi.getTeamMembers(selectedTeam.value.id)
    members.value = res.data
  } catch { /* ignore */ }
}

const loadChatSession = async () => {
  try {
    const res = await chatApi.getTeamSession(selectedTeam.value.id)
    chatSessionId.value = res.data.sessionId
  } catch { ElMessage.error('加载聊天室失败') }
}

const loadMessages = async () => {
  if (!chatSessionId.value) return
  try {
    const res = await chatApi.getMessages(chatSessionId.value)
    chatMessages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch { /* ignore */ }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesEnd.value) {
      messagesEnd.value.scrollIntoView({ behavior: 'smooth' })
    }
  })
}

const onScroll = () => {
  // could add infinite scroll pagination here
}

// Chat
const sendChatMessage = async () => {
  const content = messageText.value.trim()
  if (!content || !chatSessionId.value) return

  try {
    // Detect if content is mostly emoji
    const emojiRegex = /[\p{Emoji_Presentation}\p{Extended_Pictographic}]/gu
    const isEmoji = content.replace(emojiRegex, '').trim().length === 0 && content.length <= 10
    const msgType = isEmoji ? 'EMOJI' : 'TEXT'

    await chatApi.sendMessage(chatSessionId.value, content, msgType)
    messageText.value = ''
  } catch {
    ElMessage.error('发送消息失败')
  }
}

const insertEmoji = (emoji) => {
  messageText.value += emoji
}

// Voice recording
const toggleRecording = async () => {
  if (isRecording.value) { await stopRecording() } else { await startRecording() }
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus' : 'audio/webm'
    mediaRecorder = new MediaRecorder(stream, { mimeType })
    mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunks.push(e.data) }
    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop())
      if (audioChunks.length > 0 && chatSessionId.value) {
        const blob = new Blob(audioChunks, { type: 'audio/webm' })
        const file = new File([blob], `voice_${Date.now()}.webm`, { type: 'audio/webm' })
        try { await chatApi.uploadFile(chatSessionId.value, file) } catch { ElMessage.error('语音发送失败') }
      }
    }
    mediaRecorder.start()
    isRecording.value = true
    recordingTimer.value = 0
    recordingInterval = setInterval(() => { recordingTimer.value++ }, 1000)
  } catch { ElMessage.error('无法访问麦克风') }
}

const stopRecording = async () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop()
  isRecording.value = false
  if (recordingInterval) { clearInterval(recordingInterval); recordingInterval = null }
}

const togglePlayAudio = (msg) => {
  if (playingAudioId.value === msg.id) {
    if (audioEl) { audioEl.pause(); audioEl = null }
    playingAudioId.value = null; return
  }
  if (audioEl) audioEl.pause()
  audioEl = new Audio(getMediaUrl(msg))
  audioEl.onended = () => { playingAudioId.value = null; audioEl = null }
  audioEl.onerror = () => { playingAudioId.value = null; audioEl = null }
  audioEl.play()
  playingAudioId.value = msg.id
}

const getWaveHeights = (msgId) => {
  let hash = 0
  const s = String(msgId)
  for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash)
  return Array.from({ length: 12 }, (_, i) => ({ i, h: ((hash * (i + 7)) % 13) + 8 }))
}

const getMediaUrl = (msg) => {
  if (['IMAGE', 'VIDEO', 'AUDIO'].includes(msg.messageType)) {
    return `/api/chat/files/${encodeURIComponent(msg.content)}`
  }
  return msg.fileUrl || ''
}

const formatDuration = (bytes) => {
  if (!bytes) return '0:00'
  const secs = Math.round(bytes / 2000)
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

// File upload
const handleFileUpload = async (options) => {
  if (!chatSessionId.value) {
    ElMessage.warning('请先选择团队')
    return
  }
  try {
    const file = options.file
    // Client-side preview for images while uploading
    await chatApi.uploadFile(chatSessionId.value, file)
    ElMessage.success('文件发送成功')
  } catch {
    ElMessage.error('文件上传失败')
  }
}

// Image preview
const previewImage = (msg) => {
  previewImageUrl.value = getMediaUrl(msg)
  previewImageName.value = msg.fileName || '图片'
  showImageViewer.value = true
}

// File download
const downloadFile = async (msg) => {
  let url = msg.fileUrl
  if (msg.messageType === 'FILE' && msg.content) {
    url = `/api/chat/files/${encodeURIComponent(msg.content)}`
  }
  if (!url) return
  try {
    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (!response.ok) throw new Error('Download failed')
    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = msg.fileName || ''
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
  } catch {
    ElMessage.error('文件下载失败')
  }
}

// Pin messages
const loadPinnedMessages = async () => {
  if (!chatSessionId.value) return
  try {
    const res = await chatApi.getPinnedMessages(chatSessionId.value)
    pinnedMessages.value = res.data || []
  } catch { pinnedMessages.value = [] }
}

const handlePinMsg = async (msg) => {
  try {
    if (msg.pinned) {
      await chatApi.unpinMessage(msg.id)
      msg.pinned = false
      pinnedMessages.value = pinnedMessages.value.filter(m => m.id !== msg.id)
      ElMessage.success('已取消置顶')
    } else {
      await chatApi.pinMessage(msg.id)
      msg.pinned = true
      if (!pinnedMessages.value.find(m => m.id === msg.id)) {
        pinnedMessages.value.push(msg)
      }
      ElMessage.success('消息已置顶')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleUnpinMsg = async (msg) => {
  try {
    await chatApi.unpinMessage(msg.id)
    msg.pinned = false
    pinnedMessages.value = pinnedMessages.value.filter(m => m.id !== msg.id)
    ElMessage.success('已取消置顶')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '取消置顶失败')
  }
}

const onFilterChange = () => {
  // filter change is reactive via computed
}

// Search
const handleSearch = () => {
  if (!searchText.value.trim()) return
  // Search filtering is done via computed
}

const clearSearch = () => {
  searchText.value = ''
}

const debounceSearchMessages = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    doSearchMessages()
  }, 300)
}

const doSearchMessages = async () => {
  if (!msgSearchKeyword.value.trim() || !chatSessionId.value) {
    isSearching.value = false
    searchResults.value = []
    return
  }
  isSearching.value = true
  try {
    const res = await chatApi.getMessages(chatSessionId.value, msgSearchKeyword.value.trim())
    searchResults.value = res.data || []
  } catch {
    searchResults.value = []
  }
}

const clearMessageSearch = () => {
  isSearching.value = false
  msgSearchKeyword.value = ''
  searchResults.value = []
}

// Create team
const createTeam = async () => {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入团队名称')
    return
  }
  try {
    await teamApi.createTeam(createForm.name.trim())
    ElMessage.success('团队创建成功')
    showCreateDialog.value = false
    createForm.name = ''
    await loadTeams()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '团队创建失败')
  }
}

// Disband team
const handleDisbandTeam = async () => {
  try {
    await ElMessageBox.confirm(
      '解散团队后，所有成员将被移除且所有聊天记录将永久丢失。此操作不可撤销。',
      '解散团队',
      { confirmButtonText: '确定解散', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    await teamApi.deleteTeam(selectedTeam.value.id)
    ElMessage.success('团队已解散')
    const teamId = selectedTeam.value.id
    selectedTeam.value = null
    chatMessages.value = []
    chatSessionId.value = null
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'team_disbanded', data: { teamId } }))
    }
    await loadTeams()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '解散团队失败')
  }
}

// Menu command
const handleMenuCommand = (cmd) => {
  if (cmd === 'members') showMembersDrawer.value = true
  else if (cmd === 'disband') handleDisbandTeam()
}

// Invite
const searchUsers = async () => {
  if (!searchNickname.value.trim()) {
    userSearchResults.value = []
    return
  }
  searchingUsers.value = true
  try {
    const res = await userApi.searchUsers(searchNickname.value.trim())
    userSearchResults.value = (res.data || []).filter(u => {
      return !members.value.some(m => m.userId === u.id)
    })
  } catch {
    userSearchResults.value = []
  } finally {
    searchingUsers.value = false
  }
}

const inviteMember = async (userId) => {
  try {
    await teamApi.addMember(selectedTeam.value.id, userId)
    ElMessage.success('邀请成功')
    showInviteDialog.value = false
    searchNickname.value = ''
    userSearchResults.value = []
    await loadMembers()
    await loadTeams()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '邀请失败')
  }
}

const handleSetAdmin = async (member) => {
  try {
    await teamApi.updateMemberRole(selectedTeam.value.id, member.userId, 'ADMIN')
    ElMessage.success('已设为管理员')
    await loadMembers()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDemoteAdmin = async (member) => {
  try {
    await teamApi.updateMemberRole(selectedTeam.value.id, member.userId, 'MEMBER')
    ElMessage.success('已取消管理员')
    await loadMembers()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleRemoveMember = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.nickname} 移出团队吗？`,
      '移除成员',
      { confirmButtonText: '确定移除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    await teamApi.removeMember(selectedTeam.value.id, member.userId)
    ElMessage.success('成员已移出')
    await loadMembers()
    await loadTeams()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '移除失败')
  }
}

// Helpers
const formatFileSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const today = new Date()
  if (d.toDateString() === today.toDateString()) return '今天'
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  return d.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}

const showDateSeparator = (idx, msg) => {
  if (idx === 0) return true
  const prev = displayedMessages.value[idx - 1]
  if (!prev?.createdAt || !msg?.createdAt) return false
  const prevDate = new Date(prev.createdAt).toDateString()
  const curDate = new Date(msg.createdAt).toDateString()
  return prevDate !== curDate
}

// WebSocket
const connectWebSocket = () => {
  if (!userStore.token) return
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  ws = new WebSocket(`${protocol}//${host}/ws/chat?token=${userStore.token}`)

  ws.onopen = () => {
    if (wsReconnectTimer) {
      clearTimeout(wsReconnectTimer)
      wsReconnectTimer = null
    }
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'new_message' && chatSessionId.value && msg.data.sessionId === chatSessionId.value) {
        chatMessages.value.push(msg.data)
        if (!isSearching.value) {
          nextTick(() => scrollToBottom())
        }
      } else if (msg.type === 'team_disbanded') {
        if (selectedTeam.value && msg.data.teamId === selectedTeam.value.id) {
          selectedTeam.value = null
          chatMessages.value = []
          chatSessionId.value = null
          ElMessage.warning('该团队已被解散')
          loadTeams()
        }
      }
    } catch { /* ignore */ }
  }

  ws.onclose = () => {
    wsReconnectTimer = setTimeout(() => {
      if (selectedTeam.value) connectWebSocket()
    }, 3000)
  }
}

watch(showInviteDialog, (val) => {
  if (val) {
    searchNickname.value = ''
    userSearchResults.value = []
  }
})

onMounted(() => {
  loadTeams()
  connectWebSocket()
})

onUnmounted(() => {
  if (isResizing) stopResize()
  if (ws) { ws.close(); ws = null }
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
/* ===== Telegram-style Layout ===== */
.teams-content {
  display: flex;
  height: 100%;
  background-color: #fff;
}

/* Sidebar */
.teams-sidebar {
  min-width: 220px;
  background-color: #fff;
  border-right: 1px solid #E8ECEF;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 12px 12px 8px;
}

.sidebar-search {
  position: relative;
}

.sidebar-search :deep(.el-input__wrapper) {
  background-color: #F4F4F5;
  border-radius: 22px;
  box-shadow: none;
  border: none;
  padding: 2px 12px;
}

.sidebar-search :deep(.el-input__wrapper:hover) {
  background-color: #EBEBEC;
}

.sidebar-search :deep(.el-input__wrapper.is-focus) {
  background-color: #fff;
  box-shadow: 0 0 0 2px #3390EC !important;
}

.team-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.team-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 12px;
  margin-bottom: 2px;
  transition: background-color 0.15s;
}

.team-item:hover {
  background-color: #F4F4F5;
}

.team-item.active {
  background-color: #3390EC;
}

.team-item.active .team-name,
.team-item.active .team-preview {
  color: #fff;
}

.team-avatar {
  flex-shrink: 0;
}

.team-info {
  margin-left: 12px;
  flex: 1;
  min-width: 0;
}

.team-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.team-name {
  font-weight: 600;
  font-size: 15px;
  color: #222;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-bottom {
  margin-top: 2px;
}

.team-preview {
  font-size: 13px;
  color: #707579;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #E8ECEF;
}

.create-team-btn {
  width: 100%;
  justify-content: center;
  color: #3390EC;
  font-weight: 500;
}

.create-team-btn:hover {
  background-color: #F4F4F5;
}

.resize-handle {
  width: 4px;
  cursor: col-resize;
  background-color: transparent;
  flex-shrink: 0;
  transition: background-color 0.15s;
}

.resize-handle:hover {
  background-color: #3390EC;
}

/* Chat area */
.teams-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  min-width: 0;
}

.chat-header {
  height: 60px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #E8ECEF;
  flex-shrink: 0;
}

.chat-header-info {
  flex: 1;
  min-width: 0;
}

.chat-header-name {
  font-weight: 600;
  font-size: 16px;
  color: #222;
}

.chat-header-subtitle {
  font-size: 13px;
  color: #707579;
}

.chat-header-actions {
  display: flex;
  gap: 4px;
}

.chat-header-actions :deep(.el-button.is-circle) {
  color: #707579;
  border: none;
  background: transparent;
}

.chat-header-actions :deep(.el-button.is-circle:hover) {
  background-color: #F4F4F5;
}

.filter-select {
  width: 80px;
}

.filter-select :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  border: none;
  padding: 0 4px;
}

.filter-select :deep(.el-input__inner) {
  font-size: 13px;
  color: #707579;
}

/* Pinned section */
.pinned-section {
  position: sticky;
  top: 0;
  z-index: 10;
  margin-bottom: 12px;
  background: #FFF8E1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.pinned-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  color: #F57C00;
  font-size: 13px;
  font-weight: 600;
  user-select: none;
}

.pinned-header:hover {
  background: #FFF3CD;
}

.pinned-chevron {
  margin-left: auto;
  transition: transform 0.2s;
  font-size: 14px;
}

.pinned-chevron.rotated {
  transform: rotate(180deg);
}

.pinned-list {
  border-top: 1px solid #FFE082;
}

.pinned-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid #FFECB3;
}

.pinned-item:last-child {
  border-bottom: none;
}

.pinned-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 8px;
}

.pinned-item-author {
  font-weight: 600;
  font-size: 13px;
  color: #E65100;
  flex-shrink: 0;
}

.pinned-item-text {
  font-size: 13px;
  color: #5D4037;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pinned-unpin-btn {
  color: #BF360C !important;
  flex-shrink: 0;
  opacity: 0.6;
}

.pinned-unpin-btn:hover {
  opacity: 1;
}

/* Pin button on messages */
.pin-btn {
  color: #A0A5AD !important;
  padding: 0 2px !important;
  height: auto !important;
  opacity: 0;
  transition: opacity 0.15s;
}

.message:hover .pin-btn {
  opacity: 1;
}

.pin-btn:hover {
  color: #F57C00 !important;
}

/* Message search bar */
.message-search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #E8ECEF;
}

.message-search-bar :deep(.el-input__wrapper) {
  border-radius: 22px;
  background-color: #F4F4F5;
  box-shadow: none;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23f5f5f5' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.empty-chat-content {
  text-align: center;
}

.empty-chat-icon {
  width: 80px;
  height: 80px;
  line-height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3390EC, #6C5CE7);
  color: #fff;
  font-size: 36px;
  font-weight: bold;
  margin: 0 auto 16px;
}

.empty-chat-title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
  margin-bottom: 4px;
}

.empty-chat-text {
  font-size: 14px;
  color: #909399;
}

.date-separator {
  text-align: center;
  margin: 12px 0;
}

.date-separator span {
  display: inline-block;
  background: rgba(0, 0, 0, 0.08);
  color: #707579;
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 500;
}

/* Messages */
.message {
  display: flex;
  margin-bottom: 6px;
  align-items: flex-end;
  gap: 6px;
}

.message-self {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  align-self: flex-end;
}

.message-body {
  max-width: 60%;
  display: flex;
  flex-direction: column;
}

.message-body-self {
  align-items: flex-end;
}

.message-bubble {
  padding: 8px 14px;
  border-radius: 16px;
  word-break: break-word;
  line-height: 1.45;
  font-size: 15px;
  position: relative;
}

.bubble-other {
  background-color: #F0F2F5;
  color: #222;
  border-bottom-left-radius: 4px;
}

.bubble-self {
  background: linear-gradient(135deg, #3390EC, #2B7BD6);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-image {
  padding: 3px;
  background: transparent;
  overflow: hidden;
}

.bubble-video, .bubble-file {
  padding: 10px 14px;
}

.msg-text {
  white-space: pre-wrap;
}

.msg-image-wrapper {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  max-width: 300px;
}

.msg-image {
  width: 100%;
  max-width: 300px;
  max-height: 300px;
  object-fit: cover;
  display: block;
  border-radius: 12px;
  transition: transform 0.2s;
}

.msg-image:hover {
  transform: scale(1.02);
}

.msg-media-wrapper {
  max-width: 400px;
}

.msg-video {
  width: 100%;
  max-width: 400px;
  max-height: 300px;
  border-radius: 12px;
  outline: none;
}

.audio-player {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  min-width: 250px;
}

.msg-audio {
  flex: 1;
  height: 40px;
  outline: none;
}

.audio-name {
  font-size: 13px;
  color: #707579;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

.msg-file-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  min-width: 250px;
  transition: background-color 0.15s;
  padding: 4px;
  border-radius: 10px;
}

.msg-file-wrapper:hover {
  background: rgba(0, 0, 0, 0.04);
}

.file-icon-box {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(51, 144, 236, 0.1);
  border-radius: 12px;
  color: #3390EC;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: #222;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.file-size {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.file-download {
  color: #909399;
  flex-shrink: 0;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  padding: 0 4px;
}

.time-right {
  text-align: right;
}

/* Input area */
.chat-input-area {
  border-top: 1px solid #E8ECEF;
  padding: 8px 16px 12px;
  background: #fff;
  flex-shrink: 0;
}

.chat-input-toolbar {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
}

.toolbar-btn {
  color: #707579 !important;
  border: none !important;
  background: transparent !important;
}

.toolbar-btn:hover {
  background-color: #F4F4F5 !important;
  color: #3390EC !important;
}

.recording-active { color: #E53935 !important; background: #FFEBEE !important; }

/* Recording bar */
.recording-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px; background: #FFF3E0; border-radius: 12px;
  margin-bottom: 8px; animation: pulse 1.5s infinite;
}
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }
.recording-dot { width: 12px; height: 12px; border-radius: 50%; background: #E53935; animation: blink 0.8s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.recording-timer { font-size: 16px; font-weight: 700; color: #E53935; min-width: 30px; }
.recording-hint { font-size: 13px; color: #BF360C; flex: 1; }

/* Audio message widget */
.msg-audio-wrapper {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  padding: 4px 0; min-width: 180px;
}
.audio-play-icon { flex-shrink: 0; transition: color 0.2s; }
.audio-play-icon.is-playing { color: #3390EC; }
.audio-waveform { display: flex; align-items: center; gap: 2px; flex: 1; height: 30px; }
.audio-bar { width: 3px; background: currentColor; border-radius: 2px; opacity: 0.5; }
.audio-duration { font-size: 12px; opacity: 0.7; flex-shrink: 0; }

.chat-input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.msg-input :deep(.el-textarea__inner) {
  border-radius: 20px;
  background: #F4F4F5;
  border: none;
  padding: 10px 18px;
  font-size: 15px;
  resize: none;
  line-height: 1.4;
  box-shadow: none;
}

.msg-input :deep(.el-textarea__inner:focus) {
  background: #fff;
  box-shadow: 0 0 0 2px #3390EC;
}

.send-btn {
  flex-shrink: 0;
  background: #3390EC !important;
  border: none !important;
  width: 42px;
  height: 42px;
}

.send-btn:hover {
  background: #2B7BD6 !important;
}

.send-btn.is-disabled {
  background: #C4C9CC !important;
}

/* Empty main */
.empty-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.empty-main-content {
  text-align: center;
}

.empty-main-text {
  margin-top: 12px;
  font-size: 16px;
  color: #909399;
}

/* Members list */
.members-list {
  padding: 8px 0;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  gap: 12px;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-weight: 600;
  font-size: 15px;
  color: #222;
  display: flex;
  align-items: center;
  gap: 6px;
}

.creator-tag {
  font-size: 11px;
}

.member-actions {
  flex-shrink: 0;
}

.member-more-btn {
  color: #707579 !important;
  padding: 4px !important;
  height: auto !important;
}

.member-more-btn:hover {
  color: #3390EC !important;
  background: #F4F4F5 !important;
}

.member-email {
  font-size: 13px;
  color: #707579;
  margin-top: 2px;
}

/* Search results */
.search-results {
  margin-top: 12px;
  max-height: 340px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 12px;
  gap: 12px;
  transition: background-color 0.15s;
}

.search-result-item:hover {
  background-color: #F4F4F5;
}

.search-result-info {
  flex: 1;
  min-width: 0;
}

.search-result-name {
  font-weight: 500;
  font-size: 15px;
  color: #222;
}

.search-result-email {
  font-size: 13px;
  color: #707579;
  margin-top: 2px;
}

/* Image viewer */
.image-viewer-dialog :deep(.el-dialog__header) {
  display: none;
}

.image-viewer-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.preview-image-full {
  max-width: 80vw;
  max-height: 80vh;
  cursor: zoom-out;
  border-radius: 8px;
}

.preview-image-name {
  text-align: center;
  color: #fff;
  padding: 12px;
  font-size: 14px;
}
</style>
