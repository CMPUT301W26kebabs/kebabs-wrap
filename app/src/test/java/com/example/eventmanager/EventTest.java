package com.example.eventmanager;

import org.junit.Test;
import static org.junit.Assert.*;

public class EventTest {

    @Test
    public void testEventGettersAndSetters() {
        // Create a blank event
        Event testEvent = new Event();

        // Give it some test data
        testEvent.setName("Summer Tech BBQ");
        testEvent.setCapacity(150);
        testEvent.setEventId("event-id-999");

        // Verify the data was saved correctly
        assertEquals("Summer Tech BBQ", testEvent.getName());
        assertEquals(150, testEvent.getCapacity());
        assertEquals("event-id-999", testEvent.getEventId());
    }
}