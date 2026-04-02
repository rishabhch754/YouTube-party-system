import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import './Home.css';

const Home = () => {
  const [username, setUsername] = useState('');
  const [roomCode, setRoomCode] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [isJoining, setIsJoining] = useState(false);
  const [backendReady, setBackendReady] = useState(false);
  const navigate = useNavigate();

  const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

  const checkBackendStatus = useCallback(async () => {
    try {
      const response = await fetch(`${API_URL}/api/rooms/test`);
      if (response.ok) {
        setBackendReady(true);
      } else {
        setBackendReady(false);
      }
    } catch (error) {
      console.error('Backend not reachable:', error);
      setBackendReady(false);
    }
  }, [API_URL]);

  useEffect(() => {
    checkBackendStatus();
  }, [checkBackendStatus]);

  const createRoom = async () => {
    if (!username.trim()) {
      alert('Please enter your name');
      return;
    }

    setIsCreating(true);
    
    try {
      const response = await fetch(`${API_URL}/api/rooms/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username: username.trim() }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to create room');
      }

      const data = await response.json();
      
      localStorage.setItem('userId', data.userId);
      localStorage.setItem('username', data.username);
      localStorage.setItem('role', data.role);
      
      alert(`Room Created!\n\nRoom Code: ${data.roomId}`);
      navigate(`/room/${data.roomId}`);
    } catch (error) {
      console.error('Error creating room:', error);
      alert(`Failed to create room: ${error.message}`);
    } finally {
      setIsCreating(false);
    }
  };

  const joinRoom = async () => {
    if (!username.trim()) {
      alert('Please enter your name');
      return;
    }
    if (!roomCode.trim()) {
      alert('Please enter a room code');
      return;
    }

    setIsJoining(true);
    
    try {
      const response = await fetch(`${API_URL}/api/rooms/join`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          roomId: roomCode.toUpperCase(), 
          username: username.trim() 
        }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Room not found');
      }

      const data = await response.json();
      
      localStorage.setItem('userId', data.userId);
      localStorage.setItem('username', data.username);
      localStorage.setItem('role', data.role);
      
      alert(`Joined Room: ${roomCode.toUpperCase()}`);
      navigate(`/room/${roomCode.toUpperCase()}`);
    } catch (error) {
      console.error('Error joining room:', error);
      alert(error.message || 'Failed to join room');
    } finally {
      setIsJoining(false);
    }
  };

  return (
    <div className="home-container">
      <div className="home-card">
        <h1>YouTube Watch Party</h1>
        <p>Watch videos together with friends in real-time</p>
        
        <div className="input-group">
          <input
            type="text"
            placeholder="Your Good Name"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && createRoom()}
          />
        </div>

        <div className="action-buttons">
          <button 
            className="btn btn-primary"
            onClick={createRoom}
            disabled={isCreating || !backendReady}
          >
            {isCreating ? 'Creating...' : 'Create New Party'}
          </button>
          
          <div className="divider">or</div>
          
          <div className="join-section">
            <input
              type="text"
              placeholder="Please Enter Room code"
              value={roomCode}
              onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
              onKeyPress={(e) => e.key === 'Enter' && joinRoom()}
            />
            <button 
              className="btn btn-primary"
              onClick={joinRoom}
              disabled={isJoining || !backendReady}
            >
              {isJoining ? 'Joining...' : 'Join Party'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;