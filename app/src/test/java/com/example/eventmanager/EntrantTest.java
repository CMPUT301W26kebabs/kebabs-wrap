package com.example.eventmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.eventmanager.models.Entrant;

import org.junit.Test;

/**
 * Unit tests for {@link com.example.eventmanager.models.Entrant}.
 */
public class EntrantTest {

    @Test
    public void constructor_withDeviceId_setsDefaultsCorrectly() {
        Entrant entrant = new Entrant("device123");

        assertEquals("device123", entrant.getDeviceId());
        assertFalse(entrant.isAdmin());
        assertFalse(entrant.isOrganizer());
    }

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

    @Test
    public void settersAndGetters_storeValuesCorrectly() {
        Entrant entrant = new Entrant();

        entrant.setDeviceId("abc123");
        entrant.setName("Fahad");
        entrant.setEmail("fahad@example.com");
        entrant.setPhoneNumber("1234567890");
        entrant.setPhotoUrl("https://example.com/p.jpg");
        entrant.setAdmin(true);
        entrant.setOrganizer(true);

        assertEquals("abc123", entrant.getDeviceId());
        assertEquals("Fahad", entrant.getName());
        assertEquals("fahad@example.com", entrant.getEmail());
        assertEquals("1234567890", entrant.getPhoneNumber());
        assertEquals("https://example.com/p.jpg", entrant.getPhotoUrl());
        assertTrue(entrant.isAdmin());
        assertTrue(entrant.isOrganizer());
    }

    @Test
    public void roleManagement_adminCanBeRevokedOrganizerUnchanged() {
        Entrant entrant = new Entrant("device_xyz");

        assertFalse(entrant.isAdmin());
        assertFalse(entrant.isOrganizer());

        entrant.setAdmin(true);
        entrant.setOrganizer(true);
        assertTrue(entrant.isAdmin());
        assertTrue(entrant.isOrganizer());

        entrant.setAdmin(false);
        assertFalse(entrant.isAdmin());
        assertTrue(entrant.isOrganizer());
    }

    @Test
    public void receiveNotifications_defaultsToTrueWhenUnset() {
        Entrant entrant = new Entrant();
        assertTrue(entrant.isReceiveNotifications());
    }

    @Test
    public void receiveNotifications_optOut() {
        Entrant entrant = new Entrant();
        entrant.setReceiveNotifications(false);
        assertFalse(entrant.isReceiveNotifications());
    }

    @Test
    public void profileDisabled_onlyWhenExplicitlyTrue() {
        Entrant entrant = new Entrant();
        assertNull(entrant.getIsDisabled());
        assertFalse(entrant.isProfileDisabled());

        entrant.setIsDisabled(false);
        assertFalse(entrant.isProfileDisabled());

        entrant.setIsDisabled(true);
        assertTrue(entrant.isProfileDisabled());
    }

    @Test
    public void manageEventConstructor_setsStatusTabLabel() {
        Entrant entrant = new Entrant("d1", "Ann", "ann@x.com", "Waiting");
        assertEquals("d1", entrant.getDeviceId());
        assertEquals("Ann", entrant.getName());
        assertEquals("ann@x.com", entrant.getEmail());
        assertEquals("Waiting", entrant.getStatusTabLabel());
    }

    @Test
    public void sectionHeaderFlag_roundTrip() {
        Entrant entrant = new Entrant();
        assertFalse(entrant.isSectionHeader());
        entrant.setSectionHeader(true);
        assertTrue(entrant.isSectionHeader());
    }
}
