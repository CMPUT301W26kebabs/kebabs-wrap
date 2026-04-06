package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.eventmanager.models.NotificationLog;
import com.google.firebase.Timestamp;

import org.junit.Test;

/**
 * Unit tests for {@link com.example.eventmanager.models.NotificationLog}.
 */
public class NotificationLogTest {

    @Test
    public void emptyConstructor_forFirestore() {
        NotificationLog log = new NotificationLog();
        assertNull(log.getLogId());
        assertEquals(0, log.getRecipientCount());
    }

    @Test
    public void fullConstructor_populatesFieldsAndTimestamp() {
        NotificationLog log = new NotificationLog(
                "org1", "Organizer",
                "ev1", "Gala",
                "All Waiting List", 42,
                "Reminder", "Please check in");

        assertEquals("org1", log.getOrganizerId());
        assertEquals("Organizer", log.getOrganizerName());
        assertEquals("ev1", log.getEventId());
        assertEquals("Gala", log.getEventName());
        assertEquals("All Waiting List", log.getRecipientGroup());
        assertEquals(42, log.getRecipientCount());
        assertEquals("Reminder", log.getTitle());
        assertEquals("Please check in", log.getMessage());
        assertNotNull(log.getTimestamp());
    }

    @Test
    public void setters_roundTrip() {
        NotificationLog log = new NotificationLog();
        Timestamp ts = Timestamp.now();
        log.setLogId("L1");
        log.setOrganizerId("o");
        log.setOrganizerName("on");
        log.setEventId("e");
        log.setEventName("en");
        log.setRecipientGroup("g");
        log.setRecipientCount(3);
        log.setTitle("t");
        log.setMessage("m");
        log.setTimestamp(ts);

        assertEquals("L1", log.getLogId());
        assertEquals("o", log.getOrganizerId());
        assertEquals(3, log.getRecipientCount());
        assertEquals(ts, log.getTimestamp());
    }
}
