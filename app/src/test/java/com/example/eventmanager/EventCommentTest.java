package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.eventmanager.models.EventComment;
import com.google.firebase.Timestamp;

import org.junit.Test;

/**
 * Unit tests for {@link com.example.eventmanager.models.EventComment} (POJO and full constructor only;
 * {@link EventComment#fromDocument} needs a Firestore {@code DocumentSnapshot}).
 */
public class EventCommentTest {

    @Test
    public void emptyConstructor_forFirestore() {
        EventComment c = new EventComment();
        assertNull(c.getId());
        assertNull(c.getText());
    }

    @Test
    public void fullConstructor_roundTrip() {
        Timestamp ts = Timestamp.now();
        EventComment c = new EventComment(
                "c1", "dev", "Author", "Hello", ts, "ev1", "Party");

        assertEquals("c1", c.getId());
        assertEquals("dev", c.getDeviceId());
        assertEquals("Author", c.getAuthorName());
        assertEquals("Hello", c.getText());
        assertEquals(ts, c.getTimestamp());
        assertEquals("ev1", c.getEventId());
        assertEquals("Party", c.getEventName());
    }

    @Test
    public void setters_roundTrip() {
        EventComment c = new EventComment();
        Timestamp ts = Timestamp.now();
        c.setId("x");
        c.setDeviceId("d");
        c.setAuthorName("a");
        c.setText("t");
        c.setTimestamp(ts);
        c.setEventId("e");
        c.setEventName("n");

        assertEquals("x", c.getId());
        assertEquals("t", c.getText());
        assertEquals(ts, c.getTimestamp());
    }
}
