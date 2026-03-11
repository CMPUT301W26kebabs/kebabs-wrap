package com.example.eventmanager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

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

    /**
     * User Story 1: Join Waiting List
     * Uses a transaction to fetch the event, check your business logic, and safely add the user.
     */
    public Task<Void> joinWaitingList(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            Event event = snapshot.toObject(Event.class);

            if (event == null) {
                throw new FirebaseFirestoreException("Event not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Event Class Logic
            if (!event.canUserJoin(deviceId, event.getWaitingListCount())) {
                throw new FirebaseFirestoreException("Registration closed or waitlist full", FirebaseFirestoreException.Code.ABORTED);
            }

            // Automatically add the user to the array
            transaction.update(eventRef, "waitingList", FieldValue.arrayUnion(deviceId));
            return null;
        });
    }

    /**
     * User Story 4: Accept Invitation to Register
     * Uses a transaction to move the user from chosenList to attendees, checking capacity.
     */
    public Task<Void> acceptInvitation(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            Event event = snapshot.toObject(Event.class);

            if (event == null) {
                throw new FirebaseFirestoreException("Event not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Verify if actually chosen
            if (!event.isUserChosen(deviceId)) {
                throw new FirebaseFirestoreException("User not in chosen list", FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            // Verify event hasn't filled up
            if (!event.AttendeesSpace()) {
                throw new FirebaseFirestoreException("Event is at maximum capacity", FirebaseFirestoreException.Code.ABORTED);
            }

            // Move the user: Remove from chosen, Add to attendees
            transaction.update(eventRef, "chosenList", FieldValue.arrayRemove(deviceId));
            transaction.update(eventRef, "attendees", FieldValue.arrayUnion(deviceId));
            return null;
        });
    }

    /**
     * User Story 2 & 3: View Waiting List / View Chosen Entrants
     * Since the Event only stores deviceIds, this method fetches the actual Entrant profiles
     * from the "users" collection so the Organizer can see their names and details.
     */
    public Task<List<Entrant>> getEntrantProfiles(List<String> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Tasks.forResult(new ArrayList<>());
        }

        // Note: Firestore 'in' queries are limited to 10 items at a time.
        // For larger lists in Part 4, you will need to batch this, but this works perfectly for Part 3.
        return db.collection("users")
                .whereIn("deviceId", deviceIds)
                .get()
                .continueWith(task -> {
                    List<Entrant> entrants = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            entrants.add(doc.toObject(Entrant.class));
                        }
                    }
                    return entrants;
                });
    }

    /**
     * Utility: Fetch a single event to populate your UI
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
