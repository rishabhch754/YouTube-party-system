package com.watchparty.model;

public enum Role {
    HOST,
    MODERATOR,
    PARTICIPANT;
    
    public boolean canControl() {
        return this == HOST || this == MODERATOR;
    }
    
    public boolean canManageUsers() {
        return this == HOST;
    }
}