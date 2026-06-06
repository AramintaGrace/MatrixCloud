<template>
  <div class="page-container">
    <div class="page-header">
      <h2>会议</h2>
      <el-button v-if="activeTab === 'video'" type="primary" :icon="Plus" @click="openCreateDialog">创建会议</el-button>
    </div>

    <!-- Tab bar -->
    <div class="tab-bar">
      <div class="tab-item" :class="{ active: activeTab === 'video' }" @click="activeTab = 'video'">
        <el-icon><VideoCamera /></el-icon>
        <span>视频会议</span>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'records' }" @click="activeTab = 'records'">
        <el-icon><Notebook /></el-icon>
        <span>会议记录</span>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'">
        <el-icon><Clock /></el-icon>
        <span>AI会议语音记录</span>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'minutes' }" @click="activeTab = 'minutes'">
        <el-icon><Document /></el-icon>
        <span>AI会议纪要</span>
      </div>
    </div>

    <!-- ==================== Tab 1: 视频会议 ==================== -->
    <div v-if="activeTab === 'video'" class="page-body">
      <div class="join-section">
        <div class="join-input-group">
          <el-input
            v-model="joinForm.roomName"
            placeholder="输入会议名称或粘贴会议链接"
            size="large"
            clearable
            @keyup.enter="handleJoinClick"
          >
            <template #prefix><el-icon><Link /></el-icon></template>
          </el-input>
          <el-button type="primary" size="large" @click="handleJoinClick" :disabled="!joinForm.roomName" class="join-btn">
            加入会议
          </el-button>
        </div>
      </div>

      <div v-if="activeMeetings.length > 0" class="active-section">
        <h3 class="section-title">进行中的会议</h3>
        <div class="meeting-grid">
          <div
            v-for="meeting in activeMeetings"
            :key="meeting.id"
            class="meeting-card"
            @click="handleJoinFromList(meeting)"
          >
            <div class="meeting-card-icon">
              <el-icon :size="28"><VideoCamera /></el-icon>
            </div>
            <div class="meeting-card-info">
              <div class="meeting-card-name">{{ meeting.roomName }}</div>
              <div class="meeting-card-meta">
                <el-tag v-if="meeting.hasPassword" size="small" type="warning" effect="plain">需要密码</el-tag>
                <el-tag v-else size="small" type="success" effect="plain">公开</el-tag>
              </div>
            </div>
            <el-button type="primary" size="small" round>加入</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Tab 2: 会议记录 ==================== -->
    <div v-if="activeTab === 'records'" class="page-body">
      <div v-if="recordsLoading" class="loading-state">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <p>加载中...</p>
      </div>
      <template v-else>
        <!-- 切换按钮 + 搜索 -->
        <div class="records-toolbar">
          <div class="records-tabs">
            <button class="record-tab-btn" :class="{ active: recordTab === 'created' }" @click="recordTab = 'created'">我创建的会议</button>
            <button class="record-tab-btn" :class="{ active: recordTab === 'joined' }" @click="recordTab = 'joined'">我加入的会议</button>
          </div>
          <el-input v-model="recordSearch" placeholder="搜索会议名称..." size="default" clearable :prefix-icon="Search" style="width: 260px;" />
        </div>

        <!-- 我创建的会议 -->
        <template v-if="recordTab === 'created'">
          <div v-if="filteredCreatedRecords.length === 0" class="empty-state">
            <el-empty description="暂无创建的会议" :image-size="80" />
          </div>
          <div v-else class="record-list">
            <div v-for="m in filteredCreatedRecords" :key="m.id" class="record-card">
              <div class="record-card-header">
                <div class="record-card-title">
                  <el-icon :size="18"><VideoCamera /></el-icon>
                  <span class="record-room-name">{{ m.roomName }}</span>
                  <el-tag size="small" :type="m.status === 'ACTIVE' ? 'success' : 'info'">
                    {{ m.status === 'ACTIVE' ? '进行中' : '已结束' }}
                  </el-tag>
                </div>
                <span class="record-duration" v-if="m.duration">会议时长 {{ m.duration }}</span>
              </div>
              <div class="record-card-body">
                <span class="record-meta">参会人数 {{ m.participantCount }} 人</span>
                <el-dropdown trigger="click" v-if="m.participants && m.participants.length > 0">
                  <span class="record-participants-link">
                    <el-icon><User /></el-icon> 查看参会人员 <el-icon><ArrowDown /></el-icon>
                  </span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="p in m.participants" :key="p.userId" class="participant-dropdown-item">
                        <el-avatar :size="24" :src="p.avatar">{{ p.nickname?.charAt(0) }}</el-avatar>
                        <span style="margin-left: 8px; font-size: 13px;">{{ p.nickname }}</span>
                        <span v-if="p.duration" style="margin-left: auto; font-size: 12px; color: #3390EC;">{{ p.duration }}</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </template>

        <!-- 我加入的会议 -->
        <template v-if="recordTab === 'joined'">
          <div v-if="filteredJoinedRecords.length === 0" class="empty-state">
            <el-empty description="暂无加入的会议" :image-size="80" />
          </div>
          <div v-else class="record-list">
            <div v-for="m in filteredJoinedRecords" :key="m.id" class="record-card">
              <div class="record-card-header">
                <div class="record-card-title">
                  <el-icon :size="18"><VideoCamera /></el-icon>
                  <span class="record-room-name">{{ m.roomName }}</span>
                  <el-tag size="small" :type="m.status === 'ACTIVE' ? 'success' : 'info'">
                    {{ m.status === 'ACTIVE' ? '进行中' : '已结束' }}
                  </el-tag>
                </div>
                <span class="record-duration" v-if="m.duration">会议时长 {{ m.duration }}</span>
              </div>
              <div class="record-card-body">
                <span class="record-meta">创建者 {{ m.creatorName }}</span>
                <span class="record-meta">参会人数 {{ m.participantCount }} 人</span>
                <el-dropdown trigger="click" v-if="m.participants && m.participants.length > 0">
                  <span class="record-participants-link">
                    <el-icon><User /></el-icon> 查看参会人员 <el-icon><ArrowDown /></el-icon>
                  </span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="p in m.participants" :key="p.userId" class="participant-dropdown-item">
                        <el-avatar :size="24" :src="p.avatar">{{ p.nickname?.charAt(0) }}</el-avatar>
                        <span style="margin-left: 8px; font-size: 13px;">{{ p.nickname }}</span>
                        <span v-if="p.duration" style="margin-left: auto; font-size: 12px; color: #3390EC;">{{ p.duration }}</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </template>
      </template>
    </div>

    <!-- ==================== Tab 3: AI会议语音记录 ==================== -->
    <div v-if="activeTab === 'history'" class="page-body">
      <div class="search-box">
        <el-input v-model="historySearch" placeholder="搜索会议名称..." size="default" clearable :prefix-icon="Search" />
      </div>
      <div v-if="historyLoading" class="loading-state">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <p>加载中...</p>
      </div>
      <div v-else-if="filteredHistory.length === 0" class="empty-state">
        <el-empty description="暂无会议历史记录" />
      </div>
      <div v-else class="history-list">
        <div
          v-for="meeting in filteredHistory"
          :key="meeting.id"
          class="history-card"
        >
          <div class="history-card-header" @click="toggleHistoryDetail(meeting.id)">
            <div class="history-card-icon">
              <el-icon :size="24"><VideoCamera /></el-icon>
            </div>
            <div class="history-card-info">
              <div class="history-card-name">{{ meeting.roomName }}</div>
              <div class="history-card-meta">
                <span>创建于 {{ formatDate(meeting.createdAt) }}</span>
                <span v-if="meeting.endedAt">结束于 {{ formatDate(meeting.endedAt) }}</span>
                <el-tag :type="meeting.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ meeting.status === 'ACTIVE' ? '进行中' : '已结束' }}</el-tag>
                <el-tag size="small" type="info">{{ meeting.transcriptCount || 0 }} 条语音记录</el-tag>
              </div>
            </div>
            <el-icon class="expand-icon" :class="{ rotated: expandedMeetingId === meeting.id }">
              <ArrowDown />
            </el-icon>
          </div>
          <div v-if="expandedMeetingId === meeting.id" class="history-detail">
            <div v-if="historyDetailLoading" class="loading-state" style="padding: 20px;">
              <el-icon class="is-loading" :size="20"><Loading /></el-icon>
            </div>
            <div v-else-if="historyDetail && historyDetail.transcripts && historyDetail.transcripts.length > 0" class="transcript-list">
              <div class="history-detail-header">
                <span>创建者：{{ historyDetail.creatorName }}</span>
              </div>
              <div
                v-for="item in historyDetail.transcripts"
                :key="item.id"
                class="transcript-item"
              >
                <div class="transcript-meta">
                  <span class="transcript-speaker">{{ item.nickname }}</span>
                  <span v-if="item.timestamp" class="transcript-time">{{ item.timestamp }}</span>
                  <span class="transcript-date">{{ formatDate(item.createdAt) }}</span>
                </div>
                <div class="transcript-content">{{ item.content }}</div>
              </div>
            </div>
            <div v-else class="empty-state" style="padding: 20px;">
              <el-empty description="该会议暂无语音记录" :image-size="80" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Tab 3: AI会议纪要 ==================== -->
    <div v-if="activeTab === 'minutes'" class="page-body">
      <!-- Step 1: 选择会议历史记录 -->
      <div class="minutes-section">
        <h3 class="section-title">选择会议历史记录</h3>
        <el-select v-model="selectedMeetingId" placeholder="请选择会议" size="large" style="width: 100%;" filterable @change="onMinutesMeetingChange">
          <el-option
            v-for="m in historyMeetingsForMinutes"
            :key="m.id"
            :label="m.roomName + ' (' + formatDate(m.createdAt) + ')'"
            :value="m.id"
          />
        </el-select>
      </div>

      <!-- Step 2: 语音记录列表 -->
      <div v-if="selectedMeetingId" class="minutes-section">
        <div class="transcript-header">
          <h3 class="section-title" style="margin-bottom: 0;">语音记录</h3>
          <div style="display: flex; gap: 8px;">
            <el-button size="small" @click="showAddTranscript = !showAddTranscript">
              <el-icon><Plus /></el-icon> 添加语音
            </el-button>
            <el-button size="small" text @click="refreshTranscripts">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
        <div v-if="transcriptsLoading" class="loading-state" style="padding: 20px;">
          <el-icon class="is-loading" :size="20"><Loading /></el-icon>
        </div>
        <div v-else-if="transcripts.length === 0" class="empty-state">
          <el-empty description="该会议暂无语音记录，请先添加" :image-size="80" />
        </div>
        <div v-else class="transcript-list">
          <div v-for="item in transcripts" :key="item.id" class="transcript-item">
            <div class="transcript-meta">
              <span class="transcript-speaker">{{ item.nickname }}</span>
              <span v-if="item.timestamp" class="transcript-time">{{ item.timestamp }}</span>
              <span class="transcript-date">{{ formatDate(item.createdAt) }}</span>
            </div>
            <div class="transcript-content">{{ item.content }}</div>
          </div>
        </div>
      </div>

      <!-- 添加语音（可折叠） -->
      <div v-if="selectedMeetingId && showAddTranscript" class="minutes-section">
        <h3 class="section-title">添加语音记录</h3>
        <div class="recorder-box">
          <div v-if="!isRecording" class="recorder-idle">
            <el-button type="primary" :icon="Microphone" @click="startRecording">开始录音</el-button>
            <span class="recorder-hint">点击开始录音，语音将自动转为文字保存</span>
          </div>
          <div v-else class="recorder-active">
            <div class="recording-indicator">
              <span class="recording-dot"></span>
              <span>正在录音中... {{ formatDuration(recordingDuration) }}</span>
            </div>
            <el-button type="danger" @click="stopRecording">
              <el-icon><Close /></el-icon> 停止录音
            </el-button>
          </div>
        </div>
        <div class="upload-box" style="margin-top: 12px;">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".mp3,.wav,.m4a,.ogg,.flac,.aac,.wma,.webm"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将音频文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 MP3, WAV, M4A, OGG, FLAC, AAC, WMA, WEBM</div>
            </template>
          </el-upload>
          <div v-if="uploadFile" style="margin-top: 12px; display: flex; align-items: center; gap: 12px;">
            <el-input v-model="audioTimestamp" placeholder="时间戳（可选，如 00:05:32）" size="default" style="width: 200px;" />
            <el-button type="primary" @click="uploadAudio" :loading="uploading">
              {{ uploading ? '转写中...' : '上传并转写' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- Step 3: AI 自动生成纪要 -->
      <div v-if="selectedMeetingId" class="minutes-section">
        <h3 class="section-title">AI 生成会议纪要</h3>
        <div v-if="!summaryResult">
          <el-alert
            title="从语音记录中自动读取文本，由AI自动生成结构化的会议纪要"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 16px"
          />
          <el-button
            type="primary"
            size="large"
            @click="handleGenerateFromTranscripts"
            :loading="generatingSummary"
            :disabled="transcripts.length === 0"
          >
            {{ generatingSummary ? 'AI 正在生成中...' : '从语音记录自动生成纪要' }}
          </el-button>
          <span v-if="transcripts.length === 0" style="margin-left: 12px; font-size: 13px; color: #909399;">
            请先添加语音记录
          </span>
        </div>
        <div v-else class="summary-result">
          <div class="summary-toolbar">
            <el-tag size="small" type="success" style="margin-right: auto;">AI 生成</el-tag>
            <span style="font-size: 12px; color: #909399; margin-right: 12px;">
              分 {{ summaryResult.chunkCount }} 块 · {{ summaryResult.modelName }}
            </span>
            <el-button size="small" text @click="copySummary">
              <el-icon><CopyDocument /></el-icon> 复制
            </el-button>
          </div>
          <div class="summary-content markdown-body" v-html="renderedSummary"></div>
          <el-button @click="handleRegenerateSummary" style="margin-top: 12px;">重新生成</el-button>
        </div>
      </div>

      <!-- AI 纪要历史记录 -->
      <div class="minutes-section">
        <h3 class="section-title">AI 纪要历史记录</h3>
        <div v-if="summaryHistoryLoading" class="loading-state">
          <el-icon class="is-loading" :size="20"><Loading /></el-icon>
        </div>
        <div v-else-if="summaryHistoryList.length === 0" class="empty-state">
          <el-empty description="暂无AI会议纪要记录" :image-size="80" />
        </div>
        <div v-else class="history-list">
          <div
            v-for="item in summaryHistoryList"
            :key="item.id"
            class="history-card"
          >
            <div class="history-card-header" @click="toggleSummaryDetail(item)">
              <div class="history-card-icon" style="background: linear-gradient(135deg, #FCE4EC, #F8BBD0); color: #C62828;">
                <el-icon :size="24"><Notebook /></el-icon>
              </div>
              <div class="history-card-info">
                <div class="history-card-name">{{ item.roomName || '会议 #' + item.meetingId }}</div>
                <div class="history-card-meta">
                  <span>生成时间 {{ formatDate(item.createdAt) }}</span>
                  <el-tag size="small" type="success">{{ item.chunkCount || 0 }} 块 · {{ item.modelName }}</el-tag>
                </div>
              </div>
              <el-icon class="expand-icon" :class="{ rotated: expandedSummaryId === item.id }">
                <ArrowDown />
              </el-icon>
            </div>
            <div v-if="expandedSummaryId === item.id" class="history-detail">
              <div class="summary-content markdown-body" v-html="renderMarkdown(item.summaryText)"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建会议对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建会议" width="440px" :align-center="true">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-position="top">
        <el-form-item label="会议名称" prop="roomName">
          <el-input v-model="createForm.roomName" placeholder="输入会议名称" maxlength="50" show-word-limit size="large" />
        </el-form-item>
        <el-form-item label="会议类型">
          <el-radio-group v-model="createForm.meetingType">
            <el-radio value="NORMAL">普通会议</el-radio>
            <el-radio value="AI_SMART">AI智能会议</el-radio>
          </el-radio-group>
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            AI智能会议将自动录音、语音转文字，并在结束时自动生成AI会议纪要
          </div>
        </el-form-item>
        <el-form-item label="会议密码">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-switch v-model="createForm.enablePassword" />
            <span style="color: #909399; font-size: 13px;">设置密码后，参会者需输入密码才能加入</span>
          </div>
        </el-form-item>
        <el-form-item v-if="createForm.enablePassword" label="密码" prop="password">
          <el-input v-model="createForm.password" placeholder="4-6位会议密码" maxlength="6" show-password size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateMeeting" :loading="creating">创建并加入</el-button>
      </template>
    </el-dialog>

    <!-- 加入会议密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="输入会议密码" width="400px" :align-center="true">
      <div style="text-align: center; margin-bottom: 16px; color: #606266;">
        会议 <strong>{{ pendingJoinRoomName }}</strong> 需要密码
      </div>
      <el-input
        v-model="joinPassword"
        placeholder="请输入密码"
        maxlength="6"
        show-password
        size="large"
        @keyup.enter="handleJoinWithPassword"
      />
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleJoinWithPassword" :loading="joining">加入</el-button>
      </template>
    </el-dialog>

    <!-- 会议室 -->
    <el-dialog
      v-model="showMeetingRoom"
      width="100%"
      fullscreen
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="meeting-room">
        <div class="meeting-top-bar">
          <div class="meeting-top-left">
            <span class="room-name-label">{{ currentMeeting.roomName }}</span>
            <code class="meeting-link-inline">{{ meetingLink }}</code>
            <el-button size="small" text @click="copyMeetingLink" class="copy-link-btn">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
            <el-tag v-if="currentMeeting.hasPassword" size="small" type="warning">已加密</el-tag>
            <el-tag v-if="currentMeeting.meetingType === 'AI_SMART'" size="small" type="success" effect="dark">AI智能</el-tag>
          </div>
          <div class="meeting-top-right">
            <span v-if="meetingIsRecording" class="meeting-recording-indicator">
              <span class="recording-dot"></span> 录音中
            </span>
            <el-button v-if="currentMeeting.meetingType === 'AI_SMART'" size="small" text @click="showMeetingTranscripts = !showMeetingTranscripts">
              <el-icon><Document /></el-icon> 记录
            </el-button>
            <el-dropdown trigger="click" class="participants-dropdown">
              <span class="participant-count">
                <el-icon><User /></el-icon> {{ participants.length }} 人
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="p in participants" :key="p.id" class="participant-dropdown-item">
                    <el-avatar :size="28" :src="p.avatar">{{ p.nickname?.charAt(0) }}</el-avatar>
                    <span style="margin-left: 8px; font-size: 13px;">{{ p.nickname }}</span>
                    <el-tag v-if="p.isCreator" size="small" type="warning" style="margin-left: auto;">创建者</el-tag>
                    <span v-else-if="p.isSelf" style="margin-left: auto; font-size: 11px; color: #909399;">我</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 会议中语音记录 -->
        <div v-if="showMeetingTranscripts" class="meeting-transcripts-panel">
          <div class="transcript-header">
            <span style="font-weight: 600; font-size: 14px;">语音记录</span>
            <el-button size="small" text @click="refreshMeetingTranscripts">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
          <div v-if="meetingTranscripts.length === 0" style="padding: 12px; color: #909399; font-size: 13px; text-align: center;">
            录音中，语音记录将自动显示
          </div>
          <div v-else class="transcript-list" style="max-height: 200px; overflow-y: auto;">
            <div v-for="item in meetingTranscripts" :key="item.id" class="transcript-item">
              <div class="transcript-meta">
                <span class="transcript-speaker">{{ item.nickname }}</span>
                <span v-if="item.timestamp" class="transcript-time">{{ item.timestamp }}</span>
              </div>
              <div class="transcript-content">{{ item.content }}</div>
            </div>
          </div>
        </div>

        <div v-if="focusedVideo" class="focus-layout">
          <div class="focus-main" ref="focusMainRef">
            <template v-if="focusedVideo === 'local'">
              <video :ref="el => { if (el && el.srcObject !== localStream) { el.srcObject = localStream; el.play().catch(()=>{}) } }" autoplay muted playsinline class="focus-video"></video>
            </template>
            <template v-else-if="focusedVideo === 'local-screen' && localScreenStream">
              <video :ref="el => { if (el) el.srcObject = localScreenStream }" autoplay playsinline class="focus-video"></video>
            </template>
            <template v-else-if="focusedVideo.startsWith('screen-')">
              <video :ref="el => setRemoteScreenVideoRef(focusedVideo.replace('screen-', ''), el)" autoplay playsinline class="focus-video"></video>
            </template>
            <template v-else>
              <video :ref="el => setRemoteVideoRef(focusedVideo, el)" autoplay playsinline class="focus-video" @loadedmetadata="onRemoteVideoLoaded(focusedVideo)"></video>
            </template>
            <div class="focus-label">{{ getVideoLabel(focusedVideo) }}</div>
            <el-button class="fullscreen-btn" @click.stop="enterFullscreen" circle size="default">
              <el-icon :size="18"><FullScreen /></el-icon>
            </el-button>
          </div>
          <div class="focus-sidebar">
            <div class="video-thumb" :class="{ active: focusedVideo === 'local' }" @click="focusedVideo = 'local'">
              <video :ref="el => { if (el && el.srcObject !== localStream) { el.srcObject = localStream; el.play().catch(()=>{}) } }" autoplay muted playsinline class="thumb-video"></video>
              <div class="thumb-label">{{ userStore.user?.nickname || '我' }}</div>
              <div v-if="isVideoOff" class="video-off-overlay small">
                <el-avatar :size="28">{{ (userStore.user?.nickname || '我').charAt(0) }}</el-avatar>
              </div>
            </div>
            <div v-if="localScreenStream" class="video-thumb screen-share" :class="{ active: focusedVideo === 'local-screen' }" @click="focusedVideo = 'local-screen'">
              <video :ref="el => { if (el) el.srcObject = localScreenStream }" autoplay playsinline class="thumb-video"></video>
              <div class="thumb-label"><el-icon :size="12"><Monitor /></el-icon> 我的屏幕</div>
            </div>
            <div v-for="(stream, peerId) in remoteStreams" :key="peerId" class="video-thumb" :class="{ active: focusedVideo === peerId }" @click="focusedVideo = peerId">
              <video :ref="el => setRemoteVideoRef(peerId, el)" autoplay playsinline class="thumb-video" @loadedmetadata="onRemoteVideoLoaded(peerId)"></video>
              <div class="thumb-label">{{ peerNames[peerId] || peerId }}</div>
            </div>
            <div v-for="(stream, peerId) in remoteScreenStreams" :key="'screen-' + peerId" class="video-thumb screen-share" :class="{ active: focusedVideo === 'screen-' + peerId }" @click="focusedVideo = 'screen-' + peerId">
              <video :ref="el => setRemoteScreenVideoRef(peerId, el)" autoplay playsinline class="thumb-video"></video>
              <div class="thumb-label"><el-icon :size="12"><Monitor /></el-icon> {{ peerNames[peerId] || peerId }}</div>
            </div>
          </div>
        </div>

        <div v-else class="video-grid" :class="{ 'single-video': videoCount === 1 }">
          <div class="video-container" @click="focusedVideo = 'local'">
            <video :ref="el => { if (el && el.srcObject !== localStream) { el.srcObject = localStream; el.play().catch(()=>{}) } }" autoplay muted playsinline class="video-track"></video>
            <div class="video-label">
              <span>{{ userStore.user?.nickname || '我' }}</span>
              <span class="video-indicator" :class="{ off: isVideoOff }"></span>
            </div>
            <div v-if="isVideoOff" class="video-off-overlay">
              <el-avatar :size="64">{{ (userStore.user?.nickname || '我').charAt(0) }}</el-avatar>
            </div>
            <el-button class="fullscreen-btn-corner" @click.stop="requestFullscreen($event)" circle size="small">
              <el-icon :size="14"><FullScreen /></el-icon>
            </el-button>
          </div>
          <div v-if="localScreenStream" class="video-container screen-share" @click="focusedVideo = 'local-screen'">
            <video :ref="el => { if (el) el.srcObject = localScreenStream }" autoplay playsinline class="video-track"></video>
            <div class="video-label">
              <el-icon><Monitor /></el-icon>
              <span>{{ userStore.user?.nickname || '我' }} 的屏幕</span>
            </div>
            <el-button class="fullscreen-btn-corner" @click.stop="requestFullscreen($event)" circle size="small">
              <el-icon :size="14"><FullScreen /></el-icon>
            </el-button>
          </div>
          <div v-for="(stream, peerId) in remoteStreams" :key="peerId" class="video-container" @click="focusedVideo = peerId">
            <video :ref="el => setRemoteVideoRef(peerId, el)" autoplay playsinline class="video-track" @loadedmetadata="onRemoteVideoLoaded(peerId)"></video>
            <div class="video-label">{{ peerNames[peerId] || peerId }}</div>
            <el-button class="fullscreen-btn-corner" @click.stop="requestFullscreen($event)" circle size="small">
              <el-icon :size="14"><FullScreen /></el-icon>
            </el-button>
          </div>
          <div v-for="(stream, peerId) in remoteScreenStreams" :key="'screen-' + peerId" class="video-container screen-share" @click="focusedVideo = 'screen-' + peerId">
            <video :ref="el => setRemoteScreenVideoRef(peerId, el)" autoplay playsinline class="video-track"></video>
            <div class="video-label">
              <el-icon><Monitor /></el-icon>
              <span>{{ peerNames[peerId] || peerId }} 的屏幕</span>
            </div>
            <el-button class="fullscreen-btn-corner" @click.stop="requestFullscreen($event)" circle size="small">
              <el-icon :size="14"><FullScreen /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="meeting-controls">
          <div class="controls-left">
            <el-button :type="isMuted ? 'danger' : 'default'" @click="toggleMute" circle size="large">
              <el-icon :size="20"><component :is="isMuted ? 'Mute' : 'Microphone'" /></el-icon>
            </el-button>
            <el-button :type="isVideoOff ? 'danger' : 'default'" @click="toggleVideo" circle size="large">
              <el-icon :size="20"><component :is="isVideoOff ? 'VideoPause' : 'VideoCamera'" /></el-icon>
            </el-button>
            <el-button @click="shareScreen" circle size="large">
              <el-icon :size="20"><Monitor /></el-icon>
            </el-button>
            <el-button @click="focusedVideo = (focusedVideo ? null : 'local')" circle size="large">
              <el-icon :size="20"><component :is="focusedVideo ? 'Grid' : 'FullScreen'" /></el-icon>
            </el-button>
          </div>
          <div class="controls-right">
            <el-button type="danger" @click="handleLeaveMeeting" size="large" round>
              <el-icon><Close /></el-icon> 退出会议
            </el-button>
            <el-button v-if="isCreator" type="danger" @click="handleEndMeeting" size="large" round plain>
              <el-icon><Close /></el-icon> 结束会议
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Link, VideoCamera, VideoPause, Microphone, Monitor, Close, CopyDocument, Plus, User, Mute, FullScreen, Grid, Document, Clock, ArrowDown, Loading, Refresh, UploadFilled, Search, Notebook
} from '@element-plus/icons-vue'
import WebRTCService from '../utils/webrtc'
import { meetingApi } from '../api'
import { useUserStore } from '../store/user'
import { marked } from 'marked'

const webrtc = new WebRTCService()
const userStore = useUserStore()

// ==================== Tab state ====================
const activeTab = ref('video')

// 切换 tab 时自动刷新对应数据
watch(activeTab, (tab) => {
  if (tab === 'video') {
    fetchActiveMeetings()
  } else if (tab === 'records') {
    fetchMeetingRecords()
  } else if (tab === 'history') {
    fetchMeetingHistory()
  } else if (tab === 'minutes') {
    loadHistoryMeetingsForMinutes()
    if (selectedMeetingId.value) refreshTranscripts()
    fetchSummaryHistory()
  }
})

// ==================== Video meeting state ====================
const showCreateDialog = ref(false)
const showPasswordDialog = ref(false)
const showMeetingRoom = ref(false)
const creating = ref(false)
const joining = ref(false)
const isMuted = ref(false)
const isVideoOff = ref(false)
const localStream = ref(null)
const localScreenStream = ref(null)
const remoteStreams = reactive({})
const remoteScreenStreams = reactive({})
const remoteVideoRefs = {}
const remoteScreenVideoRefs = {}
const peerNames = reactive({})
const createFormRef = ref(null)
const activeMeetings = ref([])
const focusedVideo = ref(null)
const focusMainRef = ref(null)
const hasPendingSummary = ref(false)
const showMeetingTranscripts = ref(false)
const meetingTranscripts = ref([])
const meetingIsRecording = ref(false)
const meetingMediaRecorder = ref(null)
const meetingAudioChunks = ref([])
const meetingRecordingDuration = ref(0)
const meetingRecordingTimer = ref(null)

const createForm = reactive({ roomName: '', enablePassword: false, password: '', meetingType: 'NORMAL' })
const createRules = {
  roomName: [{ required: true, message: '请输入会议名称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入会议密码', trigger: 'blur' },
    { min: 4, max: 6, message: '密码长度为4-6位', trigger: 'blur' }
  ]
}

const joinForm = reactive({ roomName: '' })
const joinPassword = ref('')
const pendingJoinRoomName = ref('')

const currentMeeting = reactive({
  id: null, roomName: '', hasPassword: false, livekitRoomId: '', creatorId: null, meetingType: 'NORMAL'
})

const currentUserId = computed(() => userStore.user?.id)
const isCreator = computed(() => currentMeeting.creatorId && currentMeeting.creatorId === currentUserId.value)
const meetingLink = computed(() => {
  if (!currentMeeting.roomName) return ''
  return `${window.location.origin}/meetings?join=${encodeURIComponent(currentMeeting.roomName)}`
})
const videoCount = computed(() => {
  let count = 1
  if (localScreenStream.value) count++
  count += Object.keys(remoteStreams).length
  count += Object.keys(remoteScreenStreams).length
  return count
})

const participants = computed(() => {
  const localId = String(userStore.user?.id || '')
  const creatorId = currentMeeting.creatorId
  const seen = new Set()
  const list = []

  // Local user (always first candidate)
  if (userStore.user) {
    seen.add(localId)
    list.push({
      id: localId,
      nickname: userStore.user.nickname || userStore.user.username || '我',
      avatar: userStore.user.avatar || '',
      isSelf: true,
      isCreator: localId === String(creatorId)
    })
  }

  // Remote peers — skip if same ID as local user (deduplicate)
  for (const [peerId, name] of Object.entries(peerNames)) {
    if (seen.has(peerId)) continue
    seen.add(peerId)
    list.push({
      id: peerId,
      nickname: name || '用户 ' + peerId.slice(0, 6),
      avatar: '',
      isSelf: peerId === localId,
      isCreator: String(peerId) === String(creatorId)
    })
  }

  // Sort: creator first, then self, then others
  list.sort((a, b) => {
    if (a.isCreator !== b.isCreator) return a.isCreator ? -1 : 1
    if (a.isSelf !== b.isSelf) return a.isSelf ? -1 : 1
    return 0
  })

  return list
})

// ==================== Meeting history state ====================
const meetingHistory = ref([])
const historyLoading = ref(false)
const historySearch = ref('')
const expandedMeetingId = ref(null)
const historyDetail = ref(null)
const historyDetailLoading = ref(false)

const filteredHistory = computed(() => {
  if (!historySearch.value.trim()) return meetingHistory.value
  const keyword = historySearch.value.trim().toLowerCase()
  return meetingHistory.value.filter(m => m.roomName.toLowerCase().includes(keyword))
})

// ==================== AI minutes state ====================
const selectedMeetingId = ref(null)
const historyMeetingsForMinutes = ref([])
const showAddTranscript = ref(false)
const isRecording = ref(false)
const mediaRecorder = ref(null)
const audioChunks = ref([])
const recordingDuration = ref(0)
const recordingTimer = ref(null)
const uploadFile = ref(null)
const uploadRef = ref(null)
const audioTimestamp = ref('')
const uploading = ref(false)
const transcripts = ref([])
const transcriptsLoading = ref(false)
const generatingSummary = ref(false)
const summaryResult = ref(null)

const renderedSummary = computed(() => {
  if (!summaryResult.value?.summaryText) return ''
  return marked.parse(summaryResult.value.summaryText)
})

// ==================== Meeting records state ====================
const recordsLoading = ref(false)
const createdRecords = ref([])
const joinedRecords = ref([])
const recordTab = ref('created')
const recordSearch = ref('')

const filteredCreatedRecords = computed(() => {
  if (!recordSearch.value.trim()) return createdRecords.value
  const kw = recordSearch.value.trim().toLowerCase()
  return createdRecords.value.filter(m => m.roomName.toLowerCase().includes(kw))
})

const filteredJoinedRecords = computed(() => {
  if (!recordSearch.value.trim()) return joinedRecords.value
  const kw = recordSearch.value.trim().toLowerCase()
  return joinedRecords.value.filter(m => m.roomName.toLowerCase().includes(kw))
})

// ==================== Summary history state ====================
const summaryHistoryList = ref([])
const summaryHistoryLoading = ref(false)
const expandedSummaryId = ref(null)

const renderMarkdown = (text) => {
  if (!text) return ''
  return marked.parse(text)
}

// ==================== Helper functions ====================
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const formatDuration = (seconds) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// ==================== Video meeting functions ====================
const setRemoteVideoRef = (peerId, el) => {
  if (el) {
    remoteVideoRefs[peerId] = el
    if (remoteStreams[peerId] && el.srcObject !== remoteStreams[peerId]) {
      el.srcObject = remoteStreams[peerId]
      el.play().catch(() => {})
    }
  }
}

const setRemoteScreenVideoRef = (peerId, el) => {
  if (el) {
    remoteScreenVideoRefs[peerId] = el
    if (remoteScreenStreams[peerId] && el.srcObject !== remoteScreenStreams[peerId]) {
      el.srcObject = remoteScreenStreams[peerId]
      el.play().catch(() => {})
    }
  }
}

const onRemoteVideoLoaded = (peerId) => {
  const videoEl = remoteVideoRefs[peerId]
  if (videoEl && remoteStreams[peerId] && !videoEl.srcObject) {
    videoEl.srcObject = remoteStreams[peerId]
    videoEl.play().catch(() => {})
  }
}

const getVideoLabel = (videoId) => {
  if (videoId === 'local') return userStore.user?.nickname || '我'
  if (videoId === 'local-screen') return (userStore.user?.nickname || '我') + ' 的屏幕'
  if (videoId && videoId.startsWith('screen-')) {
    const pid = videoId.replace('screen-', '')
    return (peerNames[pid] || pid) + ' 的屏幕'
  }
  return peerNames[videoId] || videoId
}

const requestFullscreen = (event) => {
  const container = event.currentTarget.closest('.video-container')
  if (!container) return
  if (document.fullscreenElement) document.exitFullscreen()
  container.requestFullscreen().catch(() => {})
}

const enterFullscreen = () => {
  const el = focusMainRef.value
  if (!el) return
  if (document.fullscreenElement) document.exitFullscreen()
  el.requestFullscreen().catch(() => {})
}

const copyMeetingLink = async () => {
  try {
    await navigator.clipboard.writeText(meetingLink.value)
    ElMessage.success('会议链接已复制')
  } catch {
    ElMessage.info(`会议名称: ${currentMeeting.roomName}`)
  }
}

const fetchActiveMeetings = async () => {
  try {
    const res = await meetingApi.listActiveMeetings()
    activeMeetings.value = res.data || []
  } catch { /* ignore */ }
}

const openCreateDialog = () => {
  createForm.roomName = ''
  createForm.enablePassword = false
  createForm.password = ''
  createForm.meetingType = 'NORMAL'
  if (createFormRef.value) createFormRef.value.resetFields()
  showCreateDialog.value = true
}

const handleCreateMeeting = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      const password = createForm.enablePassword ? createForm.password : null
      const res = await meetingApi.createMeeting(createForm.roomName, password, createForm.meetingType)
      const meeting = res.data
      Object.assign(currentMeeting, {
        id: meeting.id, roomName: meeting.roomName, hasPassword: meeting.hasPassword,
        livekitRoomId: meeting.livekitRoomId, creatorId: meeting.creatorId,
        meetingType: meeting.meetingType || 'NORMAL'
      })
      showCreateDialog.value = false
      ElMessage.success('会议创建成功')
      await enterMeeting(currentMeeting.id, true)
    } catch (e) {
      ElMessage.error(e.response?.data?.message || e.message || '创建失败')
    } finally {
      creating.value = false
    }
  })
}

