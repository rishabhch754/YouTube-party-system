package com.watchparty.controller;

import com.watchparty.model.Room;
import com.watchparty.model.RoomState;
import com.watchparty.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {
    
    @Autowired
    private RoomService roomService;
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Backend is running!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/all")
    public ResponseEntity<?> getAllRooms() {
        return ResponseEntity.ok(Map.of("rooms", roomService.getAllRoomIds()));
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        
        String roomId = "ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        
        Room room = roomService.createRoom(roomId, userId, username, null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("userId", userId);
        response.put("username", username);
        response.put("role", "HOST");
        response.put("videoId", room.getState().getVideoId());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody Map<String, String> request) {
        String roomId = request.get("roomId");
        String username = request.get("username");
        
        if (roomId == null || roomId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Room ID is required"));
        }
        
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        
        if (!roomService.roomExists(roomId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Room not found: " + roomId));
        }
        
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        RoomState state = roomService.getRoomState(roomId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("userId", userId);
        response.put("username", username);
        response.put("role", "PARTICIPANT");
        response.put("videoId", state != null ? state.getVideoId() : "dQw4w9WgXcQ");
        response.put("currentTime", state != null ? state.getCurrentTime() : 0);
        response.put("isPlaying", state != null ? state.isPlaying() : false);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", room.getRoomId());
        response.put("hostId", room.getHostId());
        response.put("state", room.getState());
        response.put("participantCount", room.getParticipantCount());
        response.put("participants", roomService.getParticipants(roomId));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{roomId}/exists")
    public ResponseEntity<?> roomExists(@PathVariable String roomId) {
        return ResponseEntity.ok(Map.of("exists", roomService.roomExists(roomId)));
    }
}