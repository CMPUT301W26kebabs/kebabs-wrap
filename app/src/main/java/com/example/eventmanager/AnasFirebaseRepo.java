import com.example.eventmanager.Entrant;
import com.example.eventmanager.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all Firestore database operations.
 * Separates backend connectivity from UI logic.
 */
public class FirebaseRepository {

    private final FirebaseFirestore db;

    public FirebaseRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // US1: Join Waiting List
    /**
     * Atomically adds a user to the event's waiting list.
     * Checks: event exists, user not already joined, waitlist not full.
     *
     * @param eventId  The Firestore document ID of the event.
     * @param deviceId The device ID of the entrant joining.
     * @return A Task that completes on success or fails with a descriptive exception.
     */
    public Task<Void> joinWaitingList(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        return db.runTransaction(transaction -> {
            Event event = transaction.get(eventRef).toObject(Event.class);

            if (event == null) {
                throw new FirebaseFirestoreException("Event not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            if (event.getWaitingList().contains(deviceId)
                    || event.getAttendees().contains(deviceId)) {
                throw new FirebaseFirestoreException("User already registered",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            int limit = event.getWaitlistLimit();
            if (limit > 0 && event.getWaitingList().size() >= limit) {
                throw new FirebaseFirestoreException("Waitlist is full",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            if (!event.isRegistrationWindowActive()) {
                throw new FirebaseFirestoreException("Registration is currently closed",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            transaction.update(eventRef, "waitingList", FieldValue.arrayUnion(deviceId));
            return null;
        });
    }


    // US2 & US3: Real-time event listener (used by organizer screens)
    /**
     * Attaches a real-time Firestore listener to an event document.
     * Calls the provided callback whenever the event data changes.
     *
     * @param eventId  The Firestore document ID of the event.
     * @param callback A callback that receives the updated Event object.
     * @return A ListenerRegistration that should be removed when the Activity is destroyed.
     */
    public ListenerRegistration listenToEvent(String eventId, EventCallback callback) {
        return db.collection("events").document(eventId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    Event event = snapshot.toObject(Event.class);
                    if (event != null) {
                        callback.onEvent(event);
                    }
                });
    }

    /**
     * Callback interface for real-time event updates.
     */
    public interface EventCallback {
        void onEvent(Event event);
    }

    // US2 & US3: Fetch entrant profiles by device ID list
    /**
     * Fetches full Entrant profiles from Firestore for a list of device IDs.
     * Used by the organizer to display names/emails in the waiting and chosen lists.
     * Note: Firestore 'whereIn' is limited to 10 items; batch for larger lists in Part 4.
     *
     * @param deviceIds List of device IDs to look up.
     * @return A Task resolving to a list of Entrant objects.
     */
    public Task<List<Entrant>> getEntrantProfiles(List<String> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Tasks.forResult(new ArrayList<>());
        }

        // Firestore 'in' queries support max 10 items
        List<String> safeIds = deviceIds.size() > 10 ? deviceIds.subList(0, 10) : deviceIds;

        return db.collection("users")
                .whereIn("deviceId", safeIds)
                .get()
                .continueWith(task -> {
                    List<Entrant> entrants = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Entrant e = doc.toObject(Entrant.class);
                            if (e != null) entrants.add(e);
                        }
                    }
                    return entrants;
                });
    }


    // US4: Accept Invitation
    /**
     * Atomically moves a user from the chosenList to attendees.
     * Checks: event exists, user is in chosen list, capacity not exceeded.
     *
     * @param eventId  The Firestore document ID of the event.
     * @param deviceId The device ID of the entrant accepting the invitation.
     * @return A Task that completes on success or fails with a descriptive exception.
     */
    public Task<Void> acceptInvitation(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            Event event = snapshot.toObject(Event.class);

            if (event == null) {
                throw new FirebaseFirestoreException("Event not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            if (!event.isUserChosen(deviceId)) {
                throw new FirebaseFirestoreException("User not in chosen list",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            if (!event.hasAttendeesSpace()) {
                throw new FirebaseFirestoreException("Event is at maximum capacity",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(eventRef, "chosenList", FieldValue.arrayRemove(deviceId));
            transaction.update(eventRef, "attendees", FieldValue.arrayUnion(deviceId));
            return null;
        });
    }

    /**
     * Atomically moves a user from the chosenList back to the waiting list (decline).
     * Used alongside US4 — declining triggers replacement draw eligibility.
     *
     * @param eventId  The Firestore document ID of the event.
     * @param deviceId The device ID of the entrant declining the invitation.
     * @return A Task that completes on success or fails with a descriptive exception.
     */
    public Task<Void> declineInvitation(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            Event event = snapshot.toObject(Event.class);

            if (event == null) {
                throw new FirebaseFirestoreException("Event not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            if (!event.isUserChosen(deviceId)) {
                throw new FirebaseFirestoreException("User not in chosen list",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            transaction.update(eventRef, "chosenList", FieldValue.arrayRemove(deviceId));
            transaction.update(eventRef, "declinedList", FieldValue.arrayUnion(deviceId));
            return null;
        });
    }


    // Utility
    /**
     * Fetches a single event document once (non-realtime).
     *
     * @param eventId The Firestore document ID of the event.
     * @return A Task resolving to the Event object, or null if not found.
     */
    public Task<Event> getEvent(String eventId) {
        return db.collection("events").document(eventId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(Event.class);
                    }
                    return null;
                });
    }
}