const extractRoomName = (input) => {
  const trimmed = input.trim()
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    try {
      const url = new URL(trimmed)
      const joinParam = url.searchParams.get('join')
      if (joinParam) return joinParam
    } catch {}
  }
  return trimmed
}

const handleJoinClick = async () => {
  const roomName = extractRoomName(joinForm.roomName)
  if (!roomName) { ElMessage.warning('请输入会议名称'); return }
  pendingJoinRoomName.value = roomName
  try {
    const res = await meetingApi.getMeeting(roomName)
    const meeting = res.data
    if (meeting.hasPassword && meeting.creatorId !== userStore.user?.id) {
      showPasswordDialog.value = true
      joinPassword.value = ''
    } else {
      await doJoin(roomName, null)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '会议不存在或已结束')
  }
}

const handleJoinWithPassword = async () => {
  if (!joinPassword.value) { ElMessage.warning('请输入会议密码'); return }
  await doJoin(pendingJoinRoomName.value, joinPassword.value)
}

const handleJoinFromList = async (meeting) => {
  pendingJoinRoomName.value = meeting.roomName
  if (meeting.hasPassword && meeting.creatorId !== userStore.user?.id) {
    showPasswordDialog.value = true
    joinPassword.value = ''
  } else {
    await doJoin(meeting.roomName, null)
  }
}

