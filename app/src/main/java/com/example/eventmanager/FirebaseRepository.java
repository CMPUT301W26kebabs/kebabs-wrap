package com.example.eventmanager;

import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FirebaseRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Attempts to add an entrant to an event's waiting list, respecting the
     * optional capacity limit.
     *
     * @param eventId  The ID of the event.
     * @param entrant  The entrant trying to join.
     * @param callback Handles the success or failure response.
     */
    public void joinWaitingList(String eventId, Entrant entrant, WaitlistCallback callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        CollectionReference waitlistRef = eventRef.collection("waitingList");

        // Step 1: Fetch the Event document to check its rules
        eventRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Event event = documentSnapshot.toObject(Event.class);

                if (event != null) {
                    int maxCapacity = event.getMaxWaitlistCapacity();

                    // Step 2: Check if a limit exists (assuming > 0 means limited)
                    if (maxCapacity > 0) {

                        // Query the server for the current count of the sub-collection
                        waitlistRef.count().get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                long currentSize = task.getResult().getCount();

                                // Step 3: Enforce the limit
                                if (currentSize >= maxCapacity) {
                                    callback.onFailure("The waiting list is currently full.");
                                } else {
                                    // There is room, add the user
                                    executeAddUser(waitlistRef, entrant, callback);
                                }
                            } else {
                                callback.onFailure("Failed to verify current waitlist size.");
                            }
                        });

                    } else {
                        // No limit is set (e.g., maxCapacity is 0), add the user immediately
                        executeAddUser(waitlistRef, entrant, callback);
                    }
                }
            } else {
                callback.onFailure("Event does not exist.");
            }
        }).addOnFailureListener(e -> callback.onFailure("Database error: " + e.getMessage()));
    }

    /**
     * Helper method to perform the actual database write.
     */
    private void executeAddUser(CollectionReference waitlistRef, Entrant entrant, WaitlistCallback callback) {
        // Use the user's Device ID as the document ID in the sub-collection
        waitlistRef.document(entrant.getDeviceId()).set(entrant)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to join waitlist: " + e.getMessage()));
    }

    /**
     * Fetches the final list of entrants who are fully enrolled in an event.
     *
     * @param eventId  The ID of the event.
     * @param callback Handles the response containing the list of entrants.
     */
    public void getEnrolledEntrants(String eventId, EntrantListCallback callback) {
        db.collection("events").document(eventId).collection("enrolled")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Entrant> enrolledList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) {
                            enrolledList.add(entrant);
                        }
                    }
                    callback.onSuccess(enrolledList);
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to load enrolled list: " + e.getMessage()));
    }
}