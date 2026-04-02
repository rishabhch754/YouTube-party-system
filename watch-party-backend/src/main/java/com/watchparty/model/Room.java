package com.watchparty.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String roomId;
    private String hostId;
    private Map<String, Participant> participants;
    private RoomState state;
    private LocalDateTime createdAt;
    
    public Room(String roomId, String hostId, String hostUsername, String sessionId) {
        this.roomId = roomId;
        this.hostId = hostId;
        this.participants = new ConcurrentHashMap<>();
        this.state = new RoomState();
        this.createdAt = LocalDateTime.now();
        
        Participant host = new Participant(hostId, hostUsername, sessionId, Role.HOST);
        participants.put(hostId, host);
    }
    
    // Getters
    public String getRoomId() { return roomId; }
    public String getHostId() { return hostId; }
    public Map<String, Participant> getParticipants() { return participants; }
    public RoomState getState() { return state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Setters
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public void setParticipants(Map<String, Participant> participants) { this.participants = participants; }
    public void setState(RoomState state) { this.state = state; }
    
    public void addParticipant(Participant participant) {
        participants.put(participant.getUserId(), participant);
    }
    
    public void removeParticipant(String userId) {
        participants.remove(userId);
        if (userId.equals(hostId) && !participants.isEmpty()) {
            String newHostId = participants.keySet().iterator().next();
            hostId = newHostId;
            participants.get(newHostId).setRole(Role.HOST);
        }
    }
    
    public Optional<Participant> getParticipant(String userId) {
        return Optional.ofNullable(participants.get(userId));
    }
    
    public boolean assignRole(String userId, Role newRole, String requesterId) {
        Participant requester = participants.get(requesterId);
        if (requester == null || !requester.hasManagePermission()) {
            return false;
        }
        
        Participant target = participants.get(userId);
        if (target != null) {
            target.setRole(newRole);
            if (newRole == Role.HOST) {
                hostId = userId;
            }
            return true;
        }
        return false;
    }
    
    public int getParticipantCount() {
        return participants.size();
    }
    
    public boolean isHost(String userId) {
        return hostId.equals(userId);
    }
    
    public boolean canControl(String userId) {
        Participant participant = participants.get(userId);
        return participant != null && participant.hasControlPermission();
    }
}