const doJoin = async (roomName, password) => {
  joining.value = true
  try {
    const res = await meetingApi.joinMeeting(roomName, password)
    const meeting = res.data
    Object.assign(currentMeeting, {
      id: meeting.id, roomName: meeting.roomName, hasPassword: false,
      livekitRoomId: meeting.livekitRoomId, creatorId: meeting.creatorId,
      meetingType: meeting.meetingType || 'NORMAL'
    })
    showPasswordDialog.value = false
    joinForm.roomName = ''
    ElMessage.success('已加入会议')
    await enterMeeting(currentMeeting.id, false)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加入失败')
  } finally {
    joining.value = false
  }
}

const enterMeeting = async (meetingId, isOwner) => {
  try {
    showMeetingRoom.value = true
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    localStream.value = stream
    await nextTick()
    await webrtc.connect()

    webrtc.onPeerJoined = (peerId, nickname) => {
      if (nickname && !peerNames[peerId]) peerNames[peerId] = nickname
    }

    webrtc.onStreamReceived = (remoteStream, peerId) => {
      if (!peerNames[peerId]) peerNames[peerId] = '用户 ' + peerId.slice(0, 6)
      remoteStreams[peerId] = remoteStream
      let attempts = 0
      const attach = () => {
        const el = remoteVideoRefs[peerId]
        if (el) { el.srcObject = remoteStream; el.play().catch(() => {}) }
        else if (attempts < 30) { attempts++; setTimeout(attach, 100) }
      }
      nextTick(attach)
    }

    webrtc.onScreenShareReceived = (screenStream, peerId) => {
      remoteScreenStreams[peerId] = screenStream
      nextTick(() => {
        const el = remoteScreenVideoRefs[peerId]
        if (el) { el.srcObject = screenStream; el.play().catch(() => {}) }
      })
    }

    webrtc.onScreenShareRemoved = (peerId) => { delete remoteScreenStreams[peerId] }
    webrtc.onStreamRemoved = (peerId) => {
      delete remoteStreams[peerId]
      delete remoteScreenStreams[peerId]
      delete peerNames[peerId]
    }

    webrtc.onSummaryCompleted = (data) => {
      if (data.type === 'summary-completed') {
        ElMessage.success({ message: data.message || '会议纪要已生成', duration: 6000, showClose: true })
        hasPendingSummary.value = true
      } else if (data.type === 'summary-failed') {
        ElMessage.error(data.message || '会议纪要生成失败')
      }
    }

    const nickname = userStore.user?.nickname || userStore.user?.username || ''
    await webrtc.enterRoom(meetingId, String(userStore.user.id), stream, nickname)
    isMuted.value = false
    isVideoOff.value = false

    // AI智能会议自动开始录音
    if (currentMeeting.meetingType === 'AI_SMART') {
      startMeetingRecording()
    }
  } catch (e) {
    console.error('[Meeting] Failed to enter:', e)
    ElMessage.error('无法访问摄像头/麦克风，请检查权限设置')
    showMeetingRoom.value = false
    throw e
  }
}

