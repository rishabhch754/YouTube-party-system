import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import YouTube from 'react-youtube';
import WebSocketService from '../services/websocket';
import './Room.css';

const Room = () => {
  const { roomId } = useParams();
  const navigate = useNavigate();
  
  // State Management
  const [participants, setParticipants] = useState([]);
  const [player, setPlayer] = useState(null);
  const [userRole, setUserRole] = useState('PARTICIPANT');
  const [currentVideoId, setCurrentVideoId] = useState('6v2L2UGZJAM');
  const [newVideoUrl, setNewVideoUrl] = useState('');
  const [chatMessages, setChatMessages] = useState([]);
  const [chatInput, setChatInput] = useState('');
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(100);
  const [isSeeking, setIsSeeking] = useState(false);
  const [lastSyncTime, setLastSyncTime] = useState(0);
  
  const userId = localStorage.getItem('userId');
  const username = localStorage.getItem('username');
  const wsService = useRef(null);
  const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';

useEffect(() => {
    const fetchCurrentRole = async () => {
        try {
            const response = await fetch(`${apiUrl}/api/rooms/${roomId}`);
            const data = await response.json();
            const currentUser = data.participants?.find(p => p.userId === userId);
            if (currentUser && currentUser.role !== userRole) {
                setUserRole(currentUser.role);
                localStorage.setItem('role', currentUser.role);
                console.log('✅ Role refreshed from backend:', currentUser.role);
            }
        } catch (error) {
            console.error('Error fetching role:', error);
        }
    };
    
    fetchCurrentRole();
}, [roomId, userId, apiUrl, userRole]);  

  // WebSocket Connection Effect
  useEffect(() => {
    if (!userId || !username) {
      navigate('/');
      return;
    }

    const role = localStorage.getItem('role') || 'PARTICIPANT';
    setUserRole(role);
    
    setParticipants([{
      userId: userId,
      username: username,
      role: role
    }]);
    
    wsService.current = new WebSocketService();
    wsService.current.connect(roomId, userId, username, {
      onPlay: () => {
        if (player && player.playVideo && Date.now() - lastSyncTime > 500) {
          player.playVideo();
          setLastSyncTime(Date.now());
        }
      },
      
      onPause: () => {
        if (player && player.pauseVideo && Date.now() - lastSyncTime > 500) {
          player.pauseVideo();
          setLastSyncTime(Date.now());
        }
      },
      
      onSeek: (data) => {
        if (player && player.seekTo && Date.now() - lastSyncTime > 500) {
          player.seekTo(data.time, true);
          setCurrentTime(data.time);
          setLastSyncTime(Date.now());
        }
      },
      
      onVideoChange: (data) => {
        setCurrentVideoId(data.videoId);
        setCurrentTime(0);
        if (player && player.loadVideoById) {
          player.loadVideoById(data.videoId);
        }
      },
      
      onUserJoined: (data) => {
        setParticipants(prev => {
          if (prev.find(p => p.userId === data.userId)) return prev;
          return [...prev, data];
        });
      },
      
      onUserLeft: (data) => {
        setParticipants(prev => prev.filter(p => p.userId !== data.userId));
      },
      
      onParticipantsUpdate: (data) => {
        console.log('Participants updated from server:', data);
        setParticipants(data.participants);
      },
      
      onRoleAssigned: (data) => {
        setParticipants(prev => 
          prev.map(p => p.userId === data.userId ? {...p, role: data.role} : p)
        );
        if (data.userId === userId) {
          setUserRole(data.role);
          localStorage.setItem('role', data.role);
        }
      },
      
      onParticipantRemoved: (data) => {
        if (data.userId === userId) {
          alert('You have been removed from the room by the host!');
          navigate('/');
        } else {
          setParticipants(prev => prev.filter(p => p.userId !== data.userId));
        }
      },
      
      onChat: (data) => {
        if (data.username && data.message) {
          addChatMessage(data.username, data.message);
        }
      },
      
      onError: (error) => {
        console.error('WebSocket error:', error);
      }
    });

    return () => {
      if (wsService.current) {
        wsService.current.disconnect();
      }
    };
  }, [roomId, userId, username, navigate, player, lastSyncTime]);

  // Time update interval
  useEffect(() => {
    const interval = setInterval(() => {
      if (player && !isSeeking && player.getCurrentTime) {
        const time = player.getCurrentTime();
        if (time) setCurrentTime(time);
        const dur = player.getDuration();
        if (dur) setDuration(dur);
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [player, isSeeking]);

  // Helper Functions
  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const addChatMessage = (sender, message) => {
    setChatMessages(prev => {
      const lastMsg = prev[prev.length - 1];
      if (lastMsg && lastMsg.sender === sender && lastMsg.message === message) {
        return prev;
      }
      return [...prev, { sender, message, timestamp: new Date() }];
    });
    setTimeout(() => {
      const container = document.querySelector('.chat-messages');
      if (container) container.scrollTop = container.scrollHeight;
    }, 100);
  };

  // YouTube Player Events
  const onPlayerReady = (event) => {
    setPlayer(event.target);
    setDuration(event.target.getDuration());
  };

  const onPlayerStateChange = (event) => {
    if (!isSeeking && (userRole === 'HOST' || userRole === 'MODERATOR')) {
      if (event.data === 1) {
        wsService.current?.sendPlay(roomId, userId);
      } else if (event.data === 2) {
        wsService.current?.sendPause(roomId, userId);
      }
    }
  };

  // Control Handlers
  const handleSeekStart = () => setIsSeeking(true);
  const handleSeekChange = (e) => setCurrentTime(parseFloat(e.target.value));
  const handleSeekEnd = (e) => {
    const time = parseFloat(e.target.value);
    setIsSeeking(false);
    if (player && (userRole === 'HOST' || userRole === 'MODERATOR')) {
      player.seekTo(time, true);
      wsService.current?.sendSeek(roomId, userId, time);
    }
  };

  const extractVideoId = (url) => {
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
    const match = url.match(regExp);
    return (match && match[2].length === 11) ? match[2] : null;
  };

  const handleChangeVideo = () => {
    const videoId = extractVideoId(newVideoUrl);
    if (videoId) {
      wsService.current?.sendChangeVideo(roomId, userId, videoId);
      setNewVideoUrl('');
    } else {
      alert('Invalid YouTube URL!');
    }
  };

  // Role Management
  const assignRole = (targetId, role) => {
    wsService.current?.sendAssignRole(roomId, userId, targetId, role);
  };

  const removeParticipant = (targetId) => {
    if (window.confirm('Remove this participant?')) {
      wsService.current?.sendRemoveParticipant(roomId, userId, targetId);
    }
  };

  // Chat Handler
  const sendChat = () => {
    if (chatInput.trim()) {
      addChatMessage(username, chatInput);
      wsService.current?.sendChat(roomId, userId, username, chatInput);
      setChatInput('');
    }
  };

  // Room Handlers
  const copyRoomCode = () => {
    navigator.clipboard.writeText(roomId);
  };

  const leaveRoom = () => {
    if (window.confirm('Leave the room?')) {
      if (wsService.current) {
        wsService.current.disconnect();
      }
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('role');
      navigate('/');
    }
  };

  // ✅ UPDATED YouTube Player Options with origin
  const opts = {
    height: '450',
    width: '100%',
    playerVars: {
      autoplay: 0,
      controls: 0,
      modestbranding: 1,
      rel: 0,
      showinfo: 0,
      disablekb: 1,
      fs: 0,
      iv_load_policy: 3,
      origin: window.location.origin,  // ✅ ADD THIS LINE
    },
  };

  const canControl = userRole === 'HOST' || userRole === 'MODERATOR';
  const hostName = participants.find(p => p.role === 'HOST')?.username || 'Unknown';

  return (
    <div className="room-container">
      <div className="room-header">
        <h2>
          YouTube Watch Party
          <span className="room-code" onClick={copyRoomCode} style={{ cursor: 'pointer', fontSize: '14px' }}>
            {roomId}
          </span>
        </h2>
        <div>
          <span style={{ 
            marginRight: '15px', 
            fontWeight: 'bold',
            fontSize: '14px',
            background: '#f0f0f0',
            padding: '4px 12px',
            borderRadius: '20px'
          }}>
            Host: {hostName}
          </span>
          <span style={{ 
            marginRight: '15px', 
            fontWeight: 'bold',
            fontSize: '14px'
          }}>
            {userRole === 'HOST' ? 'HOST' : userRole === 'MODERATOR' ? 'MODERATOR' : 'PARTICIPANT'}
          </span>
          <button className="btn btn-danger" onClick={leaveRoom}>
            Leave Room
          </button>
        </div>
      </div>

      <div className="room-content">
        <div className="video-section">
          <YouTube
            videoId={currentVideoId}
            opts={opts}
            onReady={onPlayerReady}
            onStateChange={onPlayerStateChange}
          />
          
          {canControl && (
            <div className="seek-bar-container">
              <div className="time-display">
                <span>{formatTime(currentTime)}</span>
                <span>/</span>
                <span>{formatTime(duration)}</span>
              </div>
              <input
                type="range"
                min="0"
                max={duration || 100}
                step="0.1"
                value={currentTime}
                onMouseDown={handleSeekStart}
                onChange={handleSeekChange}
                onMouseUp={handleSeekEnd}
                disabled={!player}
                className="seek-bar"
              />
            </div>
          )}
          
          {canControl && (
            <div className="controls">
              <button onClick={() => player?.playVideo()}>Play</button>
              <button onClick={() => player?.pauseVideo()}>Pause</button>
              <div className="video-change">
                <input
                  type="text"
                  placeholder="Paste YouTube URL..."
                  value={newVideoUrl}
                  onChange={(e) => setNewVideoUrl(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleChangeVideo()}
                />
                <button onClick={handleChangeVideo}>Change</button>
              </div>
            </div>
          )}
          
          {!canControl && (
            <div className="participant-message">
              Only HOST and MODERATOR can control the video.
            </div>
          )}
        </div>

        <div className="sidebar">
          <div className="participants-section">
            <h3>Participants ({participants.length})</h3>
            <ul>
              {participants.map((p) => (
                <li key={p.userId} className="participant-item">
                  <span className="participant-name">
                    {p.username}
                    {p.userId === userId && ' (You)'}
                  </span>
                  <span className={`role-badge role-${p.role.toLowerCase()}`}>
                    {p.role === 'HOST' && 'HOST'}
                    {p.role === 'MODERATOR' && 'MODERATOR'}
                    {p.role === 'PARTICIPANT' && 'PARTICIPANT'}
                  </span>
                  {userRole === 'HOST' && p.userId !== userId && (
                    <div className="participant-actions">
                      {p.role !== 'MODERATOR' && (
                        <button onClick={() => assignRole(p.userId, 'MODERATOR')}>
                          Make Moderator
                        </button>
                      )}
                      {p.role !== 'PARTICIPANT' && (
                        <button onClick={() => assignRole(p.userId, 'PARTICIPANT')}>
                          Demote
                        </button>
                      )}
                      <button onClick={() => removeParticipant(p.userId)}>
                        Remove
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          </div>

          <div className="chat-section">
            <h3>Chat</h3>
            <div className="chat-messages">
              {chatMessages.map((msg, idx) => (
                <div key={idx} className="chat-message">
                  <strong>{msg.sender}:</strong> {msg.message}
                </div>
              ))}
              {chatMessages.length === 0 && (
                <div style={{ textAlign: 'center', color: '#999', padding: '20px' }}>
                  No messages yet.
                </div>
              )}
            </div>
            <div className="chat-input">
              <input
                type="text"
                placeholder="Type message..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && sendChat()}
              />
              <button onClick={sendChat}>Send</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Room;