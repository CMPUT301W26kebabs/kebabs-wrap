package com.example.eventmanager;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
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

    /** Matches {@code EventRepository} / notification routing: field {@code deviceId} or document id. */
    private static String deviceIdFromWaitlistDoc(@NonNull DocumentSnapshot doc) {
        String fromField = doc.getString("deviceId");
        if (fromField != null && !fromField.trim().isEmpty()) {
            return fromField.trim();
        }
        return doc.getId();
    }

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
                List<DocumentSnapshot> allDocs = new ArrayList<>(task.getResult().getDocuments());

                if (allDocs.isEmpty()) {
                    callback.onFailure("The waiting list is empty.");
                    return;
                }

                Collections.shuffle(allDocs);

                int winnersCount = Math.min(targetCount, allDocs.size());
                List<DocumentSnapshot> winnerDocs = allDocs.subList(0, winnersCount);

                WriteBatch batch = db.batch();
                List<Entrant> winners = new ArrayList<>();
                List<String> winnerDeviceIds = new ArrayList<>();

                for (DocumentSnapshot doc : winnerDocs) {
                    Entrant entrant = doc.toObject(Entrant.class);
                    if (entrant == null) {
                        entrant = new Entrant();
                    }
                    String id = deviceIdFromWaitlistDoc(doc);
                    entrant.setDeviceId(id);

                    // Always delete the exact waitlist document we drew (doc id may differ from deviceId field).
                    batch.delete(doc.getReference());
                    batch.set(selectedRef.document(id), entrant);
                    winners.add(entrant);
                    winnerDeviceIds.add(id);
                }

                final List<Entrant> winnersOut = winners;
                final List<String> idsOut = new ArrayList<>(winnerDeviceIds);
                batch.commit().addOnSuccessListener(aVoid -> callback.onSuccess(winnersOut, idsOut))
                        .addOnFailureListener(e ->
                                callback.onFailure("Failed to process lottery results: " + e.getMessage()));

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
