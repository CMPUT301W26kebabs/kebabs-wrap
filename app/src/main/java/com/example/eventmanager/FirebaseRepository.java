package com.example.eventmanager;

import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static FirebaseRepository instance;

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }

    // ══════════════════════════════════════════════════════════════
    //  ADMIN CALLBACK INTERFACES (Ibrahim - US 03.x)
    // ══════════════════════════════════════════════════════════════
    public interface OnDocumentsLoadedListener {
        void onLoaded(List<DocumentSnapshot> documents);
        void onError(Exception e);
    }
    public interface OnDocumentLoadedListener {
        void onLoaded(DocumentSnapshot document);
        void onError(Exception e);
    }
    public interface OnOperationCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }

    // ══════════════════════════════════════════════════════════════
    //  UMAR'S METHODS (Lottery, Waitlist, Enrolled)
    // ══════════════════════════════════════════════════════════════

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

        eventRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Event event = documentSnapshot.toObject(Event.class);
                if (event != null) {
                    int maxCapacity = event.getMaxWaitlistCapacity();
                    if (maxCapacity > 0) {
                        waitlistRef.count().get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                long currentSize = task.getResult().getCount();
                                if (currentSize >= maxCapacity) {
                                    callback.onFailure("The waiting list is currently full.");
                                } else {
                                    executeAddUser(waitlistRef, entrant, callback);
                                }
                            } else {
                                callback.onFailure("Failed to verify current waitlist size.");
                            }
                        });
                    } else {
                        executeAddUser(waitlistRef, entrant, callback);
                    }
                }
            } else {
                callback.onFailure("Event does not exist.");
            }
        }).addOnFailureListener(e -> callback.onFailure("Database error: " + e.getMessage()));
    }

    private void executeAddUser(CollectionReference waitlistRef, Entrant entrant, WaitlistCallback callback) {
        waitlistRef.document(entrant.getDeviceId()).set(entrant)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to join waitlist: " + e.getMessage()));
    }

    /**
     * Fetches the final list of entrants who are fully enrolled in an event.
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

    // ══════════════════════════════════════════════════════════════
    //  IBRAHIM'S ADMIN METHODS (US 03.01, 03.02, 03.04, 03.05)
    // ══════════════════════════════════════════════════════════════

    /** Fetches all active events (US 03.04.01) */
    public void fetchAllActiveEvents(OnDocumentsLoadedListener listener) {
        db.collection("events").get().addOnSuccessListener(qs -> {
            List<DocumentSnapshot> active = new ArrayList<>();
            for (DocumentSnapshot doc : qs.getDocuments()) {
                Boolean isDeleted = doc.getBoolean("isDeleted");
                if (isDeleted == null || !isDeleted) active.add(doc);
            }
            listener.onLoaded(active);
        }).addOnFailureListener(listener::onError);
    }

    /** Fetches ALL events including deleted (admin toggle) */
    public void fetchAllEvents(OnDocumentsLoadedListener listener) {
        db.collection("events").get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    /** Fetches a single event by ID */
    public void fetchEventById(String eventId, OnDocumentLoadedListener listener) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    /** Fetches all active profiles (US 03.05.01) */
    public void fetchAllActiveProfiles(OnDocumentsLoadedListener listener) {
        db.collection("users").get().addOnSuccessListener(qs -> {
            List<DocumentSnapshot> active = new ArrayList<>();
            for (DocumentSnapshot doc : qs.getDocuments()) {
                Boolean isDisabled = doc.getBoolean("isDisabled");
                if (isDisabled == null || !isDisabled) active.add(doc);
            }
            listener.onLoaded(active);
        }).addOnFailureListener(listener::onError);
    }

    /** Fetches ALL profiles including disabled */
    public void fetchAllProfiles(OnDocumentsLoadedListener listener) {
        db.collection("users").get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    /** Fetches a single profile by deviceId */
    public void fetchProfileById(String deviceId, OnDocumentLoadedListener listener) {
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    /** Soft-deletes an event (US 03.01.01) */
    public void softDeleteEvent(String eventId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection("events").document(eventId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /** Restores a soft-deleted event */
    public void restoreEvent(String eventId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", false);
        updates.put("removedBy", FieldValue.delete());
        updates.put("removedAt", FieldValue.delete());
        db.collection("events").document(eventId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /** Soft-disables a profile (US 03.02.01) */
    public void softDeleteProfile(String deviceId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection("users").document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /** Restores a disabled profile */
    public void restoreProfile(String deviceId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", false);
        updates.put("removedBy", FieldValue.delete());
        updates.put("removedAt", FieldValue.delete());
        db.collection("users").document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void updateEventPosterUrl(String eventId, String posterUrl, com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("posterUrl", posterUrl);
        db.collection("events").document(eventId).update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void saveEvent(Event event, com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("events").document(event.getEventId()).set(event)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void getEventsByOrganizer(String organizerId, com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.QuerySnapshot> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("events").whereEqualTo("organizerId", organizerId).get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void createEvent(Event event, com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("events").document(event.getEventId()).set(event)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public FirebaseFirestore getDb() { return db; }
}
