package com.example.eventmanager;

import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Represents an event in the lottery-based event management system.
 * Stores entrant lists (waiting, chosen, attendees) and registration window info.
 */
public class AnasEvent {

    private String eventId;
    private String name;
    private String description;
    private String location;

    private Date registrationStart;
    private Date registrationEnd;

    private int capacity;
    private String organizerId;
    private int waitlistLimit;

    private String posterUrl;

    // Entrant lists — stored as device ID strings, mirroring Firestore arrays
    private List<String> waitingList = new ArrayList<>();
    private List<String> chosenList  = new ArrayList<>();
    private List<String> attendees   = new ArrayList<>();
    private List<String> declinedList = new ArrayList<>();

    /** Required empty constructor for Firestore deserialization. */
    public AnasEvent() {}

    // Getters
    public String getEventId() { return eventId; }

    public Date getRegistrationStart() { return registrationStart; }

    public Date getRegistrationEnd() { return registrationEnd; }

    public String getName()              { return name; }
    public String getDescription()       { return description; }
    public String getLocation()          { return location; }
    public int    getCapacity()          { return capacity; }
    public String getOrganizerId()       { return organizerId; }
    public String getPosterUrl()         { return posterUrl; }

    public int getWaitlistLimit()        { return waitlistLimit; }

    public List<String> getWaitingList()  { return waitingList; }
    public List<String> getChosenList()   { return chosenList; }
    public List<String> getAttendees()    { return attendees; }
    public List<String> getDeclinedList() { return declinedList; }

    // Setters
    public void setEventId(String eventId)                   { this.eventId = eventId; }
    public void setName(String name)                         { this.name = name; }
    public void setDescription(String description)           { this.description = description; }
    public void setLocation(String location)                 { this.location = location; }
    public void setRegistrationStart(Date registrationStart) { this.registrationStart = registrationStart; }
    public void setRegistrationEnd(Date registrationEnd)     { this.registrationEnd = registrationEnd; }
    public void setCapacity(int capacity)                    { this.capacity = capacity; }
    public void setOrganizerId(String organizerId)           { this.organizerId = organizerId; }
    public void setPosterUrl(String posterUrl)               { this.posterUrl = posterUrl; }
    public void setWaitlistLimit(int waitlistLimit)          { this.waitlistLimit = waitlistLimit; }
    public void setWaitingList(List<String> waitingList)     { this.waitingList = waitingList; }
    public void setChosenList(List<String> chosenList)       { this.chosenList = chosenList; }
    public void setAttendees(List<String> attendees)         { this.attendees = attendees; }
    public void setDeclinedList(List<String> declinedList)   { this.declinedList = declinedList; }

    // Functionalities
    /**
     * US1: Checks whether registration is currently open based on the time window.
     * @return true if the current time is within [registrationStart, registrationEnd].
     */
    public boolean isRegistrationWindowActive() {
        Date now = new Date();
        if (registrationStart == null || registrationEnd == null) return false;
        return now.after(registrationStart) && now.before(registrationEnd);
    }

    /**
     * US1: Validates whether a user is permitted to join the waiting list.
     * Checks registration window, duplicate join, and waitlist capacity.
     *
     * @param deviceId     The ID of the entrant attempting to join.
     * @param waitlistLimit The max waitlist size (0 or null = unlimited).
     * @return true if joining is permitted.
     */
    public boolean canUserJoin(String deviceId, Integer waitlistLimit) {
        if (!isRegistrationWindowActive()) return false;
        if (waitingList != null && waitingList.contains(deviceId)) return false;
        if (waitlistLimit != null && waitlistLimit > 0) {
            if (waitingList != null && waitingList.size() >= waitlistLimit) return false;
        }
        return true;
    }

    /**
     * US3 & US4: Checks if a user has been selected by the lottery.
     * @param deviceId The device ID to check.
     * @return true if the user is in the chosen list.
     */
    public boolean isUserChosen(String deviceId) {
        return chosenList != null && chosenList.contains(deviceId);
    }

    /**
     * US4: Checks whether there is still space in the attendees list.
     * Ensures capacity is not exceeded when accepting an invitation.
     * @return true if attendees count is below capacity.
     */
    public boolean hasAttendeesSpace() {
        return attendees != null && attendees.size() < capacity;
    }
}
