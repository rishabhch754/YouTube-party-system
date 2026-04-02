package com.watchparty.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Participant implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private String sessionId;
    private Role role;
    private LocalDateTime joinedAt;
    
    public Participant() {}
    
    public Participant(String userId, String username, String sessionId, Role role) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getSessionId() { return sessionId; }
    public Role getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    
    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setRole(Role role) { this.role = role; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    
    public boolean hasControlPermission() {
        return role.canControl();
    }
    
    public boolean hasManagePermission() {
        return role.canManageUsers();
    }
}