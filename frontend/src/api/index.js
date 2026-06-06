import api from './request'

export const authApi = {
  login(data) {
    return api.post('/auth/login', data)
  },

  logout() {
    return api.post('/auth/logout')
  }
}

export const userApi = {
  getCurrentUser() {
    return api.get('/users/me')
  },

  updateProfile(data) {
    return api.put('/users/me', data)
  },

  searchUsers(nickname) {
    return api.get('/users/search', { params: { nickname } })
  },

  getUserById(id) {
    return api.get(`/users/${id}`)
  }
}

export const teamApi = {
  createTeam(name) {
    return api.post('/teams', { name })
  },

  addMember(teamId, userId) {
    return api.post(`/teams/${teamId}/members`, { userId })
  },

  updateMemberRole(teamId, userId, role) {
    return api.put(`/teams/${teamId}/members/${userId}/role`, { role })
  },

  removeMember(teamId, userId) {
    return api.delete(`/teams/${teamId}/members/${userId}`)
  },

  getTeamMembers(teamId) {
    return api.get(`/teams/${teamId}/members`)
  },

  getMyTeams() {
    return api.get('/teams/my-teams')
  },

  deleteTeam(teamId) {
    return api.delete(`/teams/${teamId}`)
  }
}

export const chatApi = {
  createPrivateSession(targetUserId) {
    return api.post('/chat/private', null, { params: { targetUserId } })
  },

  getPrivateSessions() {
    return api.get('/chat/private/sessions')
  },

  getTeamSession(teamId) {
    return api.get(`/chat/teams/${teamId}/session`)
  },

  getMessages(sessionId, keyword) {
    const params = keyword ? { keyword } : {}
    return api.get(`/chat/sessions/${sessionId}/messages`, { params })
  },

  sendMessage(sessionId, content, messageType = 'TEXT', fileUrl, fileName, fileSize) {
    return api.post(`/chat/sessions/${sessionId}/messages`, { content, messageType, fileUrl, fileName, fileSize })
  },

  uploadFile(sessionId, file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/chat/sessions/${sessionId}/upload`, formData)
  },

  getPinnedMessages(sessionId) {
    return api.get(`/chat/sessions/${sessionId}/pinned`)
  },

  pinMessage(messageId) {
    return api.put(`/chat/messages/${messageId}/pin`)
  },

  unpinMessage(messageId) {
    return api.put(`/chat/messages/${messageId}/unpin`)
  }
}

export const meetingApi = {
  createMeeting(roomName, password, meetingType) {
    return api.post('/meetings', { roomName, password, meetingType })
  },

  joinMeeting(roomName, password) {
    return api.post('/meetings/join', { roomName, password })
  },

  getMeeting(roomName) {
    return api.get(`/meetings/room/${encodeURIComponent(roomName)}`)
  },

  endMeeting(meetingId) {
    return api.post(`/meetings/${meetingId}/end`)
  },

  listActiveMeetings() {
    return api.get('/meetings')
  },

  getToken(roomName, userName) {
    return api.post('/meetings/token', { roomName, userName })
  },

  generateSummary(meetingId, transcriptText) {
    return api.post(`/meetings/${meetingId}/summary`, { transcriptText })
  },

  getMeetingSummary(meetingId) {
    return api.get(`/meetings/${meetingId}/summary`)
  },

  uploadTranscript(meetingId, file, timestamp) {
    const formData = new FormData()
    formData.append('file', file)
    if (timestamp) formData.append('timestamp', timestamp)
    return api.post(`/meetings/${meetingId}/transcripts`, formData)
  },

  getTranscripts(meetingId) {
    return api.get(`/meetings/${meetingId}/transcripts`)
  },

  getMeetingHistory() {
    return api.get('/meetings/history')
  },

  getMeetingHistoryDetail(meetingId) {
    return api.get(`/meetings/${meetingId}/history`)
  },

  generateSummaryFromTranscripts(meetingId) {
    return api.post(`/meetings/${meetingId}/transcripts/generate-summary`)
  },

  getSummaryHistory() {
    return api.get('/meetings/summaries/history')
  },

  getMeetingRecords() {
    return api.get('/meetings/records')
  }
}

export const calendarApi = {
  getTodos(date) {
    return api.get('/calendar/todos', { params: { date } })
  },

  getMonth(year, month) {
    return api.get('/calendar/month', { params: { year, month } })
  },

  addTodo(date, content) {
    return api.post('/calendar/todos', { date, content })
  },

  updateTodo(id, content) {
    return api.put(`/calendar/todos/${id}`, { content })
  },

  toggleTodo(id) {
    return api.put(`/calendar/todos/${id}/toggle`)
  },

  deleteTodo(id) {
    return api.delete(`/calendar/todos/${id}`)
  }
}
