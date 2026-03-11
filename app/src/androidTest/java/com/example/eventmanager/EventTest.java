package com.example.eventmanager;

import org.junit.Test;
import static org.junit.Assert.*;
import com.example.eventmanager.models.Event;
import java.util.Date;

/**
 * Unit tests for the Event model class.
 */
public class EventTest {

    @Test
    public void testEmptyConstructor() {
        // Firebase requires an empty constructor. We must ensure it doesn't crash or pre-fill bad data.
        Event event = new Event();
        assertNull("Event ID should be null upon empty creation", event.getEventId());
        assertEquals("Capacity should default to 0", 0, event.getCapacity());
        assertFalse("Geolocation should default to false", event.isGeolocationRequired());
    }

    @Test
    public void testSetAndGetCoreDetails() {
        Event event = new Event();

        event.setEventId("EVT_001");
        event.setName("Beginner Swimming");
        event.setDescription("Learn to swim!");
        event.setOrganizerId("ORG_999");

        assertEquals("EVT_001", event.getEventId());
        assertEquals("Beginner Swimming", event.getName());
        assertEquals("Learn to swim!", event.getDescription());
        assertEquals("ORG_999", event.getOrganizerId());
    }

    @Test
    public void testCapacityBoundaries() {
        Event event = new Event();

        // Test standard capacities
        event.setCapacity(20);
        event.setMaxWaitlistCapacity(50);

        assertEquals(20, event.getCapacity());
        assertEquals(50, event.getMaxWaitlistCapacity());

        // Test "unlimited" representation (often represented as 0 or -1 in logic)
        event.setMaxWaitlistCapacity(-1);
        assertEquals(-1, event.getMaxWaitlistCapacity());
    }

    @Test
    public void testDateAssignments() {
        Event event = new Event();
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 86400000); // +1 day in milliseconds

        event.setRegistrationStart(startDate);
        event.setRegistrationEnd(endDate);

        assertEquals(startDate, event.getRegistrationStart());
        assertEquals(endDate, event.getRegistrationEnd());
        assertTrue("End date should be after start date",
                event.getRegistrationEnd().after(event.getRegistrationStart()));
    }
}