# YouTube Watch Party System

## Live URL
[https://your-app.vercel.app](https://your-app.vercel.app)

## Features
- Real-time video synchronization using WebSockets
- Room-based model with unique codes
- YouTube integration
- Role-based access (Host, Moderator, Participant)
- Host can assign roles and remove participants
- Live chat

## Tech Stack
- Frontend: React 18
- Backend: Java Spring Boot 3.1.5
- Real-time: WebSocket (STOMP over SockJS)
- Database: H2

## Setup Instructions

### Backend
```bash
cd backend/watch-party-backend
mvn spring-boot:run