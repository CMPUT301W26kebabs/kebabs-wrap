package com.example.eventmanager;

public class AnasEntrant {
    private String deviceId;
    private String name;
    private String email;
    private String status;
    private boolean sectionHeader;

    public AnasEntrant() {}

    public AnasEntrant(String deviceId, String name, String email, String status) {
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.status = status;
        this.sectionHeader = false;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSectionHeader() { return sectionHeader; }
    public void setSectionHeader(boolean sectionHeader) { this.sectionHeader = sectionHeader; }
}
