package com.watchparty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
public class RoomEntity {
    
    @Id
    @Column(name = "room_id", length = 50)
    private String roomId;
    
    @Column(name = "host_id", nullable = false, length = 100)
    private String hostId;
    
    @Column(name = "host_username", nullable = false)
    private String hostUsername;
    
    @Column(name = "video_id", length = 50)
    private String videoId = "6v2L2UGZJAM";
    
    @Column(name = "video_time")
    private Double videoTime = 0.0;
    
    @Column(name = "is_playing")
    private Boolean isPlaying = false;
    
    @Column(name = "participants_json", columnDefinition = "TEXT")
    private String participantsJson;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public RoomEntity() {}
    
    // Getters
    public String getRoomId() { return roomId; }
    public String getHostId() { return hostId; }
    public String getHostUsername() { return hostUsername; }
    public String getVideoId() { return videoId; }
    public Double getVideoTime() { return videoTime; }
    public Boolean getIsPlaying() { return isPlaying; }
    public String getParticipantsJson() { return participantsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Setters
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    public void setVideoTime(Double videoTime) { this.videoTime = videoTime; }
    public void setIsPlaying(Boolean isPlaying) { this.isPlaying = isPlaying; }
    public void setParticipantsJson(String participantsJson) { this.participantsJson = participantsJson; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}