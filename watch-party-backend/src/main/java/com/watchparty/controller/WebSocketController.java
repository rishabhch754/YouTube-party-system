package com.watchparty.controller;

import com.watchparty.model.Participant;
import com.watchparty.model.Role;
import com.watchparty.model.Room;
import com.watchparty.model.RoomState;
import com.watchparty.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WebSocketController {
    
    @Autowired
    private RoomService roomService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/join_room")
    public void joinRoom(@Payload Map<String, String> payload, 
                         @Header("simpSessionId") String sessionId) {
        String roomId = payload.get("roomId");
        String username = payload.get("username");
        String userId = payload.get("userId");
        
        System.out.println("User joining: " + username + " to room: " + roomId);
        
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            System.out.println("Room not found: " + roomId);
            return;
        }
        
        // Check if user already exists
        Participant existing = room.getParticipants().get(userId);
        if (existing != null) {
            existing.setSessionId(sessionId);
            sendCurrentState(sessionId, room);
            sendParticipantsList(roomId, room);
            return;
        }
        
        // Determine role (first user becomes host)
        Role role = room.getParticipants().isEmpty() ? Role.HOST : Role.PARTICIPANT;
        Participant participant = new Participant(userId, username, sessionId, role);
        roomService.addParticipant(roomId, participant);
        
        // Send current state to new user
        sendCurrentState(sessionId, room);
        
        // Broadcast to ALL users about new participant
        Map<String, Object> userJoined = new HashMap<>();
        userJoined.put("userId", userId);
        userJoined.put("username", username);
        userJoined.put("role", role.name());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/user_joined", userJoined);
        
        // Send updated participants list to ALL users
        sendParticipantsList(roomId, room);
        
        System.out.println("User joined: " + username + " as " + role);
        System.out.println("Total participants: " + room.getParticipantCount());
    }
    
    @MessageMapping("/leave_room")
    public void leaveRoom(@Payload Map<String, String> payload) {
        String roomId = payload.get("roomId");
        String userId = payload.get("userId");
        
        System.out.println("User leaving: " + userId + " from room: " + roomId);
        
        Room room = roomService.getRoom(roomId);
        if (room != null) {
            Participant participant = room.getParticipants().get(userId);
            if (participant != null) {
                boolean isHost = participant.getRole() == Role.HOST;
                
                Map<String, Object> userLeft = new HashMap<>();
                userLeft.put("userId", userId);
                userLeft.put("username", participant.getUsername());
                userLeft.put("isHost", isHost);
                
                roomService.removeParticipant(roomId, userId);
                
                // Broadcast to all users that someone left
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/user_left", userLeft);
                
                // Send updated participants list to ALL users
                sendParticipantsList(roomId, room);
                
                System.out.println("User left: " + participant.getUsername());
                System.out.println("Remaining participants: " + room.getParticipantCount());
            }
        }
    }
    
    @MessageMapping("/play")
    public void play(@Payload Map<String, String> payload) {
        String roomId = payload.get("roomId");
        String userId = payload.get("userId");
        
        if (roomService.canControlVideo(roomId, userId)) {
            roomService.updatePlayState(roomId, true);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/play", Map.of());
        }
    }
    
    @MessageMapping("/pause")
    public void pause(@Payload Map<String, String> payload) {
        String roomId = payload.get("roomId");
        String userId = payload.get("userId");
        
        if (roomService.canControlVideo(roomId, userId)) {
            roomService.updatePlayState(roomId, false);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/pause", Map.of());
        }
    }
    
    @MessageMapping("/seek")
    public void seek(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String userId = (String) payload.get("userId");
        double time = ((Number) payload.get("time")).doubleValue();
        
        if (roomService.canControlVideo(roomId, userId)) {
            roomService.updateSeek(roomId, time);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/seek", Map.of("time", time));
        }
    }
    
    @MessageMapping("/change_video")
    public void changeVideo(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String userId = (String) payload.get("userId");
        String videoId = (String) payload.get("videoId");
        
        if (roomService.canControlVideo(roomId, userId)) {
            roomService.updateVideo(roomId, videoId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/change_video", Map.of("videoId", videoId));
        }
    }
    
    @MessageMapping("/assign_role")
    public void assignRole(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String requesterId = (String) payload.get("userId");
        String targetId = (String) payload.get("targetId");
        String roleStr = (String) payload.get("role");
        
        System.out.println("Assigning role: " + targetId + " to " + roleStr + " by " + requesterId);
        
        if (roomService.isHost(roomId, requesterId)) {
            Role newRole = Role.valueOf(roleStr);
            if (roomService.assignRole(roomId, targetId, newRole, requesterId)) {
                Map<String, Object> roleAssigned = new HashMap<>();
                roleAssigned.put("userId", targetId);
                roleAssigned.put("role", newRole.name());
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/role_assigned", roleAssigned);
                
                // Send updated participants list to ALL users after role change
                Room room = roomService.getRoom(roomId);
                sendParticipantsList(roomId, room);
                
                System.out.println("Role assigned successfully");
            }
        }
    }
    
    @MessageMapping("/remove_participant")
    public void removeParticipant(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String requesterId = (String) payload.get("userId");
        String targetId = (String) payload.get("targetId");
        
        System.out.println("Removing participant: " + targetId + " by " + requesterId);
        
        if (roomService.isHost(roomId, requesterId)) {
            Map<String, Object> removed = new HashMap<>();
            removed.put("userId", targetId);
            roomService.removeParticipant(roomId, targetId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/participant_removed", removed);
            
            // Send updated participants list to ALL users
            Room room = roomService.getRoom(roomId);
            sendParticipantsList(roomId, room);
        }
    }
    
    @MessageMapping("/chat")
    public void chat(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String username = (String) payload.get("username");
        String message = (String) payload.get("message");
        
        Map<String, Object> chatMessage = new HashMap<>();
        chatMessage.put("username", username);
        chatMessage.put("message", message);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", chatMessage);
    }
    
    private void sendCurrentState(String sessionId, Room room) {
        Map<String, Object> state = new HashMap<>();
        state.put("videoId", room.getState().getVideoId());
        state.put("currentTime", room.getState().getCurrentTime());
        state.put("isPlaying", room.getState().isPlaying());
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/state", state);
    }
    
    private void sendParticipantsList(String roomId, Room room) {
        // Get all participants
        List<Map<String, Object>> participantsList = new ArrayList<>();
        for (Participant p : room.getParticipants().values()) {
            Map<String, Object> participant = new HashMap<>();
            participant.put("userId", p.getUserId());
            participant.put("username", p.getUsername());
            participant.put("role", p.getRole().name());
            participantsList.add(participant);
        }
        
        Map<String, Object> participantsUpdate = new HashMap<>();
        participantsUpdate.put("participants", participantsList);
        participantsUpdate.put("count", participantsList.size());
        
        // Broadcast to all users in the room
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/participants_update", participantsUpdate);
        
        System.out.println("Broadcasting participants list: " + participantsList.size() + " participants");
    }
}