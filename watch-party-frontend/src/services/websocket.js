// src/services/websocket.js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.roomId = null;
    this.userId = null;
    this.callbacks = {};
    this.isConnected = false;
  }

  connect(roomId, userId, username, callbacks) {
    this.roomId = roomId;
    this.userId = userId;
    this.callbacks = callbacks;

    const wsUrl = process.env.REACT_APP_WS_URL || 'http://localhost:8080';
    
    console.log(`Connecting to WebSocket at ${wsUrl}/ws`);
    
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${wsUrl}/ws`),
      debug: (str) => {
        console.log('STOMP:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('✅ Connected to WebSocket');
        this.isConnected = true;
        
        // Subscribe to room topics
        this.client.subscribe(`/topic/room/${roomId}/play`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received PLAY:', data);
          callbacks.onPlay && callbacks.onPlay(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/pause`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received PAUSE:', data);
          callbacks.onPause && callbacks.onPause(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/seek`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received SEEK:', data);
          callbacks.onSeek && callbacks.onSeek(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/change_video`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received CHANGE_VIDEO:', data);
          callbacks.onVideoChange && callbacks.onVideoChange(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/user_joined`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received USER_JOINED:', data);
          callbacks.onUserJoined && callbacks.onUserJoined(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/user_left`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received USER_LEFT:', data);
          callbacks.onUserLeft && callbacks.onUserLeft(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/role_assigned`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received ROLE_ASSIGNED:', data);
          callbacks.onRoleAssigned && callbacks.onRoleAssigned(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/participant_removed`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received PARTICIPANT_REMOVED:', data);
          callbacks.onParticipantRemoved && callbacks.onParticipantRemoved(data);
        });
        
        this.client.subscribe(`/topic/room/${roomId}/chat`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received CHAT:', data);
          callbacks.onChat && callbacks.onChat(data);
        });
        
        // Subscribe to user-specific messages
        this.client.subscribe(`/user/queue/state`, (msg) => {
          const data = JSON.parse(msg.body);
          console.log('Received STATE:', data);
          if (data.videoId && callbacks.onVideoChange) {
            callbacks.onVideoChange({ videoId: data.videoId });
          }
          if (callbacks.onSeek && data.currentTime) {
            callbacks.onSeek({ time: data.currentTime });
          }
        });
        
        this.client.subscribe(`/user/queue/error`, (msg) => {
          const data = JSON.parse(msg.body);
          console.error('Received ERROR:', data);
          callbacks.onError && callbacks.onError(data.error);
        });
        
        // Join room
        this.client.publish({
          destination: '/app/join_room',
          body: JSON.stringify({ roomId, username, userId })
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        callbacks.onError && callbacks.onError(frame.headers.message || 'Connection error');
      },
      onWebSocketError: (error) => {
        console.error('WebSocket error:', error);
        callbacks.onError && callbacks.onError('WebSocket connection error');
      },
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
        this.isConnected = false;
      }
    });
    
    this.client.activate();
  }

  sendPlay(roomId, userId) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/play',
        body: JSON.stringify({ roomId, userId })
      });
      console.log('Sent PLAY');
    } else {
      console.warn('Cannot send PLAY - not connected');
    }
  }

  sendPause(roomId, userId) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/pause',
        body: JSON.stringify({ roomId, userId })
      });
      console.log('Sent PAUSE');
    } else {
      console.warn('Cannot send PAUSE - not connected');
    }
  }

  sendSeek(roomId, userId, time) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/seek',
        body: JSON.stringify({ roomId, userId, time })
      });
      console.log('Sent SEEK to:', time);
    } else {
      console.warn('Cannot send SEEK - not connected');
    }
  }

  sendChangeVideo(roomId, userId, videoId) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/change_video',
        body: JSON.stringify({ roomId, userId, videoId })
      });
      console.log('Sent CHANGE_VIDEO to:', videoId);
    } else {
      console.warn('Cannot send CHANGE_VIDEO - not connected');
    }
  }

  sendAssignRole(roomId, userId, targetId, role) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/assign_role',
        body: JSON.stringify({ roomId, userId, targetId, role })
      });
      console.log('Sent ASSIGN_ROLE:', targetId, '->', role);
    } else {
      console.warn('Cannot send ASSIGN_ROLE - not connected');
    }
  }

  sendRemoveParticipant(roomId, userId, targetId) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/remove_participant',
        body: JSON.stringify({ roomId, userId, targetId })
      });
      console.log('Sent REMOVE_PARTICIPANT:', targetId);
    } else {
      console.warn('Cannot send REMOVE_PARTICIPANT - not connected');
    }
  }

  sendChat(roomId, userId, username, message) {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/chat',
        body: JSON.stringify({ roomId, userId, username, message })
      });
      console.log('Sent CHAT:', message);
    } else {
      console.warn('Cannot send CHAT - not connected');
    }
  }

  disconnect() {
    if (this.client && this.isConnected) {
      this.client.publish({
        destination: '/app/leave_room',
        body: JSON.stringify({ roomId: this.roomId, userId: this.userId })
      });
      this.client.deactivate();
    }
  }
}

export default WebSocketService;