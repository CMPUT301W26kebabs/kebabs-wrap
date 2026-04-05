package com.example.eventmanager;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Shared checks for who may act as an organizer for an event (primary or co-organizer).
 */
public final class OrganizerPermissionHelper {

    private OrganizerPermissionHelper() {}

    @SuppressWarnings("unchecked")
    public static boolean isPrimaryOrganizer(DocumentSnapshot eventDoc, String deviceId) {
        if (eventDoc == null || !eventDoc.exists() || deviceId == null || deviceId.isEmpty()) {
            return false;
        }
        String organizerId = eventDoc.getString("organizerId");
        return deviceId.equals(organizerId);
    }

    @SuppressWarnings("unchecked")
    public static boolean isCoOrganizer(DocumentSnapshot eventDoc, String deviceId) {
        if (eventDoc == null || !eventDoc.exists() || deviceId == null || deviceId.isEmpty()) {
            return false;
        }
        List<String> co = (List<String>) eventDoc.get("coOrganizers");
        return co != null && co.contains(deviceId);
    }

    /**
     * True if the user is the {@code organizerId} or listed in {@code coOrganizers}.
     */
    public static boolean isOrganizerOrCoOrganizer(DocumentSnapshot eventDoc, String deviceId) {
        return isPrimaryOrganizer(eventDoc, deviceId) || isCoOrganizer(eventDoc, deviceId);
    }

    /**
     * Who may post organizer comments / use organizer tools when an event has an organizer set.
     * If {@code organizerId} is missing, legacy events stay open (prior behavior).
     */
    @SuppressWarnings("unchecked")
    public static boolean canActAsOrganizer(DocumentSnapshot eventDoc, String deviceId) {
        if (eventDoc == null || !eventDoc.exists() || deviceId == null || deviceId.isEmpty()) {
            return false;
        }
        String organizerId = eventDoc.getString("organizerId");
        if (organizerId == null || organizerId.trim().isEmpty()) {
            return true;
        }
        if (deviceId.equals(organizerId)) {
            return true;
        }
        List<String> co = (List<String>) eventDoc.get("coOrganizers");
        return co != null && co.contains(deviceId);
    }
}
