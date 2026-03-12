package com.example.eventmanager;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.example.eventmanager.models.Entrant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the business logic for sampling entrants from the event waiting list.
 */
public class LotteryManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Randomly samples a target number of entrants from the waiting list and moves them
     * to the 'selected' sub-collection.
     *
     * @param eventId     The ID of the event.
     * @param targetCount The number of entrants to sample.
     * @param callback    Handles the success or failure response.
     */
    public void drawWinners(String eventId, int targetCount, LotteryCallback callback) {
        CollectionReference waitlistRef = db.collection("events").document(eventId).collection("waitingList");
        CollectionReference selectedRef = db.collection("events").document(eventId).collection("selected");

        // Step 1: Fetch everyone currently on the waiting list
        waitlistRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Entrant> allWaitlisted = new ArrayList<>();

                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Entrant entrant = doc.toObject(Entrant.class);
                    if (entrant != null) {
                        allWaitlisted.add(entrant);
                    }
                }

                // Step 2: Check if there's anyone to draw
                if (allWaitlisted.isEmpty()) {
                    callback.onFailure("The waiting list is empty.");
                    return;
                }

                // Step 3: The Lottery Math (Shuffle and Pick)
                Collections.shuffle(allWaitlisted);

                // If the target count is greater than the waitlist size, just select everyone
                int winnersCount = Math.min(targetCount, allWaitlisted.size());
                List<Entrant> winners = allWaitlisted.subList(0, winnersCount);

                // Step 4: Execute the database moves as a single, safe Batch Transaction
                WriteBatch batch = db.batch();

                for (Entrant winner : winners) {
                    DocumentReference oldDoc = waitlistRef.document(winner.getDeviceId());
                    DocumentReference newDoc = selectedRef.document(winner.getDeviceId());

                    batch.delete(oldDoc); // Remove from waitlist
                    batch.set(newDoc, winner); // Add to selected list
                }

                batch.commit().addOnSuccessListener(aVoid -> {
                    // Success! Return the list of winners so the UI can update
                    callback.onSuccess(winners);
                }).addOnFailureListener(e -> {
                    callback.onFailure("Failed to process lottery results: " + e.getMessage());
                });

            } else {
                callback.onFailure("Failed to fetch waiting list data.");
            }
        });
    }
    /**
     * Draws a single replacement entrant from the waiting list.
     * This is typically triggered when a previously selected user declines their invitation or cancels.
     *
     * @param eventId  The ID of the event.
     * @param callback Handles the success or failure response.
     */
    public void drawReplacement(String eventId, LotteryCallback callback) {
        // A replacement draw is literally just running the lottery engine for exactly 1 person!
        drawWinners(eventId, 1, callback);
    }
}
