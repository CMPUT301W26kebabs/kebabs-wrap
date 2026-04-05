package com.example.eventmanager.models;

import com.example.eventmanager.ui.ManageEventActivity;

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
    /**
     * When {@code null} or {@code true}, the user receives notifications; {@code false} opts out (US 01.04.03).
     */
    private Boolean receiveNotifications = true;
    private boolean isAdmin;
    private boolean isOrganizer;
    private String photoUrl;
    /**
     * Legacy soft-disable by admin; when true, sign-in should be blocked.
     * Hard-deleted users have no document instead.
     */
    private Boolean isDisabled;

    // Transient UI properties used by ManageEventActivity adapters
    private transient String statusTabLabel;
    private transient boolean isSectionHeader;

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

    public Entrant(String deviceId, String name, String email, String statusTabLabel) {
        this();
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.statusTabLabel = statusTabLabel;
    }

    public String getStatusTabLabel() { return statusTabLabel; }
    public void setStatusTabLabel(String statusTabLabel) { this.statusTabLabel = statusTabLabel; }

    public boolean isSectionHeader() { return isSectionHeader; }
    public void setSectionHeader(boolean sectionHeader) { this.isSectionHeader = sectionHeader; }

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
     * Gets the user's photo URL.
     * @return The photo URL string.
     */
    public String getPhotoUrl() { return photoUrl; }

    /**
     * Sets the user's photo URL.
     * @param photoUrl The new photo URL string.
     */
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    /**
     * Whether the user wants to receive notifications. Defaults to {@code true} when unset in Firestore.
     */
    public boolean isReceiveNotifications() {
        return receiveNotifications == null || receiveNotifications;
    }

    public void setReceiveNotifications(boolean receiveNotifications) {
        this.receiveNotifications = receiveNotifications;
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

    public Boolean getIsDisabled() {
        return isDisabled;
    }

    public void setIsDisabled(Boolean disabled) {
        this.isDisabled = disabled;
    }

    /** True when admin soft-disabled this profile (still in Firestore). */
    public boolean isProfileDisabled() {
        return isDisabled != null && isDisabled;
    }
}