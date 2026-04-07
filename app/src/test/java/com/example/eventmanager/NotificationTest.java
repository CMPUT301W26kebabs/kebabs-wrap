package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.eventmanager.models.Notification;
import com.google.firebase.Timestamp;

import org.junit.Test;

/**
 * Unit tests for {@link com.example.eventmanager.models.Notification}.
 */
public class NotificationTest {

    @Test
    public void emptyConstructor_forFirestore() {
        Notification n = new Notification();
        assertNull(n.getNotificationId());
        assertNull(n.getTitle());
        assertFalse(n.isRead());
    }

    @Test
    public void informationalConstructor_leavesEventIdNull() {
        Notification n = new Notification("Hi", "Body", "Fest");
        assertEquals("Hi", n.getTitle());
        assertEquals("Body", n.getBody());
        assertEquals("Fest", n.getEventName());
        assertNull(n.getEventId());
        assertFalse(n.isRead());
        assertNotNull(n.getTimestamp());
    }

    @Test
    public void winnerConstructor_setsEventId() {
        Notification n = new Notification("You won", "Accept", "Fest", "evt_1");
        assertEquals("evt_1", n.getEventId());
        assertNotNull(n.getTimestamp());
    }

    @Test
    public void setters_roundTrip() {
        Notification n = new Notification();
        Timestamp ts = Timestamp.now();
        n.setNotificationId("n1");
        n.setTitle("t");
        n.setBody("b");
        n.setEventName("e");
        n.setEventId("eid");
        n.setRead(true);
        n.setTimestamp(ts);

        assertEquals("n1", n.getNotificationId());
        assertEquals("t", n.getTitle());
        assertEquals("b", n.getBody());
        assertEquals("e", n.getEventName());
        assertEquals("eid", n.getEventId());
        assertEquals(ts, n.getTimestamp());
        assertTrue(n.isRead());
    }
}
