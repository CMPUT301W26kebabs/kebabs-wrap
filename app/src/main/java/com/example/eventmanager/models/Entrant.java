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

    /**
     * Constructs an Entrant with profile info and a status tab label for UI display
     * in {@link ManageEventActivity}.
     *
     * @param deviceId       the unique hardware ID of the user's device.
     * @param name           the user's display name.
     * @param email          the user's email address.
     * @param statusTabLabel label indicating which lottery tab this entrant belongs to
     *                       (e.g. "Waiting", "Selected", "Enrolled", "Cancelled").
     */
    public Entrant(String deviceId, String name, String email, String statusTabLabel) {
        this();
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.statusTabLabel = statusTabLabel;
    }

    /**
     * Returns the lottery-status tab label used by the manage-event UI to group entrants.
     *
     * @return the tab label string (e.g. "Waiting", "Selected"), or {@code null} if unset.
     */
    public String getStatusTabLabel() { return statusTabLabel; }

    /**
     * Sets the lottery-status tab label for UI grouping in the manage-event screen.
     *
     * @param statusTabLabel the tab label to assign.
     */
    public void setStatusTabLabel(String statusTabLabel) { this.statusTabLabel = statusTabLabel; }

    /**
     * Indicates whether this instance is a section-header placeholder rather than a real entrant,
     * used by adapters in the manage-event list to render group dividers.
     *
     * @return {@code true} if this is a section header, {@code false} for a real entrant.
     */
    public boolean isSectionHeader() { return isSectionHeader; }

    /**
     * Marks this instance as a section-header placeholder for adapter rendering.
     *
     * @param sectionHeader {@code true} to treat as a section header.
     */
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

    /**
     * Sets the user's notification preference.
     *
     * @param receiveNotifications {@code true} to opt in, {@code false} to opt out.
     */
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

    /**
     * Returns the raw disabled flag as stored in Firestore. May be {@code null} for
     * users created before the field existed.
     *
     * @return {@code Boolean.TRUE} if admin-disabled, {@code Boolean.FALSE} or {@code null} otherwise.
     */
    public Boolean getIsDisabled() {
        return isDisabled;
    }

    /**
     * Sets the admin soft-disable flag for this user profile.
     *
     * @param disabled {@code true} to disable, {@code false} to enable.
     */
    public void setIsDisabled(Boolean disabled) {
        this.isDisabled = disabled;
    }

    /**
     * Convenience check that resolves the nullable {@code isDisabled} flag.
     * Returns {@code true} only when the admin has explicitly soft-disabled this profile.
     *
     * @return {@code true} if this profile is disabled, {@code false} otherwise.
     */
    public boolean isProfileDisabled() {
        return isDisabled != null && isDisabled;
    }
}