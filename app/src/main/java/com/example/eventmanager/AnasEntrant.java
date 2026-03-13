package com.example.eventmanager;

/**
 * This is a class that represents a user of the system.
 * Can act as an Entrant, Organizer, or Admin depending on the role flag given.
 */
public class AnasEntrant {
    private String deviceId;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean isAdmin;
    private boolean isOrganizer;
    public Entrant() {}

    public Entrant(String deviceId, String name, String email,
                   String phoneNumber, boolean isAdmin, boolean isOrganizer) {
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isAdmin = isAdmin;
        this.isOrganizer = isOrganizer;
    }

    // Getters
    public String getDeviceId() {
        return deviceId;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public boolean isAdmin() {
        return isAdmin;
    }
    public boolean isOrganizer() {
        return isOrganizer;
    }
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }
    public void setOrganizer(boolean organizer) {
        this.isOrganizer = organizer;
    }
}
