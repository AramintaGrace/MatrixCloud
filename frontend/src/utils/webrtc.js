import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

class WebRTCService {
  constructor() {
    this.stompClient = null
    this.peerConnections = new Map()
    this.localStream = null
    this.screenStream = null
    this.roomId = null
    this.userId = null
    this.nickname = ''
    this.subscriptions = []
    this.onStreamReceived = null
    this.onStreamRemoved = null
    this.onScreenShareReceived = null
    this.onScreenShareRemoved = null
    this.onPeerJoined = null
    this.onSummaryCompleted = null
    this.negotiatingPeers = new Set()
    this.screenSharingPeers = new Set()

    this.iceServers = [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ]
  }

  connect() {
    return new Promise((resolve, reject) => {
      const socket = new SockJS('/ws/signaling')

      this.stompClient = new Client({
        webSocketFactory: () => socket,
        debug: () => {},
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          resolve()
        },
        onStompError: (frame) => {
          reject(new Error('STOMP connection failed'))
        }
      })

      this.stompClient.activate()
    })
  }

  disconnect() {
    this.cleanupSubscriptions()
    if (this.stompClient) {
      this.stompClient.deactivate()
    }
    this.closeAllConnections()
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop())
      this.localStream = null
    }
    if (this.screenStream) {
      this.screenStream.getTracks().forEach(track => track.stop())
      this.screenStream = null
    }
  }

  closeAllConnections() {
    this.peerConnections.forEach(pc => {
      try { pc.close() } catch {}
    })
    this.peerConnections.clear()
    this.negotiatingPeers.clear()
    this._peersWithStream.clear()
    this.screenSharingPeers.clear()
  }

  async enterRoom(roomId, userId, stream, nickname) {
    this.cleanupSubscriptions()
    this.closeAllConnections()

    this.roomId = String(roomId)
    this.userId = String(userId)
    this.nickname = nickname || ''
    this.localStream = stream

    this.subscribeToRoom()

    this.stompClient.publish({
      destination: '/app/webrtc/join',
      body: JSON.stringify({ roomId, userId, role: 'participant', nickname: this.nickname })
    })
  }

  cleanupSubscriptions() {
    this.subscriptions.forEach(sub => {
      try { sub.unsubscribe() } catch {}
    })
    this.subscriptions = []
  }

  publishToRoom(topic, body) {
    if (!this.stompClient || !this.stompClient.connected) return
    this.stompClient.publish({
      destination: `/app/webrtc/${topic}`,
      body: JSON.stringify(body)
    })
  }

  subscribeToRoom() {
    // Handle offer from another peer
    const offerSub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/offer`, async (message) => {
        const data = JSON.parse(message.body)
        if (data.from !== this.userId) {
          await this.handleOffer(data)
        }
      })
    this.subscriptions.push(offerSub)

    // Handle answer from another peer
    const answerSub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/answer`, async (message) => {
        const data = JSON.parse(message.body)
        await this.handleAnswer(data)
      })
    this.subscriptions.push(answerSub)

    // Handle ICE candidate
    const iceSub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/ice-candidate`, async (message) => {
        const data = JSON.parse(message.body)
        if (data.from !== this.userId) {
          await this.handleIceCandidate(data)
        }
      })
    this.subscriptions.push(iceSub)

    // A new user joined the room - create offer to them
    const joinSub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/user-joined`, async (message) => {
        const data = JSON.parse(message.body)
        if (data.userId !== this.userId) {
          if (this.onPeerJoined) {
            this.onPeerJoined(data.userId, data.nickname || '')
          }
          // Small random delay to avoid both peers creating offers simultaneously
          await new Promise(r => setTimeout(r, 50 + Math.random() * 100))
          if (!this.negotiatingPeers.has(data.userId)) {
            await this.createOffer(data.userId)
          }
        }
      })
    this.subscriptions.push(joinSub)

    // A user left the room
    const leaveSub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/user-left`, (message) => {
        const data = JSON.parse(message.body)
        this.screenSharingPeers.delete(data.userId)
        if (this.onScreenShareRemoved) {
          this.onScreenShareRemoved(data.userId)
        }
        this.removePeer(data.userId)
      })
    this.subscriptions.push(leaveSub)

    // Screen share started
    const screenStartSub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/screen-share-started`, (message) => {
        const data = JSON.parse(message.body)
        if (data.from !== this.userId) {
          this.screenSharingPeers.add(data.from)
        }
      })
    this.subscriptions.push(screenStartSub)

    // Screen share stopped
    const screenStopSub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/screen-share-stopped`, (message) => {
        const data = JSON.parse(message.body)
        this.screenSharingPeers.delete(data.from)
        this._peersWithStream.delete(data.from)
        if (this.onScreenShareRemoved) {
          this.onScreenShareRemoved(data.from)
        }
      })
    this.subscriptions.push(screenStopSub)

    // AI summary completed notification
    const summarySub = this.stompClient.subscribe(
      `/topic/room/${this.roomId}/summary-completed`, (message) => {
        const data = JSON.parse(message.body)
        if (this.onSummaryCompleted) {
          this.onSummaryCompleted(data)
        }
      })
    this.subscriptions.push(summarySub)
  }

  removePeer(peerId) {
    ['_send', '_recv'].forEach(suffix => {
      const pc = this.peerConnections.get(peerId + suffix)
      if (pc) {
        try { pc.close() } catch {}
        this.peerConnections.delete(peerId + suffix)
      }
    })
    this.negotiatingPeers.delete(peerId)
    this._peersWithStream.delete(peerId)
    this.screenSharingPeers.delete(peerId)
    if (this.onStreamRemoved) {
      this.onStreamRemoved(peerId)
    }
    if (this.onScreenShareRemoved) {
      this.onScreenShareRemoved(peerId)
    }
  }

  createPeerConnection(peerId, isRecv) {
    const key = isRecv ? peerId + '_recv' : peerId + '_send'
    let pc = this.peerConnections.get(key)
    if (pc && pc.connectionState !== 'failed' && pc.connectionState !== 'closed') {
      return pc
    }

    pc = new RTCPeerConnection({ iceServers: this.iceServers })

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        this.publishToRoom('ice-candidate', {
          roomId: this.roomId, from: this.userId, to: peerId, candidate: event.candidate
        })
      }
    }

    pc.ontrack = (event) => {
      if (event.track.kind !== 'video') return

      let stream = event.streams[0]
      if (!stream) {
        stream = new MediaStream([event.track])
      }

      if (this.screenSharingPeers.has(peerId)) {
        if (this.onScreenShareReceived) {
          this.onScreenShareReceived(stream, peerId)
        }
      } else {
        this._markPeerHasStream(peerId)
        if (this.onStreamReceived) {
          this.onStreamReceived(stream, peerId)
        }
      }
    }

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
        // Don't remove on 'disconnected' as it may recover
        if (pc.connectionState === 'failed') {
          this.removePeer(peerId)
        }
      }
    }

    // Add local tracks only to send connections
    if (!isRecv && this.localStream) {
      this.localStream.getTracks().forEach(track => {
        try { pc.addTrack(track, this.localStream) } catch {}
      })
    }

    this.peerConnections.set(key, pc)
    return pc
  }

  async createOffer(peerId) {
    if (this.negotiatingPeers.has(peerId)) return
    this.negotiatingPeers.add(peerId)

    try {
      const pc = this.createPeerConnection(peerId, false)
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)

      this.publishToRoom('offer', {
        roomId: this.roomId, from: this.userId, to: peerId, offer, nickname: this.nickname
      })
    } catch (e) {
      this.negotiatingPeers.delete(peerId)
    }
  }

  async handleOffer(data) {
    const peerId = data.from
    this.negotiatingPeers.add(peerId)

    // Learn the offerer's nickname if we don't have it yet
    if (data.nickname && this.onPeerJoined) {
      this.onPeerJoined(peerId, data.nickname)
    }

    try {
      const pc = this.createPeerConnection(peerId, true)
      await pc.setRemoteDescription(new RTCSessionDescription(data.offer))
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)

      this.publishToRoom('answer', {
        roomId: this.roomId, from: this.userId, to: peerId, answer, nickname: this.nickname
      })

      // Create reciprocal offer for bidirectional media
      if (!this.peerConnections.has(peerId + '_send')) {
        this.negotiatingPeers.delete(peerId)
        await this.createOffer(peerId)
      }
    } catch (e) {
      this.negotiatingPeers.delete(peerId)
    }
  }

  async handleAnswer(data) {
    const peerId = data.from
    // Learn the answerer's nickname if we don't have it yet
    if (data.nickname && this.onPeerJoined) {
      this.onPeerJoined(peerId, data.nickname)
    }
    // Try _send first, then _recv
    let pc = this.peerConnections.get(peerId + '_send')
    if (!pc || pc.signalingState === 'stable') {
      pc = this.peerConnections.get(peerId + '_recv')
    }
    if (!pc) return

    try {
      await pc.setRemoteDescription(new RTCSessionDescription(data.answer))
    } catch {}
  }

  async handleIceCandidate(data) {
    const peerId = data.from
    let pc = this.peerConnections.get(peerId + '_send')
    if (!pc) pc = this.peerConnections.get(peerId + '_recv')
    if (pc && data.candidate) {
      try {
        await pc.addIceCandidate(new RTCIceCandidate(data.candidate))
      } catch {}
    }
  }

  // Track which peers have sent us a camera stream
  _peersWithStream = new Set()

  _hasStreamForPeer(peerId) {
    return this._peersWithStream.has(peerId)
  }

  _markPeerHasStream(peerId) {
    this._peersWithStream.add(peerId)
  }

  async startScreenShare(screenStream) {
    if (!screenStream) return
    this.screenStream = screenStream
    const videoTrack = screenStream.getVideoTracks()[0]
    if (!videoTrack) return

    this.publishToRoom('screen-share-started', {
      roomId: this.roomId, from: this.userId
    })

    for (const [key, pc] of this.peerConnections) {
      if (key.endsWith('_send')) {
        try {
          pc.addTrack(videoTrack, screenStream)
          const offer = await pc.createOffer()
          await pc.setLocalDescription(offer)
          const peerId = key.replace('_send', '')
          this.publishToRoom('offer', {
            roomId: this.roomId, from: this.userId, to: peerId, offer
          })
        } catch (e) {
          console.warn('Failed to add screen track to peer:', e)
        }
      }
    }
  }

  async stopScreenShare() {
    if (!this.screenStream) return

    this.publishToRoom('screen-share-stopped', {
      roomId: this.roomId, from: this.userId
    })

    for (const [key, pc] of this.peerConnections) {
      if (key.endsWith('_send')) {
        const senders = pc.getSenders().filter(s => s.track && s.track.kind === 'video')
        if (senders.length > 1) {
          try { pc.removeTrack(senders[1]) } catch {}
          try {
            const offer = await pc.createOffer()
            await pc.setLocalDescription(offer)
            const peerId = key.replace('_send', '')
            this.publishToRoom('offer', {
              roomId: this.roomId, from: this.userId, to: peerId, offer
            })
          } catch (e) {
            console.warn('Failed to renegotiate after removing screen track:', e)
          }
        }
      }
    }

    this.screenStream.getTracks().forEach(t => t.stop())
    this.screenStream = null
  }

  stop() {
    if (this.roomId && this.userId) {
      this.publishToRoom('leave', { roomId: this.roomId, userId: this.userId })
    }
    this.stopScreenShare()
    this.cleanupSubscriptions()
    this.closeAllConnections()
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop())
      this.localStream = null
    }
    if (this.screenStream) {
      this.screenStream.getTracks().forEach(track => track.stop())
      this.screenStream = null
    }
  }
}

export default WebRTCService
