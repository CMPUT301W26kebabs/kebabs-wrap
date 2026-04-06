package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.eventmanager.models.Event;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for {@link com.example.eventmanager.models.Event}.
 */
public class EventTest {

    @Test
    public void emptyConstructor_hasFirestoreFriendlyDefaults() {
        Event event = new Event();

        assertNull(event.getEventId());
        assertNull(event.getName());
        assertNull(event.getDescription());
        assertNull(event.getLocation());
        assertNull(event.getStartDate());
        assertNull(event.getEndDate());
        assertNull(event.getRegistrationStart());
        assertNull(event.getRegistrationEnd());
        assertNull(event.getPosterUrl());
        assertNull(event.getOrganizerId());
        assertEquals(0, event.getCapacity());
        assertEquals(0, event.getMaxWaitlistCapacity());
        assertFalse(event.isGeolocationRequired());
        assertFalse(event.isPrivateEvent());
        assertFalse(event.isDeleted());
    }

    @Test
    public void settersAndGetters_roundTrip() {
        Event event = new Event();
        Date regStart = new Date();
        Date regEnd = new Date(regStart.getTime() + 3600000);
        Date evStart = new Date(regEnd.getTime() + 3600000);
        Date evEnd = new Date(evStart.getTime() + 7200000);

        event.setEventId("event001");
        event.setName("Tech Meetup");
        event.setDescription("Networking");
        event.setLocation("Edmonton");
        event.setStartDate(evStart);
        event.setEndDate(evEnd);
        event.setRegistrationStart(regStart);
        event.setRegistrationEnd(regEnd);
        event.setCapacity(50);
        event.setMaxWaitlistCapacity(100);
        event.setPosterUrl("poster.jpg");
        event.setOrganizerId("organizer123");
        event.setGeolocationRequired(true);
        event.setPrivateEvent(true);
        event.setDeleted(true);

        assertEquals("event001", event.getEventId());
        assertEquals("Tech Meetup", event.getName());
        assertEquals("Networking", event.getDescription());
        assertEquals("Edmonton", event.getLocation());
        assertEquals(evStart, event.getStartDate());
        assertEquals(evEnd, event.getEndDate());
        assertEquals(regStart, event.getRegistrationStart());
        assertEquals(regEnd, event.getRegistrationEnd());
        assertEquals(50, event.getCapacity());
        assertEquals(100, event.getMaxWaitlistCapacity());
        assertEquals("poster.jpg", event.getPosterUrl());
        assertEquals("organizer123", event.getOrganizerId());
        assertTrue(event.isGeolocationRequired());
        assertTrue(event.isPrivateEvent());
        assertTrue(event.isDeleted());
    }

    @Test
    public void registrationEnd_afterRegistrationStart() {
        Event event = new Event();
        Date start = new Date();
        Date end = new Date(start.getTime() + 86400000);

        event.setRegistrationStart(start);
        event.setRegistrationEnd(end);

        assertTrue(event.getRegistrationEnd().after(event.getRegistrationStart()));
    }

    @Test
    public void maxWaitlistCapacity_acceptsUnlimitedSentinel() {
        Event event = new Event();
        event.setMaxWaitlistCapacity(-1);
        assertEquals(-1, event.getMaxWaitlistCapacity());
    }
}
