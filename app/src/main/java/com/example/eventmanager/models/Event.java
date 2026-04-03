package com.example.eventmanager.models;
import java.util.Date;

/**
 * Represents an event within the lottery system.
 * This class stores the configuration and details of an event but delegates
 * the management of entrant lists (waiting list, selected, enrolled) to Firestore sub-collections.
 */
public class Event {

    private String eventId;
    private String name;
    private String description;
    private String location;
    /** When the event actually runs (distinct from registration window). */
    private Date startDate;
    private Date endDate;
    private Date registrationStart;
    private Date registrationEnd;
    private int capacity;
    private int maxWaitlistCapacity;
    private String posterUrl;
    private String organizerId;
    private boolean isGeolocationRequired;
    /** Invitation-only / private: hide from public home & browse; not broadcast to followers. */
    private boolean privateEvent;

    /**
     * Empty constructor required by Firebase Firestore for automatic data mapping.
     */
    public Event() {
    }

    /**
     * Gets the unique identifier for the event.
     * @return The event ID string.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the unique identifier for the event.
     * @param eventId The new event ID string.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the name of the event.
     * @return The event name string.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event.
     * @param name The new event name string.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the detailed description of the event.
     * @return The event description string.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the detailed description of the event.
     * @param description The new event description string.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the event location/address.
     * @return The location string.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the event location/address.
     * @param location The new location string.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets the start date and time for event registration.
     * @return The registration start Date object.
     */
    public Date getRegistrationStart() {
        return registrationStart;
    }

    /**
     * Sets the start date and time for event registration.
     * @param registrationStart The new registration start Date object.
     */
    public void setRegistrationStart(Date registrationStart) {
        this.registrationStart = registrationStart;
    }

    /**
     * Gets the end date and time for event registration.
     * @return The registration end Date object.
     */
    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    /**
     * Sets the end date and time for event registration.
     * @param registrationEnd The new registration end Date object.
     */
    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    /**
     * Gets the maximum number of attendees allowed to enroll in the event.
     * @return The event capacity integer.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the maximum number of attendees allowed to enroll in the event.
     * @param capacity The new event capacity integer.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Gets the maximum number of entrants allowed on the waiting list.
     * @return The max waitlist capacity integer.
     */
    public int getMaxWaitlistCapacity() {
        return maxWaitlistCapacity;
    }

    /**
     * Sets the maximum number of entrants allowed on the waiting list.
     * @param maxWaitlistCapacity The new max waitlist capacity integer.
     */
    public void setMaxWaitlistCapacity(int maxWaitlistCapacity) {
        this.maxWaitlistCapacity = maxWaitlistCapacity;
    }

    /**
     * Gets the URL of the uploaded event poster image.
     * @return The poster URL string.
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Sets the URL of the uploaded event poster image.
     * @param posterUrl The new poster URL string.
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /**
     * Gets the device ID of the user who organized the event.
     * @return The organizer's device ID string.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the device ID of the user who organized the event.
     * @param organizerId The new organizer's device ID string.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Checks if geolocation verification is required to join the waiting list.
     * @return True if geolocation is required, false otherwise.
     */
    public boolean isGeolocationRequired() {
        return isGeolocationRequired;
    }

    /**
     * Sets whether geolocation verification is required to join the waiting list.
     * @param geolocationRequired True to require geolocation, false otherwise.
     */
    public void setGeolocationRequired(boolean geolocationRequired) {
        isGeolocationRequired = geolocationRequired;
    }

    public boolean isPrivateEvent() {
        return privateEvent;
    }

    public void setPrivateEvent(boolean privateEvent) {
        this.privateEvent = privateEvent;
    }
}