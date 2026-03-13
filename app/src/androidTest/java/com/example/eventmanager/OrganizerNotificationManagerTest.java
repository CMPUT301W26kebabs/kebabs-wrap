package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OrganizerNotificationManagerTest {

    private static final String TEST_EVENT_ID   = "junit_test_event_001";
    private static final String TEST_EVENT_NAME = "Test Event";
    private static final String TEST_DEVICE_ID  = "junit_test_device_001";
    private static final int    TIMEOUT_SEC     = 10;

    private OrganizerNotificationManager manager;
    private FirebaseFirestore db;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        manager = new OrganizerNotificationManager();
        db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", TEST_DEVICE_ID);

        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("winnersList").document(TEST_DEVICE_ID).set(data));
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("waitingList").document(TEST_DEVICE_ID).set(data));
        Tasks.await(db.collection("users").document(TEST_DEVICE_ID).set(data));
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("winnersList").document(TEST_DEVICE_ID).delete());
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("waitingList").document(TEST_DEVICE_ID).delete());

        QuerySnapshot notifications = Tasks.await(
                db.collection("users").document(TEST_DEVICE_ID)
                        .collection("notifications").get()
        );
        for (DocumentSnapshot doc : notifications.getDocuments()) {
            Tasks.await(doc.getReference().delete());
        }
        Tasks.await(db.collection("users").document(TEST_DEVICE_ID).delete());
    }

    // ── US 02.05.01 ───────────────────────────────────────────────────────────

    @Test
    public void testNotifyWinners_writesNotificationToWinner()
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] hasNotification = {false};

        manager.notifyWinners(TEST_EVENT_ID, TEST_EVENT_NAME);

        // Give Firestore time to complete the write, then query
        db.collection("users").document(TEST_DEVICE_ID)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        hasNotification[0] = true;
                        latch.countDown();
                    }
                });

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse("Winner should have received a notification", !hasNotification[0]);
    }

    @Test
    public void testNotifyWinners_notificationContainsEventId()
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        final String[] eventId = {null};

        manager.notifyWinners(TEST_EVENT_ID, TEST_EVENT_NAME);

        db.collection("users").document(TEST_DEVICE_ID)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        eventId[0] = snapshots.getDocuments()
                                .get(0).getString("eventId");
                        latch.countDown();
                    }
                });

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertNotNull("Winner notification must contain eventId", eventId[0]);
        assertEquals("eventId should match test event", TEST_EVENT_ID, eventId[0]);
    }

    // ── US 02.07.01 ───────────────────────────────────────────────────────────

    @Test
    public void testNotifyWaitingList_writesNotificationToEntrant()
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] hasNotification = {false};

        manager.notifyWaitingList(TEST_EVENT_ID, TEST_EVENT_NAME);

        db.collection("users").document(TEST_DEVICE_ID)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        hasNotification[0] = true;
                        latch.countDown();
                    }
                });

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse("Waitlist entrant should have received a notification",
                !hasNotification[0]);
    }

    @Test
    public void testNotifyWaitingList_notificationHasNoEventId()
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        final String[] eventId = {"placeholder"};

        manager.notifyWaitingList(TEST_EVENT_ID, TEST_EVENT_NAME);

        db.collection("users").document(TEST_DEVICE_ID)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        eventId[0] = snapshots.getDocuments()
                                .get(0).getString("eventId");
                        latch.countDown();
                    }
                });

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertEquals("Waitlist notification should have no eventId", null, eventId[0]);
    }
}