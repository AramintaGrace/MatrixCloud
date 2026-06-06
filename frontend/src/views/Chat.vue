<template>
  <div class="chat-container">
    <!-- 左侧对话列表 -->
    <div class="chat-sidebar" :style="{ width: sidebarWidth + 'px' }">
      <div class="sidebar-header">
        <div class="sidebar-search">
          <el-input
            v-model="searchText"
            placeholder="搜索聊天"
            :prefix-icon="Search"
            size="default"
            clearable
          />
        </div>
      </div>
      <div class="chat-list">
        <div
          v-for="chat in filteredChats"
          :key="chat.sessionId"
          class="chat-item"
          :class="{ active: selectedChat?.sessionId === chat.sessionId }"
          @click="selectChat(chat)"
        >
          <el-avatar :size="48" :style="{ borderRadius: '50%' }" class="chat-avatar">
            {{ chat.targetName.charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="chat-info">
            <div class="chat-top">
              <span class="chat-name">{{ chat.targetName }}</span>
              <span class="chat-time">{{ chat.lastTime }}</span>
            </div>
            <div class="chat-bottom">
              <span class="chat-last">{{ chat.lastMessage || '开始聊天吧' }}</span>
            </div>
          </div>
        </div>
        <div v-if="filteredChats.length === 0" style="text-align: center; color: #909399; padding: 20px;">
          暂无对话
        </div>
      </div>
    </div>

    <div class="resize-handle" @mousedown="startResize"></div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main" v-if="selectedChat">
      <div class="chat-header">
        <el-avatar :size="40" :style="{ borderRadius: '50%' }">
          {{ selectedChat.targetName.charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="chat-header-info">
          <div class="chat-header-name">{{ selectedChat.targetName }}</div>
        </div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-chat">
          <div class="empty-chat-content">
            <div class="empty-chat-icon">{{ selectedChat.targetName.charAt(0).toUpperCase() }}</div>
            <div class="empty-chat-title">{{ selectedChat.targetName }}</div>
            <div class="empty-chat-text">开始对话吧</div>
          </div>
        </div>
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message"
          :class="{ 'message-self': msg.senderId === userStore.user?.id }"
        >
          <el-avatar v-if="msg.senderId !== userStore.user?.id" :size="36" class="msg-avatar">
            {{ (msg.senderName || '').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="message-body" :class="{ 'message-body-self': msg.senderId === userStore.user?.id }">
            <!-- Image message -->
            <div v-if="msg.messageType === 'IMAGE'" class="message-bubble bubble-image" :class="{
              'bubble-self': msg.senderId === userStore.user?.id
            }">
              <div class="msg-image-wrapper" @click="previewImage(msg)">
                <img :src="getMediaUrl(msg)" :alt="msg.fileName" class="msg-image" loading="lazy" />
              </div>
            </div>
            <!-- Video message -->
            <div v-else-if="msg.messageType === 'VIDEO'" class="message-bubble" :class="{
              'bubble-self': msg.senderId === userStore.user?.id,
              'bubble-other': msg.senderId !== userStore.user?.id
            }">
              <div class="msg-video-wrapper">
                <video :src="getMediaUrl(msg)" controls class="msg-video" preload="metadata" @click.stop>
                  您的浏览器不支持视频播放
                </video>
              </div>
            </div>
            <!-- File message -->
            <div v-else-if="msg.messageType === 'FILE'" class="message-bubble" :class="{
              'bubble-self': msg.senderId === userStore.user?.id,
              'bubble-other': msg.senderId !== userStore.user?.id,
              'bubble-file': true
            }">
              <div class="msg-file-wrapper" @click="downloadFile(msg)">
                <div class="file-icon-box">
                  <el-icon :size="24"><Document /></el-icon>
                </div>
                <div class="file-info">
                  <div class="file-name">{{ msg.fileName || '未知文件' }}</div>
                  <div class="file-size">{{ formatFileSize(msg.fileSize) }}</div>
                </div>
                <el-icon :size="18" class="file-download-icon"><Download /></el-icon>
              </div>
            </div>
            <!-- Audio message -->
            <div v-else-if="msg.messageType === 'AUDIO'" class="message-bubble" :class="{
              'bubble-self': msg.senderId === userStore.user?.id,
              'bubble-other': msg.senderId !== userStore.user?.id
            }">
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
            <!-- Text/Emoji message -->
            <div v-else class="message-bubble" :class="{
              'bubble-self': msg.senderId === userStore.user?.id,
              'bubble-other': msg.senderId !== userStore.user?.id
            }">
              {{ msg.content }}
            </div>
            <div class="message-time" :class="{ 'time-right': msg.senderId === userStore.user?.id }">
              {{ msg.time }}
            </div>
          </div>
        </div>
        <div ref="messagesEnd"></div>
      </div>

      <div class="chat-input-area">
        <!-- Voice recording indicator -->
        <div v-if="isRecording" class="recording-bar">
          <span class="recording-dot"></span>
          <span class="recording-timer">{{ recordingTimer }}s</span>
          <span class="recording-hint">正在录音，再次点击停止</span>
          <el-button size="small" type="danger" @click="stopRecording" round>停止并发送</el-button>
        </div>
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
        <div class="chat-input-row" v-if="!isRecording">
          <el-input
            v-model="messageText"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="输入消息..."
            class="msg-input"
            @keydown.enter.exact.prevent="sendMessage"
            resize="none"
          />
          <el-button type="primary" :disabled="!messageText.trim()" @click="sendMessage" class="send-btn" :icon="Promotion" circle />
        </div>
      </div>
    </div>

    <div v-else class="empty-main">
      <div class="empty-main-content">
        <el-icon :size="64" color="#C4C9CC"><ChatDotRound /></el-icon>
        <div class="empty-main-text">选择一个对话开始聊天</div>
      </div>
    </div>

    <!-- 图片预览 -->
    <el-dialog v-model="showImageViewer" :align-center="true" width="auto" class="image-viewer-dialog">
      <img :src="previewImageUrl" :alt="previewImageName" class="preview-image-full" @click="showImageViewer = false" />
      <div class="preview-image-name">{{ previewImageName }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import { Search, ChatDotRound, Promotion, FolderOpened, Document, Download, Microphone, VideoPlay } from '@element-plus/icons-vue'
import { chatApi } from '../api'
import EmojiPicker from '../components/EmojiPicker.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const searchText = ref('')
const selectedChat = ref(null)
const messageText = ref('')
const messages = ref([])
const chats = ref([])
const messagesContainer = ref(null)
const messagesEnd = ref(null)
const sidebarWidth = ref(320)
const playingAudioId = ref(null)
const showImageViewer = ref(false)
const previewImageUrl = ref('')
const previewImageName = ref('')

// Voice recording
const isRecording = ref(false)
const recordingTimer = ref(0)
let mediaRecorder = null
let recordingInterval = null
let audioChunks = []

let ws = null
let wsTimer = null
let isResizing = false
let audioEl = null

const filteredChats = computed(() => {
  if (!searchText.value) return chats.value
  const kw = searchText.value.toLowerCase()
  return chats.value.filter(c => c.targetName.toLowerCase().includes(kw))
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

const loadPrivateSessions = async () => {
  try {
    const res = await chatApi.getPrivateSessions()
    chats.value = res.data || []
  } catch { chats.value = [] }
}

const loadMessages = async () => {
  if (!selectedChat.value) return
  try {
    const res = await chatApi.getMessages(selectedChat.value.sessionId)
    messages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch { messages.value = [] }
}

const selectChat = async (chat) => {
  selectedChat.value = chat
  await loadMessages()
}

const sendMessage = async () => {
  const content = messageText.value.trim()
  if (!content || !selectedChat.value) return
  try {
    const res = await chatApi.sendMessage(selectedChat.value.sessionId, content)
    // Dedup: WebSocket broadcast may have already added this message
    if (!messages.value.some(m => m.id === res.data.id)) {
      messages.value.push(res.data)
    }
    messageText.value = ''
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('发送失败')
  }
}

// Voice recording
const toggleRecording = async () => {
  if (isRecording.value) {
    await stopRecording()
  } else {
    await startRecording()
  }
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus' : 'audio/webm'
    mediaRecorder = new MediaRecorder(stream, { mimeType })
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop())
      if (audioChunks.length > 0) {
        await uploadAudio()
      }
    }
    mediaRecorder.start()
    isRecording.value = true
    recordingTimer.value = 0
    recordingInterval = setInterval(() => { recordingTimer.value++ }, 1000)
  } catch {
    ElMessage.error('无法访问麦克风')
  }
}

const stopRecording = async () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
  if (recordingInterval) { clearInterval(recordingInterval); recordingInterval = null }
}

const uploadAudio = async () => {
  if (!selectedChat.value || audioChunks.length === 0) return
  try {
    const blob = new Blob(audioChunks, { type: 'audio/webm' })
    const file = new File([blob], `voice_${Date.now()}.webm`, { type: 'audio/webm' })
    await chatApi.uploadFile(selectedChat.value.sessionId, file)
  } catch {
    ElMessage.error('语音发送失败')
  }
}

const togglePlayAudio = (msg) => {
  if (playingAudioId.value === msg.id) {
    if (audioEl) { audioEl.pause(); audioEl = null }
    playingAudioId.value = null
    return
  }
  if (audioEl) { audioEl.pause() }
  audioEl = new Audio(getMediaUrl(msg))
  audioEl.onended = () => { playingAudioId.value = null; audioEl = null }
  audioEl.onerror = () => { playingAudioId.value = null; audioEl = null }
  audioEl.play()
  playingAudioId.value = msg.id
}

const formatDuration = (bytes) => {
  if (!bytes) return '0:00'
  // Estimate duration from webm file size (~2KB/s)
  const secs = Math.round(bytes / 2000)
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

const handleFileUpload = async (options) => {
  if (!selectedChat.value) { ElMessage.warning('请先选择对话'); return }
  try {
    await chatApi.uploadFile(selectedChat.value.sessionId, options.file)
    ElMessage.success('文件发送成功')
  } catch { ElMessage.error('文件上传失败') }
}

const downloadFile = async (msg) => {
  let url = `/api/chat/files/${encodeURIComponent(msg.content)}`
  try {
    const response = await fetch(url, { headers: { 'Authorization': `Bearer ${userStore.token}` } })
    if (!response.ok) throw new Error('Download failed')
    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl; a.download = msg.fileName || ''
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
  } catch { ElMessage.error('文件下载失败') }
}

const formatFileSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const getMediaUrl = (msg) => {
  // Use proxy endpoint for reliable access (no pre-signed URL expiry)
  if (msg.messageType === 'IMAGE' || msg.messageType === 'VIDEO' || msg.messageType === 'AUDIO') {
    return `/api/chat/files/${encodeURIComponent(msg.content)}`
  }
  return msg.fileUrl || ''
}

const getWaveHeights = (msgId) => {
  // Deterministic heights from message ID
  let hash = 0
  const s = String(msgId)
  for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash)
  return Array.from({ length: 12 }, (_, i) => {
    const h = ((hash * (i + 7)) % 13) + 8
    return { i, h }
  })
}

const previewImage = (msg) => {
  previewImageUrl.value = getMediaUrl(msg)
  previewImageName.value = msg.fileName || '图片'
  showImageViewer.value = true
}

const insertEmoji = (emoji) => { messageText.value += emoji }
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesEnd.value) messagesEnd.value.scrollIntoView({ behavior: 'smooth' })
  })
}

