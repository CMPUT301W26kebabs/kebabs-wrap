package com.example.eventmanager.repository;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";
    private static final String COLLECTION_USERS  = "users";
    private static final String COLLECTION_EVENTS = "events";

    private final FirebaseFirestore db;
    private static FirebaseRepository instance;

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }

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

    public interface OnCountListener {
        void onCount(int count);
    }

    public void fetchAllActiveEvents(OnDocumentsLoadedListener listener) {
        db.collection(COLLECTION_EVENTS)
                .whereEqualTo("isDeleted", false)
                .orderBy("registrationStart", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(e -> fetchAllActiveEventsFallback(listener));
    }

    private void fetchAllActiveEventsFallback(OnDocumentsLoadedListener listener) {
        db.collection(COLLECTION_EVENTS).get()
                .addOnSuccessListener(qs -> {
                    List<DocumentSnapshot> active = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted == null || !isDeleted) active.add(doc);
                    }
                    listener.onLoaded(active);
                })
                .addOnFailureListener(listener::onError);
    }

    public void fetchAllEvents(OnDocumentsLoadedListener listener) {
        db.collection(COLLECTION_EVENTS).get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    public void fetchEventById(String eventId, OnDocumentLoadedListener listener) {
        db.collection(COLLECTION_EVENTS).document(eventId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    public void getWaitingListCount(String eventId, OnCountListener listener) {
        db.collection(COLLECTION_EVENTS).document(eventId)
                .collection("waitingList").get()
                .addOnSuccessListener(qs -> listener.onCount(qs.size()))
                .addOnFailureListener(e -> listener.onCount(0));
    }

    public void fetchAllActiveProfiles(OnDocumentsLoadedListener listener) {
        db.collection(COLLECTION_USERS).get()
                .addOnSuccessListener(qs -> {
                    List<DocumentSnapshot> active = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Boolean isDisabled = doc.getBoolean("isDisabled");
                        if (isDisabled == null || !isDisabled) active.add(doc);
                    }
                    listener.onLoaded(active);
                })
                .addOnFailureListener(listener::onError);
    }

    public void fetchAllProfiles(OnDocumentsLoadedListener listener) {
        db.collection(COLLECTION_USERS).get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    public void fetchProfileById(String deviceId, OnDocumentLoadedListener listener) {
        db.collection(COLLECTION_USERS).document(deviceId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    public void softDeleteEvent(String eventId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection(COLLECTION_EVENTS).document(eventId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void restoreEvent(String eventId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", false);
        updates.put("removedBy", FieldValue.delete());
        updates.put("removedAt", FieldValue.delete());
        db.collection(COLLECTION_EVENTS).document(eventId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void softDeleteProfile(String deviceId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection(COLLECTION_USERS).document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void restoreProfile(String deviceId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", false);
        updates.put("removedBy", FieldValue.delete());
        updates.put("removedAt", FieldValue.delete());
        db.collection(COLLECTION_USERS).document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public FirebaseFirestore getDb() { return db; }
}
