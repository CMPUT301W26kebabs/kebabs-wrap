package com.example.eventmanager.repository;

import com.example.eventmanager.callbacks.EntrantListCallback;
import com.example.eventmanager.callbacks.WaitlistCallback;
import com.example.eventmanager.repository.FirebaseRepository;

import androidx.annotation.NonNull;

import android.util.Log;
import com.example.eventmanager.callbacks.WaitlistCallback;
import com.example.eventmanager.callbacks.EntrantListCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FieldValue;


import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.example.eventmanager.models.RegistrationHistoryItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central repository for reading and writing entrant and event data in Firestore.
 *
 * Serves as the data layer for entrant profile management, event CRUD,
 * registration history, admin operations (soft delete/restore), and waitlist
 * enrollment. Also propagates profile updates into event sub-collections to
 * keep organizer-facing lists in sync.
 *
 * Accessed as a singleton via {@link #getInstance()}.
 */
public class FirebaseRepository {

    private static final String[] EVENT_SUB_COLLECTIONS =
            {"waitingList", "selected", "enrolled", "cancelled", "inviteeList"};

    private final FirebaseFirestore db;

    public FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Generic async callback used throughout this repository.
     *
     * @param <T> the type of value delivered on success
     */
    public interface RepoCallback<T> {
        void onSuccess(T result);
        void onError(@NonNull Exception e);
    }

    /**
     * Retrieves the entrant profile for a device identifier.
     *
     * @param deviceId unique device identifier used as the user document id
     * @param callback callback that receives the entrant or {@code null} when no profile exists
     */
    public void getUser(String deviceId, RepoCallback<Entrant> callback) {
        db.collection("users")
                .document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onSuccess(snapshot.toObject(Entrant.class));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Saves or updates an entrant profile in Firestore using merge so existing
     * fields (e.g. isAdmin, isOrganizer) are not accidentally overwritten.
     *
     * @param entrant  entrant profile to persist
     * @param callback callback notified when write completes or fails
     */
    public void saveUser(Entrant entrant, RepoCallback<Void> callback) {
        db.collection("users")
                .document(entrant.getDeviceId())
                .set(entrant, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Deletes the user profile AND removes the user from every event sub-collection
     * (waitingList, selected, enrolled, cancelled, inviteeList) across all events.
     * This satisfies the requirement that deleting a profile also erases all
     * registrations, registration history, and enrollments.
     *
     * @param deviceId device identifier of the user to delete
     * @param callback callback notified when all deletes complete or any fails
     */
    public void deleteUserAndAllRegistrations(String deviceId, RepoCallback<Void> callback) {
        // First load all events so we know which sub-collections to clean
        db.collection("events").get()
                .addOnSuccessListener(eventsSnapshot -> {
                    List<Task<Void>> deleteTasks = new ArrayList<>();

                    // Delete user document
                    deleteTasks.add(db.collection("users").document(deviceId).delete());

                    if (eventsSnapshot != null) {
                        for (DocumentSnapshot eventDoc : eventsSnapshot.getDocuments()) {
                            String eventId = eventDoc.getId();

                            // Delete the entire event if the user being deleted is its organizer
                            String organizerId = eventDoc.getString("organizerId");
                            if (organizerId != null && organizerId.equals(deviceId)) {
                                deleteTasks.add(db.collection("events").document(eventId).delete());
                            }

                            for (String subCol : EVENT_SUB_COLLECTIONS) {
                                // Delete the user's entry from each sub-collection of each event
                                deleteTasks.add(
                                        db.collection("events")
                                                .document(eventId)
                                                .collection(subCol)
                                                .document(deviceId)
                                                .delete()
                                );
                            }
                        }
                    }

                    Tasks.whenAll(deleteTasks)
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Loads an event document by its identifier.
     *
     * @param eventId  event document id
     * @param callback callback that receives the event or {@code null} when not found
     */
    public void getEventById(String eventId, RepoCallback<Event> callback) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onSuccess(snapshot.toObject(Event.class));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Adds an entrant to an event waiting list sub-collection.
     *
     * @param eventId  event document id
     * @param entrant  entrant being added to the waiting list
     * @param callback callback notified when the write completes or fails
     */
    public void signUpForEvent(String eventId, Entrant entrant, RepoCallback<Void> callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(entrant.getDeviceId())
                .set(entrant)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Loads all events this entrant appears in across the waitingList, selected,
     * enrolled, cancelled, and inviteeList sub-collections (US 01.02.03).
     *
     * Instead of collection-group queries (which require full document paths),
     * this fetches all events and checks each sub-collection directly by deviceId.
     *
     * @param deviceId device identifier of the entrant
     * @param callback callback that receives the ordered history list
     */
    public void getRegistrationHistoryForEntrant(String deviceId,
                                                 RepoCallback<List<RegistrationHistoryItem>> callback) {

        db.collection("events").get()
                .addOnSuccessListener(eventsSnapshot -> {
                    if (eventsSnapshot == null || eventsSnapshot.isEmpty()) {
                        callback.onSuccess(Collections.emptyList());
                        return;
                    }

                    List<String> eventIds = new ArrayList<>();
                    for (DocumentSnapshot doc : eventsSnapshot.getDocuments()) {
                        eventIds.add(doc.getId());
                    }

                    // For each event, fire 5 document lookups (one per sub-collection)
                    // Tasks are stored in order: [wait0, sel0, enr0, can0, inv0, wait1, ...]
                    List<Task<DocumentSnapshot>> allTasks = new ArrayList<>();
                    for (String eventId : eventIds) {
                        DocumentReference eventRef =
                                db.collection("events").document(eventId);
                        for (String subCol : EVENT_SUB_COLLECTIONS) {
                            allTasks.add(eventRef.collection(subCol).document(deviceId).get());
                        }
                    }

                    Tasks.whenAllComplete(allTasks).addOnCompleteListener(done -> {
                        Map<String, RegistrationHistoryItem.RegistrationStatus> statusByEvent =
                                new LinkedHashMap<>();

                        int subCount = EVENT_SUB_COLLECTIONS.length;
                        for (int i = 0; i < eventIds.size(); i++) {
                            String eventId = eventIds.get(i);
                            int base = i * subCount;

                            // 0=waitingList, 1=selected, 2=enrolled, 3=cancelled, 4=inviteeList
                            boolean inWait = docExists(allTasks.get(base));
                            boolean inSel  = docExists(allTasks.get(base + 1));
                            boolean inEnr  = docExists(allTasks.get(base + 2));
                            boolean inCan  = docExists(allTasks.get(base + 3));
                            boolean inInv  = docExists(allTasks.get(base + 4));

                            // Priority: enrolled > selected > invitee > cancelled > waitingList
                            if (inEnr) {
                                statusByEvent.put(eventId,
                                        RegistrationHistoryItem.RegistrationStatus.ENROLLED);
                            } else if (inSel) {
                                statusByEvent.put(eventId,
                                        RegistrationHistoryItem.RegistrationStatus.SELECTED);
                            } else if (inInv) {
                                statusByEvent.put(eventId,
                                        RegistrationHistoryItem.RegistrationStatus.INVITED);
                            } else if (inCan) {
                                statusByEvent.put(eventId,
                                        RegistrationHistoryItem.RegistrationStatus.CANCELLED);
                            } else if (inWait) {
                                statusByEvent.put(eventId,
                                        RegistrationHistoryItem.RegistrationStatus.WAITING_LIST);
                            }
                        }

                        if (statusByEvent.isEmpty()) {
                            callback.onSuccess(Collections.emptyList());
                            return;
                        }

                        // Fetch full event documents for matched events only
                        List<String> matchedIds = new ArrayList<>(statusByEvent.keySet());
                        List<Task<DocumentSnapshot>> fetchTasks = new ArrayList<>();
                        for (String eid : matchedIds) {
                            fetchTasks.add(db.collection("events").document(eid).get());
                        }

                        Tasks.whenAllComplete(fetchTasks).addOnCompleteListener(fetchDone -> {
                            List<RegistrationHistoryItem> items = new ArrayList<>();
                            for (int i = 0; i < matchedIds.size(); i++) {
                                Task<DocumentSnapshot> ft = fetchTasks.get(i);
                                if (!ft.isSuccessful()) continue;
                                DocumentSnapshot snap = ft.getResult();
                                if (snap == null || !snap.exists()) continue;
                                Event ev = snap.toObject(Event.class);
                                if (ev == null) continue;
                                String eid = matchedIds.get(i);
                                ev.setEventId(eid);
                                items.add(new RegistrationHistoryItem(
                                        ev, statusByEvent.get(eid)));
                            }
                            sortHistoryByDate(items);
                            callback.onSuccess(items);
                        });
                    });
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Returns true if the task completed successfully and the document exists.
     */
    private static boolean docExists(Task<DocumentSnapshot> task) {
        if (!task.isSuccessful()) return false;
        DocumentSnapshot snap = task.getResult();
        return snap != null && snap.exists();
    }

    private static void sortHistoryByDate(List<RegistrationHistoryItem> items) {
        Collections.sort(items, new Comparator<RegistrationHistoryItem>() {
            @Override
            public int compare(RegistrationHistoryItem a, RegistrationHistoryItem b) {
                Date da = a.getEvent().getRegistrationStart();
                Date db = b.getEvent().getRegistrationStart();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            }
        });
    }

    /**
     * After a profile update, propagates the entrant's latest name, email,
     * and phoneNumber into every event sub-collection (waitingList, selected,
     * enrolled, cancelled) where the entrant has a document.
     *
     * This ensures organizers see up-to-date entrant info in their lists.
     *
     * @param entrant  the updated entrant profile
     * @param callback optional — notified when sync completes or fails
     */
    public void syncEntrantAcrossEvents(Entrant entrant, RepoCallback<Void> callback) {
        String deviceId = entrant.getDeviceId();

        // Build the map of fields to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", entrant.getName());
        updates.put("email", entrant.getEmail());
        if (entrant.getPhoneNumber() != null) {
            updates.put("phoneNumber", entrant.getPhoneNumber());
        }

        db.collection("events").get()
                .addOnSuccessListener(eventsSnapshot -> {
                    if (eventsSnapshot == null || eventsSnapshot.isEmpty()) {
                        if (callback != null) callback.onSuccess(null);
                        return;
                    }

                    List<Task<Void>> updateTasks = new ArrayList<>();

                    for (DocumentSnapshot eventDoc : eventsSnapshot.getDocuments()) {
                        String eventId = eventDoc.getId();
                        for (String subCol : EVENT_SUB_COLLECTIONS) {
                            DocumentReference ref = db.collection("events")
                                    .document(eventId)
                                    .collection(subCol)
                                    .document(deviceId);

                            // Use a get-then-update approach so we only write
                            // to documents that actually exist
                            updateTasks.add(
                                    ref.get().continueWithTask(task -> {
                                        if (task.isSuccessful()
                                                && task.getResult() != null
                                                && task.getResult().exists()) {
                                            return ref.update(updates);
                                        }
                                        return Tasks.forResult(null);
                                    })
                            );
                        }
                    }

                    Tasks.whenAll(updateTasks)
                            .addOnSuccessListener(unused -> {
                                if (callback != null) callback.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    private static FirebaseRepository instance;
    private static final String TAG = "FirebaseRepository";

    /**
     * Returns the singleton instance, creating it on first access.
     *
     * @return the shared {@link FirebaseRepository} instance
     */
    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }




    // ══════════════════════════════════════════════════════════════
    //  CALLBACK INTERFACES
    // ══════════════════════════════════════════════════════════════

    /**
     * Callback for queries that return multiple Firestore documents (e.g. admin list screens).
     */
    public interface OnDocumentsLoadedListener {
        void onLoaded(List<DocumentSnapshot> documents);
        void onError(Exception e);
    }

    /**
     * Callback for queries that return a single Firestore document.
     */
    public interface OnDocumentLoadedListener {
        void onLoaded(DocumentSnapshot document);
        void onError(Exception e);
    }

    /**
     * Callback for write/delete operations with no return value.
     */
    public interface OnOperationCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }

    // ══════════════════════════════════════════════════════════════
    //  USER MANAGEMENT (Anas)
    // ══════════════════════════════════════════════════════════════

    /**
     * Creates or merges a minimal user profile for the given device. Assigns a
     * hardcoded organizer role for a specific test device; all others default to
     * a regular user. Used during first-launch bootstrapping.
     *
     * @param deviceId unique device identifier used as the user document ID
     */
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

    /**
     * Writes a new event document to Firestore using the event's own ID.
     *
     * @param event   the event to persist
     * @param success listener invoked on successful write
     * @param failure listener invoked on error
     */
    public void createEvent(Event event, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("events").document(event.getEventId()).set(event)
                .addOnSuccessListener(success).addOnFailureListener(failure);
    }

    /**
     * Merge-updates an existing event document (organizer edit flow).
     * Only fields present in the object are overwritten; others are preserved.
     *
     * @param event   the event with updated fields
     * @param success listener invoked on successful write
     * @param failure listener invoked on error
     */
    public void updateEvent(Event event, OnSuccessListener<Void> success, OnFailureListener failure) {
        db.collection("events").document(event.getEventId()).set(event, SetOptions.merge())
                .addOnSuccessListener(success).addOnFailureListener(failure);
    }

    /**
     * Updates only the {@code posterUrl} field of an event document.
     *
     * @param eventId   Firestore event document ID
     * @param posterUrl new poster image URL (Cloud Storage or remote)
     * @param onSuccess listener invoked on successful update
     * @param onFailure listener invoked on error
     */
    public void updateEventPosterUrl(String eventId, String posterUrl,
                                     OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("events").document(eventId).update("posterUrl", posterUrl)
                .addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /**
     * Queries all events where {@code organizerId} matches the given value.
     *
     * @param organizerId device ID of the organizer
     * @param onSuccess   receives the query snapshot of matching events
     * @param onFailure   listener invoked on error
     */
    public void getEventsByOrganizer(String organizerId,
                                     OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("events").whereEqualTo("organizerId", organizerId).get()
                .addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    /**
     * Events the user can manage: primary organizer ({@code organizerId}) or co-organizer ({@code coOrganizers}).
     * Merges two queries and de-duplicates by document id.
     *
     * @param deviceId  device ID of the organizer or co-organizer
     * @param onSuccess receives the de-duplicated list of event document snapshots
     * @param onFailure listener invoked on error
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

    /**
     * Adds an entrant to the event's waiting list after verifying that the
     * maximum waitlist capacity (if set) has not been reached.
     *
     * @param eventId  Firestore event document ID
     * @param entrant  the entrant to add
     * @param callback notified on success or with a descriptive failure message
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

    /**
     * Fetches all entrants from the {@code enrolled} sub-collection of an event.
     *
     * @param eventId  Firestore event document ID
     * @param callback receives the list of enrolled {@link Entrant} objects
     */
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

    /**
     * Fetches all events that are neither soft-deleted nor private.
     * Used by the admin browse-events screen.
     *
     * @param listener receives the filtered list of active event snapshots
     */
    public void fetchAllActiveEvents(OnDocumentsLoadedListener listener) {
        db.collection("events").get().addOnSuccessListener(qs -> {
            List<DocumentSnapshot> active = new ArrayList<>();
            for (DocumentSnapshot doc : qs.getDocuments()) {
                Boolean isDeleted = doc.getBoolean("isDeleted");
                if (isDeleted != null && isDeleted) continue;
                if (Boolean.TRUE.equals(doc.getBoolean("privateEvent"))) continue;
                active.add(doc);
            }
            listener.onLoaded(active);
        }).addOnFailureListener(listener::onError);
    }

    /**
     * Fetches every event document regardless of status or visibility.
     *
     * @param listener receives the full list of event snapshots
     */
    public void fetchAllEvents(OnDocumentsLoadedListener listener) {
        db.collection("events").get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    /**
     * Fetches a single event document by its Firestore ID.
     *
     * @param eventId  Firestore event document ID
     * @param listener receives the document snapshot (may not exist)
     */
    public void fetchEventById(String eventId, OnDocumentLoadedListener listener) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    /**
     * Marks an event as soft-deleted by setting {@code isDeleted=true} and recording
     * the admin who performed the action along with a server timestamp.
     *
     * @param eventId  Firestore event document ID
     * @param adminId  device ID of the admin performing the deletion
     * @param listener notified on success or failure
     */
    public void softDeleteEvent(String eventId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection("events").document(eventId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    /**
     * Reverses a soft-delete on an event by clearing {@code isDeleted}, {@code removedBy},
     * and {@code removedAt} fields.
     *
     * @param eventId  Firestore event document ID
     * @param listener notified on success or failure
     */
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

    /**
     * Fetches all user profiles that are not soft-disabled.
     * Used by the admin manage-profiles screen.
     *
     * @param listener receives the filtered list of active user snapshots
     */
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

    /**
     * Fetches every user profile document regardless of disabled status.
     *
     * @param listener receives the full list of user snapshots
     */
    public void fetchAllProfiles(OnDocumentsLoadedListener listener) {
        db.collection("users").get()
                .addOnSuccessListener(qs -> listener.onLoaded(qs.getDocuments()))
                .addOnFailureListener(listener::onError);
    }

    /**
     * Fetches a single user profile document by device ID.
     *
     * @param deviceId device identifier used as the user document ID
     * @param listener receives the document snapshot (may not exist)
     */
    public void fetchProfileById(String deviceId, OnDocumentLoadedListener listener) {
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(listener::onLoaded)
                .addOnFailureListener(listener::onError);
    }

    /**
     * Marks a user profile as soft-disabled by setting {@code isDisabled=true} and
     * recording the admin who performed the action along with a server timestamp.
     *
     * @param deviceId device identifier of the profile to disable
     * @param adminId  device ID of the admin performing the action
     * @param listener notified on success or failure
     */
    public void softDeleteProfile(String deviceId, String adminId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", true);
        updates.put("removedBy", adminId);
        updates.put("removedAt", FieldValue.serverTimestamp());
        db.collection("users").document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    /**
     * Reverses a soft-disable on a user profile by clearing {@code isDisabled},
     * {@code removedBy}, and {@code removedAt} fields.
     *
     * @param deviceId device identifier of the profile to restore
     * @param listener notified on success or failure
     */
    public void restoreProfile(String deviceId, OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", false);
        updates.put("removedBy", FieldValue.delete());
        updates.put("removedAt", FieldValue.delete());
        db.collection("users").document(deviceId).update(updates)
                .addOnSuccessListener(v -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    /**
     * Promotes a user to admin by setting {@code isAdmin} to {@code true}.
     * This is a one-way operation; demoting is not supported from the app.
     *
     * @param deviceId device identifier of the user to promote
     * @param listener notified on success or failure
     */
    public void promoteToAdmin(String deviceId, OnOperationCompleteListener listener) {
        db.collection("users").document(deviceId)
                .update("isAdmin", true)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════════════════════════════

    /**
     * Exposes the underlying Firestore instance for callers that need direct access.
     *
     * @return the {@link FirebaseFirestore} instance used by this repository
     */
    public FirebaseFirestore getDb() { return db; }

    // ANAS METHODS



    /**
     * Callback that delivers a descriptive message string on success or failure.
     */
    public interface StatusCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    /**
     * Listener for real-time event document changes.
     */
    public interface EventListener {
        void onEvent(Event event);
    }

    /**
     * Attaches a real-time snapshot listener to a single event document. The listener
     * fires immediately with current data and again on every server-side change.
     *
     * @param eventId  Firestore event document ID
     * @param listener invoked with the deserialized {@link Event} on each update
     * @return a {@link ListenerRegistration} the caller must remove to stop listening
     */
    public ListenerRegistration listenToEvent(String eventId, EventListener listener) {
        return db.collection("events").document(eventId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    Event event = snapshot.toObject(Event.class);
                    if (event != null) listener.onEvent(event);
                });
    }

    /**
     * Adds a device to an event's waiting list with a server timestamp.
     * Unlike {@link #joinWaitingList(String, Entrant, WaitlistCallback)}, this
     * variant does not check waitlist capacity and returns a {@link Task}.
     *
     * @param eventId  Firestore event document ID
     * @param deviceId device identifier of the entrant to add
     * @return a {@link Task} that completes when the write finishes
     */
    public Task<Void> joinWaitingList(String eventId, String deviceId) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("timestamp", FieldValue.serverTimestamp());
        return db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId).set(data);
    }

    /**
     * Returns a {@link Task} that resolves to the event document snapshot.
     *
     * @param eventId Firestore event document ID
     * @return a {@link Task} containing the document snapshot
     */
    public Task<DocumentSnapshot> getEvent(String eventId) {
        return db.collection("events").document(eventId).get();
    }

    /**
     * Accepts a lottery invitation by writing the user to {@code enrolled} and
     * deleting them from {@code selected} in sequence.
     *
     * @param eventId  Firestore event document ID
     * @param deviceId device identifier of the accepting entrant
     * @return a {@link Task} that completes when both writes finish
     */
    public Task<Void> acceptInvitation(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("acceptedAt", FieldValue.serverTimestamp());
        return eventRef.collection("enrolled").document(deviceId).set(data)
                .continueWithTask(task -> eventRef.collection("selected").document(deviceId).delete());
    }

    /**
     * Declines a lottery invitation by writing the user to {@code cancelled},
     * then removing them from both {@code selected} and {@code inviteeList}.
     *
     * @param eventId  Firestore event document ID
     * @param deviceId device identifier of the declining entrant
     * @return a {@link Task} that completes when all writes finish
     */
    public Task<Void> declineInvitation(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("declinedAt", FieldValue.serverTimestamp());
        return eventRef.collection("cancelled").document(deviceId).set(data)
                .continueWithTask(task -> eventRef.collection("selected").document(deviceId).delete())
                .continueWithTask(task -> eventRef.collection("inviteeList").document(deviceId).delete());
    }

    /**
     * Loads the waiting list for an event and reports the count via a status message.
     *
     * @param eventId  Firestore event document ID
     * @param callback receives a success message with the entrant count, or an error
     */
    public void getWaitingList(String eventId, StatusCallback callback) {
        db.collection("events").document(eventId).collection("waitingList").get()
                .addOnSuccessListener(qs -> callback.onSuccess("Loaded " + qs.size() + " entrants"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Loads the selected (chosen) entrants for an event and reports the count via a status message.
     *
     * @param eventId  Firestore event document ID
     * @param callback receives a success message with the chosen count, or an error
     */
    public void getChosenEntrants(String eventId, StatusCallback callback) {
        db.collection("events").document(eventId).collection("selected").get()
                .addOnSuccessListener(qs -> callback.onSuccess("Loaded " + qs.size() + " chosen"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

}