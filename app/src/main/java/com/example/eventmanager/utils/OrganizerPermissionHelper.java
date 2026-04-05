package com.example.eventmanager.utils;
import com.example.eventmanager.models.Event;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Shared checks for who may act as an organizer for an event (primary or co-organizer).
 */
public final class OrganizerPermissionHelper {

    private OrganizerPermissionHelper() {}

    /**
     * Checks whether the given device is the primary organizer of the event.
     *
     * @param eventDoc the Firestore document snapshot representing the event
     * @param deviceId the device identifier to check
     * @return {@code true} if {@code deviceId} matches the event's {@code organizerId}
     */
    @SuppressWarnings("unchecked")
    public static boolean isPrimaryOrganizer(DocumentSnapshot eventDoc, String deviceId) {
        if (eventDoc == null || !eventDoc.exists() || deviceId == null || deviceId.isEmpty()) {
            return false;
        }
        String organizerId = eventDoc.getString("organizerId");
        return deviceId.equals(organizerId);
    }

    /**
     * Checks whether the given device is listed as a co-organizer of the event.
     *
     * @param eventDoc the Firestore document snapshot representing the event
     * @param deviceId the device identifier to check
     * @return {@code true} if {@code deviceId} appears in the event's
     *         {@code coOrganizers} list
     */
    @SuppressWarnings("unchecked")
    public static boolean isCoOrganizer(DocumentSnapshot eventDoc, String deviceId) {
        if (eventDoc == null || !eventDoc.exists() || deviceId == null || deviceId.isEmpty()) {
            return false;
        }
        List<String> co = (List<String>) eventDoc.get("coOrganizers");
        return co != null && co.contains(deviceId);
    }

    /**
     * Returns {@code true} if the device is either the primary organizer or a
     * co-organizer of the event.
     *
     * @param eventDoc the Firestore document snapshot representing the event
     * @param deviceId the device identifier to check
     * @return {@code true} if the device holds any organizer role for this event
     */
    public static boolean isOrganizerOrCoOrganizer(DocumentSnapshot eventDoc, String deviceId) {
        return isPrimaryOrganizer(eventDoc, deviceId) || isCoOrganizer(eventDoc, deviceId);
    }

    /**
     * Determines whether the device may perform organizer actions (e.g. posting
     * organizer comments, using organizer tools). If the event has no
     * {@code organizerId} set (legacy events), access is granted to preserve
     * backward compatibility.
     *
     * @param eventDoc the Firestore document snapshot representing the event
     * @param deviceId the device identifier to check
     * @return {@code true} if the device is permitted to act as an organizer
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
