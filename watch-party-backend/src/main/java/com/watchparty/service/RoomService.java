package com.watchparty.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watchparty.entity.RoomEntity;
import com.watchparty.model.Participant;
import com.watchparty.model.Role;
import com.watchparty.model.Room;
import com.watchparty.model.RoomState;
import com.watchparty.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final Map<String, Room> activeRooms = new ConcurrentHashMap<>();
    
    public Room createRoom(String roomId, String hostId, String hostUsername, String sessionId) {
        Room room = new Room(roomId, hostId, hostUsername, sessionId);
        
        RoomEntity entity = new RoomEntity();
        entity.setRoomId(roomId);
        entity.setHostId(hostId);
        entity.setHostUsername(hostUsername);
        entity.setVideoId(room.getState().getVideoId());
        entity.setVideoTime(room.getState().getCurrentTime());
        entity.setIsPlaying(room.getState().isPlaying());
        
        try {
            String participantsJson = objectMapper.writeValueAsString(room.getParticipants());
            entity.setParticipantsJson(participantsJson);
        } catch (JsonProcessingException e) {
            entity.setParticipantsJson("{}");
        }
        
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        
        roomRepository.save(entity);
        activeRooms.put(roomId, room);
        
        return room;
    }
    
    public Room getRoom(String roomId) {
        if (activeRooms.containsKey(roomId)) {
            return activeRooms.get(roomId);
        }
        
        Optional<RoomEntity> entityOpt = roomRepository.findByRoomId(roomId);
        if (entityOpt.isPresent()) {
            RoomEntity entity = entityOpt.get();
            Room room = new Room(roomId, entity.getHostId(), entity.getHostUsername(), null);
            room.getState().update(entity.getVideoId(), entity.getIsPlaying(), entity.getVideoTime());
            
            try {
                if (entity.getParticipantsJson() != null && !entity.getParticipantsJson().isEmpty()) {
                    Map<String, Participant> participants = objectMapper.readValue(
                        entity.getParticipantsJson(), 
                        new TypeReference<Map<String, Participant>>() {}
                    );
                    room.setParticipants(participants);
                }
            } catch (JsonProcessingException e) {
                // Ignore
            }
            
            activeRooms.put(roomId, room);
            return room;
        }
        
        return null;
    }
    
    public boolean roomExists(String roomId) {
        return activeRooms.containsKey(roomId) || roomRepository.existsByRoomId(roomId);
    }
    
    public void addParticipant(String roomId, Participant participant) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.addParticipant(participant);
            updateRoomInDatabase(room);
        }
    }
    
    public void removeParticipant(String roomId, String userId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.removeParticipant(userId);
            if (room.getParticipants().isEmpty()) {
                activeRooms.remove(roomId);
                roomRepository.deleteByRoomId(roomId);
            } else {
                updateRoomInDatabase(room);
            }
        }
    }
    
    public boolean assignRole(String roomId, String userId, Role newRole, String requesterId) {
        Room room = getRoom(roomId);
        if (room != null) {
            boolean success = room.assignRole(userId, newRole, requesterId);
            if (success) {
                updateRoomInDatabase(room);
            }
            return success;
        }
        return false;
    }
    
    public boolean canControlVideo(String roomId, String userId) {
        Room room = getRoom(roomId);
        return room != null && room.canControl(userId);
    }
    
    public boolean isHost(String roomId, String userId) {
        Room room = getRoom(roomId);
        return room != null && room.isHost(userId);
    }
    
    public void updateVideo(String roomId, String videoId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.getState().setVideoId(videoId);
            room.getState().setCurrentTime(0);
            room.getState().setPlaying(false);
            updateRoomInDatabase(room);
        }
    }
    
    public void updatePlayState(String roomId, boolean isPlaying) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.getState().setPlaying(isPlaying);
            updateRoomInDatabase(room);
        }
    }
    
    public void updateSeek(String roomId, double currentTime) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.getState().setCurrentTime(currentTime);
            updateRoomInDatabase(room);
        }
    }
    
    public RoomState getRoomState(String roomId) {
        Room room = getRoom(roomId);
        return room != null ? room.getState() : null;
    }
    
    public List<Participant> getParticipants(String roomId) {
        Room room = getRoom(roomId);
        return room != null ? new ArrayList<>(room.getParticipants().values()) : Collections.emptyList();
    }
    
    public Role getUserRole(String roomId, String userId) {
        Room room = getRoom(roomId);
        if (room != null) {
            Participant participant = room.getParticipants().get(userId);
            if (participant != null) {
                return participant.getRole();
            }
        }
        return null;
    }
    
    private void updateRoomInDatabase(Room room) {
        Optional<RoomEntity> entityOpt = roomRepository.findByRoomId(room.getRoomId());
        if (entityOpt.isPresent()) {
            RoomEntity entity = entityOpt.get();
            entity.setVideoId(room.getState().getVideoId());
            entity.setVideoTime(room.getState().getCurrentTime());
            entity.setIsPlaying(room.getState().isPlaying());
            
            try {
                String participantsJson = objectMapper.writeValueAsString(room.getParticipants());
                entity.setParticipantsJson(participantsJson);
            } catch (JsonProcessingException e) {
                // Ignore
            }
            
            entity.setUpdatedAt(LocalDateTime.now());
            roomRepository.save(entity);
        }
    }
    
    public List<String> getAllRoomIds() {
        List<String> roomIds = new ArrayList<>(activeRooms.keySet());
        roomRepository.findAll().forEach(entity -> {
            if (!roomIds.contains(entity.getRoomId())) {
                roomIds.add(entity.getRoomId());
            }
        });
        return roomIds;
    }
}