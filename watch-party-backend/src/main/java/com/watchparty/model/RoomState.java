package com.watchparty.model;

import java.io.Serializable;

public class RoomState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String videoId = "6v2L2UGZJAM";
    private boolean isPlaying = false;
    private double currentTime = 0.0;
    private long lastUpdated = System.currentTimeMillis();
    
    public RoomState() {}
    
    // Getters
    public String getVideoId() { return videoId; }
    public boolean isPlaying() { return isPlaying; }
    public double getCurrentTime() { return currentTime; }
    public long getLastUpdated() { return lastUpdated; }
    
    // Setters
    public void setVideoId(String videoId) { 
        this.videoId = videoId;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setPlaying(boolean playing) { 
        this.isPlaying = playing;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setCurrentTime(double currentTime) { 
        this.currentTime = currentTime;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void update(String videoId, boolean isPlaying, double currentTime) {
        this.videoId = videoId;
        this.isPlaying = isPlaying;
        this.currentTime = currentTime;
        this.lastUpdated = System.currentTimeMillis();
    }
}