const toggleMute = () => {
  if (localStream.value) {
    const audioTrack = localStream.value.getAudioTracks()[0]
    if (audioTrack) { isMuted.value = !isMuted.value; audioTrack.enabled = !isMuted.value }
  }
}

const toggleVideo = () => {
  if (localStream.value) {
    const videoTrack = localStream.value.getVideoTracks()[0]
    if (videoTrack) { isVideoOff.value = !isVideoOff.value; videoTrack.enabled = !isVideoOff.value }
  }
}

const shareScreen = async () => {
  try {
    const screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false })
    localScreenStream.value = screenStream
    webrtc.startScreenShare(screenStream)
    screenStream.getVideoTracks()[0].onended = () => { stopLocalScreenShare() }
    ElMessage.success('屏幕共享已开启')
  } catch { /* user cancelled */ }
}

const stopLocalScreenShare = () => {
  webrtc.stopScreenShare()
  if (localScreenStream.value) { localScreenStream.value.getTracks().forEach(t => t.stop()) }
  localScreenStream.value = null
  ElMessage.info('屏幕共享已停止')
}

const handleEndMeeting = async () => {
  try {
    await ElMessageBox.confirm('结束会议后，所有参会者将被移出。确定要结束吗？', '结束会议', {
      confirmButtonText: '确定结束', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  // Stop recording and upload first so transcripts are saved before auto-summary
  await leaveMeeting()
  try {
    await meetingApi.endMeeting(currentMeeting.id)
    ElMessage.success('会议已结束')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
  showMeetingRoom.value = false
  fetchActiveMeetings()
}

const handleLeaveMeeting = () => {
  ElMessageBox.confirm('确定要退出会议吗？', '退出会议', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    await leaveMeeting()
    showMeetingRoom.value = false
    fetchActiveMeetings()
  }).catch(() => {})
}

const leaveMeeting = async () => {
  // 停止录音并上传
  await stopMeetingRecordingAndUpload()

  webrtc.stop()
  if (localStream.value) { localStream.value.getTracks().forEach(track => track.stop()); localStream.value = null }
  localScreenStream.value = null
  Object.keys(remoteStreams).forEach(k => delete remoteStreams[k])
  Object.keys(remoteScreenStreams).forEach(k => delete remoteScreenStreams[k])
  Object.keys(peerNames).forEach(k => delete peerNames[k])
  focusedVideo.value = null
  showMeetingTranscripts.value = false
  meetingTranscripts.value = []
}

const startMeetingRecording = () => {
  if (!localStream.value) return
  try {
    const audioTrack = localStream.value.getAudioTracks()[0]
    if (!audioTrack) return
    const audioStream = new MediaStream([audioTrack])
    const recorder = new MediaRecorder(audioStream, {
      mimeType: MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/mp4'
    })
    meetingMediaRecorder.value = recorder
    meetingAudioChunks.value = []

    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) meetingAudioChunks.value.push(e.data)
    }

    recorder.start(5000) // 每5秒保存一个chunk, 避免丢失
    meetingIsRecording.value = true
    meetingRecordingDuration.value = 0
    meetingRecordingTimer.value = setInterval(() => { meetingRecordingDuration.value++ }, 1000)
  } catch (e) {
    console.error('[Meeting] Failed to start recording:', e)
  }
}

const stopMeetingRecordingAndUpload = async () => {
  const recorder = meetingMediaRecorder.value
  if (!recorder || recorder.state === 'inactive') return

  return new Promise((resolve) => {
    recorder.onstop = async () => {
      meetingIsRecording.value = false
      if (meetingRecordingTimer.value) { clearInterval(meetingRecordingTimer.value); meetingRecordingTimer.value = null }

      if (meetingAudioChunks.value.length === 0) { resolve(); return }

      try {
        const blob = new Blob(meetingAudioChunks.value, { type: recorder.mimeType })
        const ext = recorder.mimeType.includes('webm') ? 'webm' : 'mp4'
        const file = new File([blob], `meeting_${currentMeeting.id}_${Date.now()}.${ext}`, { type: recorder.mimeType })

        const timestamp = formatDuration(meetingRecordingDuration.value)
        await meetingApi.uploadTranscript(currentMeeting.id, file, timestamp)

        // Refresh transcripts in the meeting room
        await refreshMeetingTranscripts()
        ElMessage.success('语音记录已自动保存')
      } catch (e) {
        console.error('[Meeting] Failed to upload recording:', e)
        ElMessage.error('语音记录上传失败')
      }
      meetingMediaRecorder.value = null
      meetingAudioChunks.value = []
      meetingRecordingDuration.value = 0
      resolve()
    }
    recorder.stop()
    // Stop all tracks in the recording stream
    if (recorder.stream) {
      recorder.stream.getTracks().forEach(t => t.stop())
    }
  })
}

const refreshMeetingTranscripts = async () => {
  if (!currentMeeting.id) return
  try {
    const res = await meetingApi.getTranscripts(currentMeeting.id)
    meetingTranscripts.value = res.data || []
  } catch { /* ignore */ }
}

// ==================== Meeting history functions ====================
// Tab switching is now handled by the watch on activeTab — it auto-refreshes data

const fetchMeetingRecords = async () => {
  recordsLoading.value = true
  try {
    const res = await meetingApi.getMeetingRecords()
    createdRecords.value = res.data?.created || []
    joinedRecords.value = res.data?.joined || []
  } catch {
    ElMessage.error('获取会议记录失败')
  } finally {
    recordsLoading.value = false
  }
}

const fetchSummaryHistory = async () => {
  summaryHistoryLoading.value = true
  try {
    const res = await meetingApi.getSummaryHistory()
    summaryHistoryList.value = res.data || []
  } catch {
    ElMessage.error('获取纪要记录失败')
  } finally {
    summaryHistoryLoading.value = false
  }
}

const toggleSummaryDetail = (item) => {
  if (expandedSummaryId.value === item.id) {
    expandedSummaryId.value = null
  } else {
    expandedSummaryId.value = item.id
  }
}

const fetchMeetingHistory = async () => {
  historyLoading.value = true
  try {
    const res = await meetingApi.getMeetingHistory()
    meetingHistory.value = res.data || []
  } catch {
    ElMessage.error('获取会议历史失败')
  } finally {
    historyLoading.value = false
  }
}

const toggleHistoryDetail = async (meetingId) => {
  if (expandedMeetingId.value === meetingId) {
    expandedMeetingId.value = null
    historyDetail.value = null
    return
  }
  expandedMeetingId.value = meetingId
  historyDetailLoading.value = true
  try {
    const res = await meetingApi.getMeetingHistoryDetail(meetingId)
    historyDetail.value = res.data
  } catch {
    ElMessage.error('获取会议详情失败')
  } finally {
    historyDetailLoading.value = false
  }
}

// ==================== AI minutes functions ====================
const loadHistoryMeetingsForMinutes = async () => {
  try {
    const res = await meetingApi.getMeetingHistory()
    historyMeetingsForMinutes.value = res.data || []
  } catch { /* ignore */ }
}

const onMinutesMeetingChange = async (meetingId) => {
  if (!meetingId) return
  transcriptsLoading.value = true
  summaryResult.value = null
  showAddTranscript.value = false
  try {
    const res = await meetingApi.getTranscripts(meetingId)
    transcripts.value = res.data || []
  } catch {
    transcripts.value = []
  } finally {
    transcriptsLoading.value = false
  }
}

const refreshTranscripts = async () => {
  if (!selectedMeetingId.value) return
  try {
    const res = await meetingApi.getTranscripts(selectedMeetingId.value)
    transcripts.value = res.data || []
  } catch { /* ignore */ }
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const recorder = new MediaRecorder(stream, { mimeType: MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/mp4' })
    mediaRecorder.value = recorder
    audioChunks.value = []

    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.value.push(e.data)
    }

    recorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop())
      const blob = new Blob(audioChunks.value, { type: recorder.mimeType })
      const ext = recorder.mimeType.includes('webm') ? 'webm' : 'mp4'
      const file = new File([blob], `recording_${Date.now()}.${ext}`, { type: recorder.mimeType })
      await uploadAndTranscribe(file)
    }

    recorder.start()
    isRecording.value = true
    recordingDuration.value = 0
    recordingTimer.value = setInterval(() => { recordingDuration.value++ }, 1000)
    ElMessage.success('开始录音')
  } catch (e) {
    ElMessage.error('无法访问麦克风，请检查权限设置')
  }
}

