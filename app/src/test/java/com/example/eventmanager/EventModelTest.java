package com.example.eventmanager.models;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Date;

/**
 * Additional unit tests for the Event model.
 */
public class EventModelTest {

    /**
     * Verifies default values of a new Event.
     */
    @Test
    public void newEvent_hasExpectedDefaultValues() {
        Event event = new Event();

        assertNull(event.getEventId());
        assertNull(event.getName());
        assertNull(event.getDescription());
        assertNull(event.getRegistrationStart());
        assertNull(event.getRegistrationEnd());
        assertNull(event.getPosterUrl());
        assertNull(event.getOrganizerId());
        assertEquals(0, event.getCapacity());
        assertEquals(0, event.getMaxWaitlistCapacity());
        assertFalse(event.isGeolocationRequired());
    }

    /**
     * Verifies setters and getters store values correctly.
     */
    @Test
    public void settersAndGetters_storeValuesCorrectly() {
        Event event = new Event();

        Date start = new Date();
        Date end = new Date(start.getTime() + 3600000);

        event.setEventId("event001");
        event.setName("Tech Meetup");
        event.setDescription("A networking event");
        event.setRegistrationStart(start);
        event.setRegistrationEnd(end);
        event.setCapacity(50);
        event.setMaxWaitlistCapacity(100);
        event.setPosterUrl("poster.jpg");
        event.setOrganizerId("organizer123");
        event.setGeolocationRequired(true);

        assertEquals("event001", event.getEventId());
        assertEquals("Tech Meetup", event.getName());
        assertEquals("A networking event", event.getDescription());
        assertEquals(start, event.getRegistrationStart());
        assertEquals(end, event.getRegistrationEnd());
        assertEquals(50, event.getCapacity());
        assertEquals(100, event.getMaxWaitlistCapacity());
        assertEquals("poster.jpg", event.getPosterUrl());
        assertEquals("organizer123", event.getOrganizerId());
        assertTrue(event.isGeolocationRequired());
    }
}