const initFromQuery = async () => {
  const targetUserId = route.query.with
  const targetName = route.query.name
  if (targetUserId) {
    try {
      const res = await chatApi.createPrivateSession(Number(targetUserId))
      const session = res.data
      selectedChat.value = {
        sessionId: session.sessionId,
        targetName: targetName || session.targetName,
        targetUserId: session.targetUserId
      }
      await loadMessages()
      router.replace('/chat')
      await loadPrivateSessions()
    } catch (e) { ElMessage.error(e.response?.data?.message || '创建对话失败') }
  }
}

const connectWS = () => {
  if (!userStore.token) return
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  ws = new WebSocket(`${protocol}//${host}/ws/chat?token=${userStore.token}`)
  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'new_message' && selectedChat.value && msg.data.sessionId === selectedChat.value.sessionId) {
        if (!messages.value.some(m => m.id === msg.data.id)) {
          messages.value.push(msg.data)
          nextTick(() => scrollToBottom())
        }
      }
    } catch {}
  }
  ws.onclose = () => { wsTimer = setTimeout(() => { if (selectedChat.value) connectWS() }, 3000) }
}

watch(selectedChat, (val) => { if (val && (!ws || ws.readyState !== WebSocket.OPEN)) connectWS() })

onMounted(async () => {
  await loadPrivateSessions()
  connectWS()
  await initFromQuery()
})