const stopRecording = () => {
  if (mediaRecorder.value && mediaRecorder.value.state === 'recording') {
    mediaRecorder.value.stop()
    isRecording.value = false
    if (recordingTimer.value) { clearInterval(recordingTimer.value); recordingTimer.value = null }
    ElMessage.info('录音已停止，正在转写...')
  }
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const handleFileRemove = () => {
  uploadFile.value = null
}

const uploadAndTranscribe = async (file) => {
  uploading.value = true
  try {
    const res = await meetingApi.uploadTranscript(selectedMeetingId.value, file, audioTimestamp.value || null)
    ElMessage.success('语音转文本保存成功')
    // Refresh from server to ensure consistent data (avoid stale push)
    await refreshTranscripts()
    audioTimestamp.value = ''
    uploadFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '语音转文本失败')
  } finally {
    uploading.value = false
  }
}

const uploadAudio = () => {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择音频文件')
    return
  }
  uploadAndTranscribe(uploadFile.value)
}

const handleGenerateFromTranscripts = async () => {
  if (!selectedMeetingId.value) {
    ElMessage.warning('请先选择会议')
    return
  }
  if (transcripts.value.length === 0) {
    ElMessage.warning('该会议没有语音记录，请先添加')
    return
  }
  generatingSummary.value = true
  try {
    const res = await meetingApi.generateSummaryFromTranscripts(selectedMeetingId.value)
    summaryResult.value = res.data
    ElMessage.success('会议纪要生成成功')
    // Refresh summary history so the merged tab shows the new summary
    fetchSummaryHistory()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '生成失败')
  } finally {
    generatingSummary.value = false
  }
}

