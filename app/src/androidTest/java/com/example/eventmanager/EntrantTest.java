package com.example.eventmanager;

import org.junit.Test;
import static org.junit.Assert.*;
import com.example.eventmanager.models.Entrant;

/**
 * Unit tests for the Entrant model class.
 */
public class EntrantTest {

    @Test
    public void testEntrantCreation() {
        // Arrange
        String mockDeviceId = "device123";

        // Act
        Entrant entrant = new Entrant(mockDeviceId);

        // Assert
        assertEquals("device123", entrant.getDeviceId());
        assertFalse("New entrants should not be admins by default", entrant.isAdmin());
    }

    @Test
    public void testSetAndGetDetails() {
        // Arrange
        Entrant entrant = new Entrant("device456");

        // Act
        entrant.setName("John Doe");
        entrant.setEmail("john@example.com");

        // Assert
        assertEquals("John Doe", entrant.getName());
        assertEquals("john@example.com", entrant.getEmail());
    }

    @Test
    public void testRoleManagement() {
        Entrant entrant = new Entrant("device_xyz");

        // By default, a new user should just be a standard entrant, not an admin or organizer
        assertFalse(entrant.isAdmin());
        assertFalse(entrant.isOrganizer());

        // Grant roles
        entrant.setAdmin(true);
        entrant.setOrganizer(true);

        assertTrue(entrant.isAdmin());
        assertTrue(entrant.isOrganizer());

        // Revoke admin role
        entrant.setAdmin(false);
        assertFalse(entrant.isAdmin());
        assertTrue("Organizer status should remain true", entrant.isOrganizer());
    }

    @Test
    public void testEmptyConstructorForFirebase() {
        Entrant entrant = new Entrant();
        assertNull(entrant.getDeviceId());
        assertNull(entrant.getName());
    }
}