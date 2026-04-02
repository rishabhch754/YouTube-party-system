# YouTube Watch Party

A real-time synchronized video watching platform that allows multiple users to watch YouTube videos together.

## Live URL
[https://you-tube-party-system.vercel.app]   (https://you-tube-party-system.vercel.app)

## Tech Stack
| Layer | Technology |
|------------|------------
| Frontend   | React 18 
| Backend    | Java Spring Boot 3.1.5 
| Real-time  | WebSocket (STOMP over SockJS) 
| Database   | H2 
| Deployment | Vercel (Frontend) + Render (Backend) 

## Architecture Overview

### How WebSockets Enable Real-time Sync

1. User joins room → WebSocket connection via SockJS
2. Host plays/pauses/seeks → Event sent to `/app/{action}`
3. Backend validates role (HOST/MODERATOR only)
4. Backend broadcasts event to `/topic/room/{id}/{action}`
5. All connected clients receive event
6. YouTube player updates via IFrame API

## Role-Based Access Control

| Action             | HOST | MODERATOR | PARTICIPANT 
|--------------------|------|-----------|-------------
| Play/Pause/Seek    | Yes  | Yes       | No 
| Change Video       | Yes  |  Yes      | No 
| Assign Role        | Yes  | No        | No 
| Remove Participant | Yes  | No        | No
| Chat               | Yes  | Yes       | Yes 

##  Setup Instructions

## Prerequisites
- Java 17
- Node.js 18+
- Maven

### Backend Setup
```bash
cd watch-party-backend
mvn spring-boot:run

## API Endpoints

Method	Endpoint	Description
POST	/api/rooms/create	Create new room
POST	/api/rooms/join	Join existing room
GET	/api/rooms/test	Health check

##  Deployment
Service	Platform	URL
Frontend	Vercel	https://you-tube-party-system.vercel.app
Backend	Render	https://watch-party-backend-xux1.onrender.com

# Demo
Video demonstration attached.

## Features Implemented
Real-time video synchronization (play/pause/seek)
Room-based model with unique codes
YouTube integration
Role-based access (Host, Moderator, Participant)
Host can assign roles and remove participants
Live chat
Multiple users can watch together