onUnmounted(() => {
  if (isResizing) stopResize()
  if (ws) { ws.close(); ws = null }
  if (wsTimer) clearTimeout(wsTimer)
  if (recordingInterval) clearInterval(recordingInterval)
  if (audioEl) { audioEl.pause(); audioEl = null }
})
</script>

<style scoped>
.chat-container { display: flex; height: 100%; background-color: #fff; }

.chat-sidebar {
  min-width: 220px;
  background-color: #fff;
  border-right: 1px solid #E8ECEF;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.resize-handle {
  width: 4px;
  cursor: col-resize;
  background-color: transparent;
  flex-shrink: 0;
  transition: background-color 0.15s;
}
.resize-handle:hover { background-color: #3390EC; }

.sidebar-header { padding: 12px; }
.sidebar-search :deep(.el-input__wrapper) {
  background-color: #F4F4F5; border-radius: 22px; box-shadow: none; border: none;
}
.sidebar-search :deep(.el-input__wrapper:hover) { background-color: #EBEBEC; }

.chat-list { flex: 1; overflow-y: auto; padding: 4px 8px; }
.chat-item {
  display: flex; align-items: center; padding: 10px 12px; cursor: pointer;
  border-radius: 12px; margin-bottom: 2px; transition: background-color 0.15s;
}
.chat-item:hover { background-color: #F4F4F5; }
.chat-item.active { background-color: #3390EC; }
.chat-item.active .chat-name, .chat-item.active .chat-last, .chat-item.active .chat-time { color: #fff; }
.chat-avatar { flex-shrink: 0; }
.chat-info { margin-left: 12px; flex: 1; min-width: 0; }
.chat-top { display: flex; justify-content: space-between; align-items: center; }
.chat-name { font-weight: 600; font-size: 15px; color: #222; }
.chat-time { font-size: 12px; color: #909399; flex-shrink: 0; }
.chat-bottom { margin-top: 2px; }
.chat-last { font-size: 13px; color: #707579; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.chat-main { flex: 1; display: flex; flex-direction: column; background-color: #fff; min-width: 0; }
.chat-header { height: 60px; padding: 0 16px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid #E8ECEF; flex-shrink: 0; }
.chat-header-info { flex: 1; }
.chat-header-name { font-weight: 600; font-size: 16px; color: #222; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 12px 16px;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23f5f5f5' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.empty-chat { display: flex; align-items: center; justify-content: center; height: 100%; }
.empty-chat-content { text-align: center; }
.empty-chat-icon { width: 80px; height: 80px; line-height: 80px; border-radius: 50%; background: linear-gradient(135deg, #3390EC, #6C5CE7); color: #fff; font-size: 36px; font-weight: bold; margin: 0 auto 16px; }
.empty-chat-title { font-size: 18px; font-weight: 600; color: #222; margin-bottom: 4px; }
.empty-chat-text { font-size: 14px; color: #909399; }

.message { display: flex; margin-bottom: 6px; align-items: flex-end; gap: 6px; }
.message-self { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; align-self: flex-end; }
.message-body { max-width: 60%; }
.message-body-self { display: flex; flex-direction: column; align-items: flex-end; }

.message-bubble { padding: 8px 14px; border-radius: 16px; word-break: break-word; line-height: 1.45; font-size: 15px; }
.bubble-other { background-color: #F0F2F5; color: #222; border-bottom-left-radius: 4px; }
.bubble-self { background: linear-gradient(135deg, #3390EC, #2B7BD6); color: #fff; border-bottom-right-radius: 4px; }
.bubble-file { padding: 10px 14px !important; }
.bubble-image { padding: 3px !important; background: transparent !important; overflow: hidden; }

.msg-image-wrapper { cursor: pointer; border-radius: 12px; overflow: hidden; max-width: 300px; }
.msg-image { width: 100%; max-width: 300px; max-height: 300px; object-fit: cover; display: block; border-radius: 12px; transition: transform 0.2s; }
.msg-image:hover { transform: scale(1.02); }

.msg-video-wrapper { max-width: 360px; }
.msg-video { width: 100%; max-width: 360px; max-height: 280px; border-radius: 12px; outline: none; }

.message-time { font-size: 12px; color: #909399; margin-top: 2px; padding: 0 4px; }
.time-right { text-align: right; }

/* Voice recording */
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
.recording-active { color: #E53935 !important; background: #FFEBEE !important; }

/* Audio message */
.msg-audio-wrapper {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  padding: 4px 0; min-width: 180px;
}
.audio-play-icon { flex-shrink: 0; transition: color 0.2s; }
.audio-play-icon.is-playing { color: #3390EC; }
.audio-waveform { display: flex; align-items: center; gap: 2px; flex: 1; height: 30px; }
.audio-bar { width: 3px; background: currentColor; border-radius: 2px; opacity: 0.5; }
.audio-duration { font-size: 12px; opacity: 0.7; flex-shrink: 0; }

/* File message */
.msg-file-wrapper { display: flex; align-items: center; gap: 10px; cursor: pointer; min-width: 200px; }
.file-icon-box { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.2); border-radius: 10px; flex-shrink: 0; }
.file-info { flex: 1; min-width: 0; }
.file-name { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
.file-size { font-size: 11px; opacity: 0.7; margin-top: 2px; }
.file-download-icon { flex-shrink: 0; opacity: 0.7; }

.chat-input-area { border-top: 1px solid #E8ECEF; padding: 8px 16px 12px; background: #fff; flex-shrink: 0; }
.chat-input-toolbar { display: flex; gap: 4px; margin-bottom: 6px; }
.toolbar-btn { color: #707579 !important; border: none !important; background: transparent !important; }
.toolbar-btn:hover { background-color: #F4F4F5 !important; color: #3390EC !important; }
.chat-input-row { display: flex; align-items: flex-end; gap: 8px; }

.msg-input :deep(.el-textarea__inner) {
  border-radius: 20px; background: #F4F4F5; border: none;
  padding: 10px 18px; font-size: 15px; resize: none; line-height: 1.4; box-shadow: none;
}
.msg-input :deep(.el-textarea__inner:focus) { background: #fff; box-shadow: 0 0 0 2px #3390EC; }

.send-btn { flex-shrink: 0; background: #3390EC !important; border: none !important; width: 42px; height: 42px; }
.send-btn:hover { background: #2B7BD6 !important; }
.send-btn.is-disabled { background: #C4C9CC !important; }

.empty-main { flex: 1; display: flex; align-items: center; justify-content: center; background: #fff; }
.empty-main-content { text-align: center; }
.empty-main-text { margin-top: 12px; font-size: 16px; color: #909399; }

/* Image viewer */
.image-viewer-dialog :deep(.el-dialog__header) { display: none; }
.image-viewer-dialog :deep(.el-dialog__body) { padding: 0; }
.preview-image-full { max-width: 80vw; max-height: 80vh; cursor: zoom-out; border-radius: 8px; }
.preview-image-name { text-align: center; color: #fff; padding: 12px; font-size: 14px; }
</style>
