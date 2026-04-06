package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.eventmanager.models.Event;
import com.example.eventmanager.models.RegistrationHistoryItem;

import org.junit.Test;

/**
 * Unit tests for {@link com.example.eventmanager.models.RegistrationHistoryItem}.
 */
public class RegistrationHistoryItemTest {

    @Test
    public void getEventDisplayName_prefersNonBlankName() {
        Event event = new Event();
        event.setEventId("doc123");
        event.setName("  Workshop  ");
        RegistrationHistoryItem item = new RegistrationHistoryItem(
                event, RegistrationHistoryItem.RegistrationStatus.WAITING_LIST);
        assertEquals("  Workshop  ", item.getEventDisplayName());
    }

    @Test
    public void getEventDisplayName_fallsBackToEventIdWhenNameBlank() {
        Event event = new Event();
        event.setEventId("doc456");
        event.setName("   ");
        RegistrationHistoryItem item = new RegistrationHistoryItem(
                event, RegistrationHistoryItem.RegistrationStatus.ENROLLED);
        assertEquals("doc456", item.getEventDisplayName());
    }

    @Test
    public void getEventDisplayName_fallsBackToLiteralEventWhenNoId() {
        Event event = new Event();
        RegistrationHistoryItem item = new RegistrationHistoryItem(
                event, RegistrationHistoryItem.RegistrationStatus.CANCELLED);
        assertEquals("Event", item.getEventDisplayName());
    }

    @Test
    public void getLocationDisplay_trimsAndNullWhenBlank() {
        Event event = new Event();
        event.setLocation("  Hall A  ");
        assertEquals("  Hall A  ", new RegistrationHistoryItem(event,
                RegistrationHistoryItem.RegistrationStatus.SELECTED).getLocationDisplay());

        event.setLocation("   ");
        assertNull(new RegistrationHistoryItem(event,
                RegistrationHistoryItem.RegistrationStatus.SELECTED).getLocationDisplay());
    }

    @Test
    public void status_roundTrip() {
        Event event = new Event();
        event.setName("E");
        for (RegistrationHistoryItem.RegistrationStatus s : RegistrationHistoryItem.RegistrationStatus.values()) {
            assertEquals(s, new RegistrationHistoryItem(event, s).getStatus());
        }
    }
}
