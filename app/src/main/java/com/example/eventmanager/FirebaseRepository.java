package com.example.eventmanager;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static FirebaseRepository instance;

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }

    // ══════════════════════════════════════════════════════════════
    //  CALLBACK INTERFACES
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
    //  USER MANAGEMENT (Anas)
    // ══════════════════════════════════════════════════════════════

    public void saveUser(@NonNull String deviceId) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("deviceId", deviceId);
        if (deviceId.equals("5c7640b6f0f59181")) {
            userData.put("name", "OrgName");
            userData.put("isOrganizer", true);
        } else {
            userData.put("name", "UserName");
            userData.put("isOrganizer", false);
        }
        userData.put("email", "");
        userData.put("phoneNumber", "");
        userData.put("isAdmin", false);

        db.collection("users")
                .document(deviceId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "User saved for deviceId: " + deviceId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save user: " + deviceId, e));
    }

    // ══════════════════════════════════════════════════════════════
    //  EVENTS, QR, POSTERS (Huzaifa)
    // ══════════════════════════════════════════════════════════════

    public void createEvent(Event event, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("events").document(event.getEventId()).set(event)
                .addOnSuccessListener(success).addOnFailureListener(failure);
    }

    public void updateEventPosterUrl(String eventId, String posterUrl,
                                     OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("events").document(eventId).update("posterUrl", posterUrl)
                .addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    public void getEventsByOrganizer(String organizerId,
                                     OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("events").whereEqualTo("organizerId", organizerId).get()
                .addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /**
     * Events the user can manage: primary organizer ({@code organizerId}) or co-organizer ({@code coOrganizers}).
     * Merges two queries and de-duplicates by document id.
     */
    public void getEventsForOrganizerDashboard(@NonNull String deviceId,
                                              @NonNull OnSuccessListener<List<DocumentSnapshot>> onSuccess,
                                              @NonNull OnFailureListener onFailure) {
        db.collection("events").whereEqualTo("organizerId", deviceId).get()
                .addOnSuccessListener(qPrimary -> {
                    db.collection("events").whereArrayContains("coOrganizers", deviceId).get()
                            .addOnSuccessListener(qCo -> {
                                Map<String, DocumentSnapshot> byId = new LinkedHashMap<>();
                                if (qPrimary != null) {
                                    for (DocumentSnapshot d : qPrimary.getDocuments()) {
                                        byId.put(d.getId(), d);
                                    }
                                }
                                if (qCo != null) {
                                    for (DocumentSnapshot d : qCo.getDocuments()) {
                                        byId.put(d.getId(), d);
                                    }
                                }
                                onSuccess.onSuccess(new ArrayList<>(byId.values()));
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    // ══════════════════════════════════════════════════════════════
    //  WAITLIST & ENROLLED (Umar - Lottery)
    // ══════════════════════════════════════════════════════════════

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
                                } else { executeAddUser(waitlistRef, entrant, callback); }
                            } else { callback.onFailure("Failed to verify current waitlist size."); }
                        });
                    } else { executeAddUser(waitlistRef, entrant, callback); }
                }
            } else { callback.onFailure("Event does not exist."); }
        }).addOnFailureListener(e -> callback.onFailure("Database error: " + e.getMessage()));
    }

    private void executeAddUser(CollectionReference waitlistRef, Entrant entrant, WaitlistCallback callback) {
        waitlistRef.document(entrant.getDeviceId()).set(entrant)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to join waitlist: " + e.getMessage()));
    }

    public void getEnrolledEntrants(String eventId, EntrantListCallback callback) {
        db.collection("events").document(eventId).collection("enrolled").get()
                .addOnSuccessListener(qs -> {
                    List<Entrant> enrolledList = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) enrolledList.add(entrant);
                    }
                    callback.onSuccess(enrolledList);
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to load enrolled list: " + e.getMessage()));
    }

    // ══════════════════════════════════════════════════════════════
    //  ADMIN — EVENTS (Ibrahim - US 03.01, 03.04)
    // ══════════════════════════════════════════════════════════════

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

    public void fetchAllEvents(OnDocumentsLoadedListener listener) {
        db.collection("events").get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    public void fetchEventById(String eventId, OnDocumentLoadedListener listener) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    public void softDeleteEvent(String eventId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection("events").document(eventId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    public void restoreEvent(String eventId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", false);
        updates.put("removedBy", FieldValue.delete());
        updates.put("removedAt", FieldValue.delete());
        db.collection("events").document(eventId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    // ══════════════════════════════════════════════════════════════
    //  ADMIN — PROFILES (Ibrahim - US 03.02, 03.05)
    // ══════════════════════════════════════════════════════════════

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

    public void fetchAllProfiles(OnDocumentsLoadedListener listener) {
        db.collection("users").get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    public void fetchProfileById(String deviceId, OnDocumentLoadedListener listener) {
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    public void softDeleteProfile(String deviceId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection("users").document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    public void restoreProfile(String deviceId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", false);
        updates.put("removedBy", FieldValue.delete());
        updates.put("removedAt", FieldValue.delete());
        db.collection("users").document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════════════════════════════

    public FirebaseFirestore getDb() { return db; }
}
