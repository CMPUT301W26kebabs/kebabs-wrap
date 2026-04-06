package com.example.eventmanager.entrant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.models.Notification;
import com.example.eventmanager.repository.NotificationRepository;
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
 * Entrant — US 01.04.01 / 01.04.02 (notifications persisted for the user).
 */
@RunWith(AndroidJUnit4.class)
public class NotificationRepositoryTest {

    private static final String TEST_DEVICE_ID = "junit_test_device_001";

    private NotificationRepository repository;
    private FirebaseFirestore db;

    @Before
    public void setUp() {
        repository = new NotificationRepository();
        db = FirebaseFirestore.getInstance();
    }

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

    @Test
    public void testAddNotification_writesCorrectDataToFirestore()
            throws ExecutionException, InterruptedException {

        Notification notification = new Notification(
                "You've been selected!",
                "You were chosen to attend: Tech Meetup 2026",
                "Tech Meetup 2026"
        );

        repository.addNotification(TEST_DEVICE_ID, notification);

        QuerySnapshot result = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            Thread.sleep(200);
            result = Tasks.await(
                    db.collection("users")
                            .document(TEST_DEVICE_ID)
                            .collection("notifications")
                            .get()
            );
            if (!result.isEmpty()) {
                break;
            }
        }

        assertNotNull(result);
        assertFalse("Notification was not written to Firestore within timeout", result.isEmpty());
        assertEquals("Expected exactly one notification", 1, result.size());

        DocumentSnapshot doc = result.getDocuments().get(0);
        assertEquals("You've been selected!", doc.getString("title"));
        assertEquals("Tech Meetup 2026", doc.getString("eventName"));
        assertFalse("Notification should default to unread",
                Boolean.TRUE.equals(doc.getBoolean("isRead")));

        assertNotNull("notificationId should not be null", doc.getString("notificationId"));
    }
}
