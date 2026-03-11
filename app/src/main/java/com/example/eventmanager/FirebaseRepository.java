package com.example.eventmanager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void createEvent(Event event, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("events")
                .document(event.getEventId())
                .set(event)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }
    /**
     * Updates the posterUrl field for a specific Event document.
     * Pass null for posterUrl to remove the reference.
     */
    public void updateEventPosterUrl(String eventId, String posterUrl,
                                     OnSuccessListener<Void> onSuccess,
                                     OnFailureListener onFailure) {
        db.collection("events").document(eventId)
                .update("posterUrl", posterUrl)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
    public void getEventsByOrganizer(String organizerId,
                                     OnSuccessListener<QuerySnapshot> onSuccess,
                                     OnFailureListener onFailure) {
        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
