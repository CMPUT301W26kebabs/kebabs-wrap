package com.example.eventmanager.entrant;

import com.example.eventmanager.ui.EventDetailsActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Shared Firestore documents for {@link EventDetailsActivity} instrumented tests.
 */
final class EventDetailsTestFixtures {

    static final String OPEN_EVENT_ID = "test_event_open";
    static final String CLOSED_EVENT_ID = "test_event_closed";
    static final String MOCK_EVENT_ID = "mock_test_event_id";

    /** Organizer id that will not match any real device under test. */
    private static final String FAKE_ORGANIZER_ID = "junit_fixture_organizer";

    private EventDetailsTestFixtures() {}

    static void upsertEventForTests(String eventId, long regStartMs, long regEndMs)
            throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Fixture: " + eventId);
        data.put("description", "Instrumented test event.");
        data.put("registrationStart", new Timestamp(new Date(regStartMs)));
        data.put("registrationEnd", new Timestamp(new Date(regEndMs)));
        data.put("startDate", new Timestamp(new Date(now + 86400000L * 14)));
        data.put("location", "Test Venue");
        data.put("geolocationRequired", false);
        data.put("isGeolocationRequired", false);
        data.put("organizerId", FAKE_ORGANIZER_ID);
        data.put("capacity", 50L);
        data.put("maxWaitlistCapacity", 0L);
        Tasks.await(db.collection("events").document(eventId).set(data));
    }

    static void seedAllStandardFixtures() throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();
        long day = 86400000L;
        upsertEventForTests(OPEN_EVENT_ID, now - 2 * day, now + 7 * day);
        upsertEventForTests(MOCK_EVENT_ID, now - 2 * day, now + 7 * day);
        upsertEventForTests(CLOSED_EVENT_ID, now - 30 * day, now - day);
    }

    static void removeUserFromEventWaitingList(String eventId, String deviceId)
            throws ExecutionException, InterruptedException {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Tasks.await(db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId).delete());
    }
}
