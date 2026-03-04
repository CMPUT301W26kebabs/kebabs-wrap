package com.example.eventmanager.models;

/**
 * Represents a user of the application, encompassing both standard entrants and organizers/admins.
 * This class serves as a pure data model (POJO) to be serialized and deserialized by Firestore.
 * * It contains personal information and system roles, uniquely identified by the device's hardware ID.
 */
public class Entrant {

    private String deviceId;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean isAdmin;
    private boolean isOrganizer;

    /**
     * Empty constructor required by Firebase Firestore for automatic data mapping.
     */
    public Entrant() {
    }

    /**
     * Constructs a new Entrant with the required unique identifier.
     *
     * @param deviceId The unique hardware ID of the user's Android device.
     */
    public Entrant(String deviceId) {
        this.deviceId = deviceId;
        this.isAdmin = false;
        this.isOrganizer = false;
    }

    /**
     * Gets the user's unique device ID.
     * @return The device ID string.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the user's unique device ID.
     * @param deviceId The new device ID string.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the user's full name.
     * @return The name string.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's full name.
     * @param name The new name string.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's email address.
     * @return The email string.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     * @param email The new email string.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's phone number.
     * @return The phone number string.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the user's phone number.
     * @param phoneNumber The new phone number string.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Checks if the user has administrative privileges.
     * @return True if the user is an admin, false otherwise.
     */
    public boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Sets the administrative privilege status of the user.
     * @param admin True to grant admin privileges, false to revoke.
     */
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    /**
     * Checks if the user has organizer privileges.
     * @return True if the user is an organizer, false otherwise.
     */
    public boolean isOrganizer() {
        return isOrganizer;
    }

    /**
     * Sets the organizer privilege status of the user.
     * @param organizer True to grant organizer privileges, false to revoke.
     */
    public void setOrganizer(boolean organizer) {
        isOrganizer = organizer;
    }
}