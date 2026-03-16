package com.example.eventmanager;

import static org.junit.Assert.assertFalse;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
public class AcceptDeclineTest {

    private static final String TEST_EVENT_ID  = "junit_test_event_001";
    private static final String TEST_DEVICE_ID = "junit_test_device_001";
    private static final int    TIMEOUT_SEC    = 10;

    private EventRepository eventRepository;
    private FirebaseFirestore db;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        eventRepository = new EventRepository();
        db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", TEST_DEVICE_ID);

        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("winnersList").document(TEST_DEVICE_ID).set(data));
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("waitingList").document(TEST_DEVICE_ID).set(data));
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("winnersList").document(TEST_DEVICE_ID).delete());
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("waitingList").document(TEST_DEVICE_ID).delete());
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("enrolled").document(TEST_DEVICE_ID).delete());
    }

    @Test
    public void testDecline_removesFromWinnersList() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] exists = {true};

        eventRepository.removeFromWinners(TEST_EVENT_ID, TEST_DEVICE_ID);

        // Wait then verify
        db.collection("events").document(TEST_EVENT_ID)
                .collection("winnersList").document(TEST_DEVICE_ID)
                .get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    latch.countDown();      // signal that we're done
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse("User should be removed from winnersList", exists[0]);
    }

    @Test
    public void testDecline_removesFromWaitingList() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] exists = {true};

        eventRepository.removeFromWaitingList(TEST_EVENT_ID, TEST_DEVICE_ID);

        db.collection("events").document(TEST_EVENT_ID)
                .collection("waitingList").document(TEST_DEVICE_ID)
                .get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse("User should be removed from waitingList", exists[0]);
    }

    @Test
    public void testDecline_doesNotEnrollUser() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] exists = {false};

        eventRepository.removeFromWinners(TEST_EVENT_ID, TEST_DEVICE_ID);
        eventRepository.removeFromWaitingList(TEST_EVENT_ID, TEST_DEVICE_ID);

        db.collection("events").document(TEST_EVENT_ID)
                .collection("enrolled").document(TEST_DEVICE_ID)
                .get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertFalse("Declining should never enroll the user", exists[0]);
    }
}