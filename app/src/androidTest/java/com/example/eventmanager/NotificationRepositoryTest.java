package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/**
 * Instrumented integration tests for NotificationRepository.
 * These tests run against your real Firestore database using a
 * dedicated test device ID so they never touch real user data.
 *
 * Covers US 01.04.01: Entrant receives notification when selected from waiting list.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationRepositoryTest {

    // Isolated test device ID — never clashes with real user data
    private static final String TEST_DEVICE_ID = "junit_test_device_001";

    private NotificationRepository repository;
    private FirebaseFirestore db;

    @Before
    public void setUp() {
        repository = new NotificationRepository();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Cleans up all test notification documents after each test runs.
     * Ensures tests are independent and don't affect each other.
     */
    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshots = Tasks.await(
                db.collection("users")
                        .document(TEST_DEVICE_ID)
                        .collection("notifications")
                        .get()
        );

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            Tasks.await(doc.getReference().delete());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 1: Writing a notification
    // -------------------------------------------------------------------------

    /**
     * Verifies that addNotification() correctly writes a document to Firestore
     * with all fields populated and isRead defaulting to false.
     *
     * Simulates the moment a winner is selected from the waiting list.
     */
    @Test
    public void testAddNotification_writesCorrectDataToFirestore()
            throws ExecutionException, InterruptedException {

        // Arrange
        Notification notification = new Notification(
                "You've been selected!",
                "You were chosen to attend: Tech Meetup 2026",
                "Tech Meetup 2026"
        );

        // Act
        repository.addNotification(TEST_DEVICE_ID, notification);

        // Small wait for Firestore write to complete
        Thread.sleep(2000);

        // Assert — read the document back directly from Firestore
        QuerySnapshot result = Tasks.await(
                db.collection("users")
                        .document(TEST_DEVICE_ID)
                        .collection("notifications")
                        .get()
        );

        // Exactly one document should exist
        assertFalse("Notification was not written to Firestore", result.isEmpty());
        assertEquals("Expected exactly one notification", 1, result.size());

        // Verify field values
        DocumentSnapshot doc = result.getDocuments().get(0);
        assertEquals("You've been selected!", doc.getString("title"));
        assertEquals("Tech Meetup 2026", doc.getString("eventName"));
        assertFalse("Notification should default to unread",
                Boolean.TRUE.equals(doc.getBoolean("isRead")));

        // notificationId should have been set back onto the document
        assertNotNull("notificationId should not be null", doc.getString("notificationId"));
    }
}