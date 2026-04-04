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

How to Use the Application
Step 1: Open the Application
Go to: https://you-tube-party-system.vercel.app

Step 2: Enter Your Name
Type your name in the "Your Good Name" field.

Step 3: Create a Room (As Host)
Click "Create New Party" button

A unique Room Code will be generated (e.g., ROOM-ABC12345)

Copy this Room Code to share with friends

Step 4: Join a Room (As Participant)
Open the same URL in another browser tab or incognito window

Enter a different name

Paste the Room Code in "Please Enter Room code" field

Click "Join Party" button

Step 5: Watch Together
Host controls the video (Play, Pause, Seek, Change Video)

Participant watches the synced video automatically

Chat with each other using the chat box

Host can make a participant Moderator to give them control

Step 6: Role Management (Host Only)
Click "Make Moderator" next to any participant to give them control

Click "Demote" to remove moderator privileges

Click "Remove" to remove a participant from the room

 Note

The backend is hosted on **Render Free Tier**. Due to free tier limitations:
- Server goes to **sleep** after 15 minutes of inactivity
- First request after sleep takes **40-50 seconds** to wake up
- Subsequent requests work normally
