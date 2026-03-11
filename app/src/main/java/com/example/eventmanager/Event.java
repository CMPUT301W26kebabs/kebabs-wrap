package com.example.eventmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Represents an event where entrants join a waiting list
 * and may be selected to register.
 */
public class Event {

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

    // Lists
    private List<String> waitingList = new ArrayList<>();
    private List<String> chosenList = new ArrayList<>();
    private List<String> attendees = new ArrayList<>();

    // Required empty constructor for Firestore
    public Event() {}

    // Getters
    public String getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Date getRegistrationStart() {
        return registrationStart;
    }

    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    public int getCapacity() {
        return capacity;
    }
    public int getWaitingListCount() {return waitlistLimit;}

    public String getOrganizerId() {
        return organizerId;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public List<String> getWaitingList() {
        return waitingList;
    }

    public List<String> getChosenList() {
        return chosenList;
    }

    public List<String> getAttendees() {
        return attendees;
    }

    // Setters
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setName(String name) { this.name = name; }
    public void setRegistrationStart(Date registrationStart) { this.registrationStart = registrationStart; }
    public void setRegistrationEnd(Date registrationEnd) { this.registrationEnd = registrationEnd; }
    public void setWaitlistLimit(int waitlistLimit) { this.waitlistLimit = waitlistLimit; }
    public void setDescription(String description) { this.description = description;}
    public void setCapacity(int capacity) { this.capacity = capacity;}
    public void setLocation(String location) {this.location = location;}
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl;}
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId;}

    public void setWaitingList(List<String> waitingList) { this.waitingList = waitingList;}

    public void setAttendees(List<String> attendees) { this.attendees = attendees;}

    public void setChosenList(List<String> chosenList) { this.chosenList = chosenList;}

    /**
     * Checks if the button should be visible/enabled.
     * @return true if within time window
     */
    public boolean isRegistrationWindowActive() {
        Date now = new Date();
        if (registrationStart == null || registrationEnd == null) return false;
        return now.after(registrationStart) && now.before(registrationEnd);
    }

    /**
     * Validates if a specific user is allowed to join.
     * @param deviceId The ID of the entrant attempting to join.
     * @param waitlistLimit The max size allowed (if any).
     * @return true if joining is permitted.
     */
    public boolean canUserJoin(String deviceId, Integer waitlistLimit) {

        // Checking registration window
        if (!isRegistrationWindowActive()) {return false;}

        // Checking if already on list
        if (waitingList != null && waitingList.contains(deviceId)) {return false;}

        // Checking if waitlist full
        if (waitlistLimit != null && waitlistLimit > 0) {
            if (waitingList != null && waitingList.size() >= waitlistLimit) {return false;}
        }

        return true;
    }

    /**
     * Logic for User Story 3 & 4 (View Chosen & Accept Invitation)
     * Checks if a user has been selected by the lottery.
     */
    public boolean isUserChosen(String deviceId) {
        return chosenList != null && chosenList.contains(deviceId);
    }

    /**
     * Logic for User Story 4 (Accept Invitation)
     * Ensures the event hasn't filled up since the user was invited.
     */
    public boolean AttendeesSpace() {
        return attendees != null && attendees.size() < capacity;
    }

}
