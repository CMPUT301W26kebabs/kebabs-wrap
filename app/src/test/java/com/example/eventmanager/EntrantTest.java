package com.example.eventmanager.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Local unit tests for the Entrant model.
 */
public class EntrantTest {

    /**
     * Verifies that the constructor with a device ID stores the ID and
     * initializes role flags to false.
     */
    @Test
    public void constructor_withDeviceId_setsDefaultsCorrectly() {
        Entrant entrant = new Entrant("device123");

        assertEquals("device123", entrant.getDeviceId());
        assertFalse(entrant.isAdmin());
        assertFalse(entrant.isOrganizer());
    }

    /**
     * Verifies that the empty constructor leaves string fields unset and role flags false.
     */
    @Test
    public void emptyConstructor_leavesFieldsUnset() {
        Entrant entrant = new Entrant();

        assertNull(entrant.getDeviceId());
        assertNull(entrant.getName());
        assertNull(entrant.getEmail());
        assertNull(entrant.getPhoneNumber());
        assertFalse(entrant.isAdmin());
        assertFalse(entrant.isOrganizer());
    }

    /**
     * Verifies that all setters correctly store data and getters return it unchanged.
     */
    @Test
    public void settersAndGetters_storeValuesCorrectly() {
        Entrant entrant = new Entrant();

        entrant.setDeviceId("abc123");
        entrant.setName("Fahad");
        entrant.setEmail("fahad@example.com");
        entrant.setPhoneNumber("1234567890");
        entrant.setAdmin(true);
        entrant.setOrganizer(true);

        assertEquals("abc123", entrant.getDeviceId());
        assertEquals("Fahad", entrant.getName());
        assertEquals("fahad@example.com", entrant.getEmail());
        assertEquals("1234567890", entrant.getPhoneNumber());
        assertTrue(entrant.isAdmin());
        assertTrue(entrant.isOrganizer());
    }
}