const handleRegenerateSummary = () => {
  summaryResult.value = null
}

const copySummary = async () => {
  try {
    await navigator.clipboard.writeText(summaryResult.value.summaryText)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

// ==================== Lifecycle ====================
onMounted(() => {
  fetchActiveMeetings()
  const params = new URLSearchParams(window.location.search)
  const joinRoom = params.get('join')
  if (joinRoom) {
    joinForm.roomName = joinRoom
    handleJoinClick()
  }
})

onUnmounted(() => {
  webrtc.stop()
  if (recordingTimer.value) clearInterval(recordingTimer.value)
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background: #fff;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

/* ==================== Tab bar ==================== */
.tab-bar {
  display: flex;
  gap: 0;
  border-bottom: 2px solid #E8ECEF;
  margin-bottom: 20px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  font-size: 14px;
  font-weight: 500;
  color: #909399;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
  user-select: none;
}

.tab-item:hover {
  color: #3390EC;
}

.tab-item.active {
  color: #3390EC;
  border-bottom-color: #3390EC;
}

.tab-item .el-icon {
  font-size: 16px;
}

/* ==================== Page body ==================== */
.page-body {
  max-width: 900px;
}

.join-section { margin-bottom: 24px; }

.join-input-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.join-input-group :deep(.el-input__wrapper) {
  border-radius: 16px;
  background: #F4F4F5;
  box-shadow: none;
  border: none;
}

.join-input-group :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px #3390EC;
  background: #fff;
}

.join-btn {
  border-radius: 16px;
  flex-shrink: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #222;
  margin-bottom: 12px;
}

.meeting-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meeting-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 14px;
  border: 1px solid #E8ECEF;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
}

.meeting-card:hover {
  background: #F8FAFC;
  border-color: #3390EC;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.meeting-card-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #E3F2FD, #BBDEFB);
  border-radius: 12px;
  color: #1565C0;
}

.meeting-card-info { flex: 1; }

.meeting-card-name {
  font-weight: 600;
  font-size: 15px;
  color: #222;
  margin-bottom: 4px;
}

.meeting-card-meta { display: flex; gap: 8px; }

/* ==================== Meeting history ==================== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
  color: #909399;
  gap: 8px;
}

.empty-state {
  padding: 40px 0;
}

.search-box {
  margin-bottom: 16px;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 12px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-card {
  border: 1px solid #E8ECEF;
  border-radius: 14px;
  overflow: hidden;
  transition: all 0.15s;
}

.history-card:hover {
  border-color: #3390EC;
}

.history-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  cursor: pointer;
  background: #fff;
  transition: background 0.15s;
}

.history-card-header:hover {
  background: #F8FAFC;
}

.history-card-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #E8F5E9, #C8E6C9);
  border-radius: 12px;
  color: #2E7D32;
}

.history-card-info { flex: 1; }

.history-card-name {
  font-weight: 600;
  font-size: 15px;
  color: #222;
  margin-bottom: 4px;
}

.history-card-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.expand-icon {
  transition: transform 0.2s;
  color: #909399;
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

.history-detail {
  border-top: 1px solid #E8ECEF;
  background: #FAFBFC;
}

.history-detail-header {
  padding: 10px 18px;
  font-size: 13px;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}

/* ==================== AI Minutes ==================== */
.minutes-section {
  margin-bottom: 24px;
}

.recorder-box {
  padding: 24px;
  border: 2px dashed #E8ECEF;
  border-radius: 14px;
  text-align: center;
}

.recorder-idle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.recorder-hint {
  font-size: 13px;
  color: #909399;
}

.recorder-active {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #f56c6c;
  font-weight: 500;
}

.recording-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #f56c6c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

.upload-box {
  padding: 16px;
  border: 1px solid #E8ECEF;
  border-radius: 14px;
}

.transcript-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.transcript-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transcript-item {
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #FAFBFC;
}

.transcript-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.transcript-speaker {
  font-weight: 600;
  font-size: 13px;
  color: #3390EC;
}

.transcript-time {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 4px;
}

.transcript-date {
  font-size: 11px;
  color: #c0c4cc;
  margin-left: auto;
}

.transcript-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
}

/* ==================== Meeting Room ==================== */
.meeting-room {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* Override el-dialog body padding to make top bar flush with edges */
:deep(.el-dialog__body) {
  padding: 0;
}
:deep(.el-dialog__header) {
  display: none;
}

.meeting-top-bar {
  padding: 10px 24px;
  background: #F0F1F3;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  flex-shrink: 0;
  width: 100%;
}

.meeting-top-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.room-name-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.meeting-top-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meeting-top-right :deep(.el-button--text) {
  color: #606266;
}
.meeting-top-right :deep(.el-button--text):hover {
  color: #3390EC;
}

.participants-dropdown {
  cursor: pointer;
}

.participant-count {
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.participant-count:hover {
  background: rgba(0,0,0,0.06);
}

.participant-dropdown-item {
  display: flex;
  align-items: center;
  padding: 6px 12px !important;
  min-width: 160px;
}

.meeting-link-inline {
  background: #fff;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  border: 1px solid #dcdfe6;
  color: #606266;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 24px;
}

.copy-link-btn {
  flex-shrink: 0;
}


.meeting-recording-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #f56c6c;
  font-weight: 500;
}

.meeting-transcripts-panel {
  padding: 12px 24px;
  background: #FAFBFC;
  border-bottom: 1px solid #E8ECEF;
  max-height: 260px;
  overflow-y: auto;
}

.video-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 10px;
  padding: 8px;
  overflow-y: auto;
  background: #fff;
}

.video-grid.single-video {
  grid-template-columns: 1fr;
  max-width: 700px;
  margin: 0 auto;
}

.video-container {
  position: relative;
  background: #1a1a2e;
  border-radius: 14px;
  overflow: hidden;
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.video-track {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-label {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.video-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4CAF50;
}

.video-indicator.off {
  background: #f44336;
}

.video-off-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 46, 0.9);
}

.meeting-controls {
  padding: 16px 24px;
  background: #F8FAFC;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.controls-left { display: flex; gap: 12px; }
.controls-right { display: flex; gap: 8px; }
.video-container.screen-share { border: 2px solid #4CAF50; }

.fullscreen-btn-corner {
  position: absolute;
  bottom: 12px;
  right: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  background: rgba(0, 0, 0, 0.5) !important;
  border-color: transparent !important;
  color: #fff !important;
}
.video-container:hover .fullscreen-btn-corner { opacity: 1; }

.focus-layout {
  flex: 1;
  display: flex;
  gap: 10px;
  overflow: hidden;
  min-height: 0;
}

.focus-sidebar {
  width: 180px;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px;
}

.video-thumb {
  position: relative;
  background: #1a1a2e;
  border-radius: 10px;
  overflow: hidden;
  min-height: 100px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}
.video-thumb:hover { border-color: #666; }
.video-thumb.active { border-color: #3390EC; }
.video-thumb.screen-share { border-color: #4CAF50; }

.thumb-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  min-height: 100px;
}

.thumb-label {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: calc(100% - 8px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-off-overlay.small {
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 46, 0.9);
  position: absolute;
}

.focus-main {
  flex: 1;
  position: relative;
  background: #1a1a2e;
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.focus-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.focus-label {
  position: absolute;
  bottom: 16px;
  left: 16px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 14px;
}

.fullscreen-btn {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: rgba(0, 0, 0, 0.5) !important;
  border-color: transparent !important;
  color: #fff !important;
}

/* AI Summary */
.summary-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.summary-content {
  max-height: 55vh;
  overflow-y: auto;
  padding: 20px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
}

.summary-content :deep(h1) { font-size: 22px; font-weight: 700; margin: 18px 0 10px; color: #1d1d1f; border-bottom: 2px solid #409EFF; padding-bottom: 8px; }
.summary-content :deep(h2) { font-size: 17px; font-weight: 600; margin: 16px 0 8px; color: #303133; }
.summary-content :deep(h3) { font-size: 15px; font-weight: 600; margin: 12px 0 6px; }
.summary-content :deep(p) { margin: 6px 0; }
.summary-content :deep(ul), .summary-content :deep(ol) { padding-left: 22px; margin: 8px 0; }
.summary-content :deep(li) { margin: 4px 0; }
.summary-content :deep(table) { width: 100%; border-collapse: collapse; margin: 12px 0; }
.summary-content :deep(th), .summary-content :deep(td) { border: 1px solid #dcdfe6; padding: 8px 12px; text-align: left; }
.summary-content :deep(th) { background: #f5f7fa; font-weight: 600; }
.summary-content :deep(blockquote) { border-left: 3px solid #409EFF; padding-left: 12px; color: #606266; margin: 8px 0; }
.summary-content :deep(hr) { border: none; border-top: 1px solid #ebeef5; margin: 16px 0; }
.summary-content :deep(strong) { color: #1d1d1f; }

/* ==================== Meeting Records ==================== */
.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-card {
  border: 1px solid #E8ECEF;
  border-radius: 12px;
  padding: 16px 20px;
  background: #fff;
  transition: border-color 0.2s;
}
.record-card:hover {
  border-color: #3390EC;
}

.record-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.record-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-room-name {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}

.record-duration {
  font-size: 13px;
  color: #3390EC;
  font-weight: 500;
}

.record-card-body {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 6px;
}

.record-meta {
  font-size: 13px;
  color: #909399;
  margin-right: 12px;
  line-height: 28px;
}

.records-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.records-tabs {
  display: flex;
  gap: 0;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.record-tab-btn {
  padding: 8px 20px;
  border: none;
  background: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  outline: none;
}
.record-tab-btn:hover {
  color: #3390EC;
  background: #f0f7ff;
}
.record-tab-btn.active {
  background: #3390EC;
  color: #fff;
}

.record-participants-link {
  font-size: 13px;
  color: #3390EC;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}
.record-participants-link:hover {
  background: rgba(51,144,236,0.08);
}
</style>
