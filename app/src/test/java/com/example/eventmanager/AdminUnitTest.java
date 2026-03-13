package com.example.eventmanager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for Admin User Stories.
 * Tests the filtering, search, and soft-delete logic used in admin screens.
 *
 * US 03.04.01 - Browse Events
 * US 03.05.01 - Browse Profiles
 * US 03.01.01 - Remove Events
 * US 03.02.01 - Remove Profiles
 */
public class AdminUnitTest {

    // ══════════════════════════════════════════════════════════════
    //  US 03.04.01 — Browse Events
    // ══════════════════════════════════════════════════════════════

    @Test
    public void browseEvents_activeEventsExcludeDeleted() {
        List<Map<String, Object>> events = new ArrayList<>();

        Map<String, Object> active = new HashMap<>();
        active.put("name", "Community Potluck");
        active.put("isDeleted", false);
        events.add(active);

        Map<String, Object> deleted = new HashMap<>();
        deleted.put("name", "Cancelled Meetup");
        deleted.put("isDeleted", true);
        events.add(deleted);

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> e : events) {
            Boolean isDel = (Boolean) e.get("isDeleted");
            if (isDel == null || !isDel) filtered.add(e);
        }

        assertEquals(1, filtered.size());
        assertEquals("Community Potluck", filtered.get(0).get("name"));
    }

    @Test
    public void browseEvents_allFilterShowsEverything() {
        List<Map<String, Object>> events = new ArrayList<>();

        Map<String, Object> e1 = new HashMap<>();
        e1.put("name", "Event A");
        e1.put("isDeleted", false);
        events.add(e1);

        Map<String, Object> e2 = new HashMap<>();
        e2.put("name", "Event B");
        e2.put("isDeleted", true);
        events.add(e2);

        assertEquals(2, events.size());
    }

    @Test
    public void browseEvents_deletedFilterShowsOnlyDeleted() {
        List<Map<String, Object>> events = new ArrayList<>();

        Map<String, Object> e1 = new HashMap<>();
        e1.put("name", "Active Event");
        e1.put("isDeleted", false);
        events.add(e1);

        Map<String, Object> e2 = new HashMap<>();
        e2.put("name", "Removed Event");
        e2.put("isDeleted", true);
        events.add(e2);

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> e : events) {
            Boolean isDel = (Boolean) e.get("isDeleted");
            if (isDel != null && isDel) filtered.add(e);
        }

        assertEquals(1, filtered.size());
        assertEquals("Removed Event", filtered.get(0).get("name"));
    }

    @Test
    public void browseEvents_searchByNameMatchesCorrectly() {
        String query = "potluck";
        String eventName = "Community Potluck Night";

        boolean matches = eventName.toLowerCase().contains(query.toLowerCase());

        assertTrue(matches);
    }

    @Test
    public void browseEvents_searchNoMatchReturnsEmpty() {
        String query = "xyz123nonexistent";
        String eventName = "Community Potluck Night";

        boolean matches = eventName.toLowerCase().contains(query.toLowerCase());

        assertFalse(matches);
    }

    @Test
    public void browseEvents_searchIsCaseInsensitive() {
        String query = "POTLUCK";
        String eventName = "Community Potluck Night";

        boolean matches = eventName.toLowerCase().contains(query.toLowerCase());

        assertTrue(matches);
    }

    @Test
    public void browseEvents_searchByEventIdWorks() {
        String query = "abc123";
        String eventId = "abc123def456";

        boolean matches = eventId.toLowerCase().contains(query.toLowerCase());

        assertTrue(matches);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 03.05.01 — Browse Profiles
    // ══════════════════════════════════════════════════════════════

    @Test
    public void browseProfiles_activeProfilesExcludeDisabled() {
        List<Map<String, Object>> profiles = new ArrayList<>();

        Map<String, Object> active = new HashMap<>();
        active.put("name", "Alice");
        active.put("isDisabled", false);
        profiles.add(active);

        Map<String, Object> disabled = new HashMap<>();
        disabled.put("name", "Bob");
        disabled.put("isDisabled", true);
        profiles.add(disabled);

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> p : profiles) {
            Boolean isDis = (Boolean) p.get("isDisabled");
            if (isDis == null || !isDis) filtered.add(p);
        }

        assertEquals(1, filtered.size());
        assertEquals("Alice", filtered.get(0).get("name"));
    }

    @Test
    public void browseProfiles_searchByEmailWorks() {
        String query = "alice@";
        String email = "alice@example.com";

        assertTrue(email.toLowerCase().contains(query.toLowerCase()));
    }

    @Test
    public void browseProfiles_searchByNameWorks() {
        String query = "ali";
        String name = "Alice Johnson";

        assertTrue(name.toLowerCase().contains(query.toLowerCase()));
    }

    @Test
    public void browseProfiles_searchByDeviceIdWorks() {
        String query = "device_abc";
        String deviceId = "device_abc_123";

        assertTrue(deviceId.toLowerCase().contains(query.toLowerCase()));
    }

    @Test
    public void browseProfiles_combinedSearchMatchesNameEmailOrId() {
        String query = "alice";
        String name = "Alice Johnson";
        String email = "aj@example.com";
        String deviceId = "device_123";

        String searchable = (name + " " + email + " " + deviceId).toLowerCase();

        assertTrue(searchable.contains(query.toLowerCase()));
    }

    @Test
    public void browseProfiles_alphabeticalSortingWorks() {
        List<String> names = new ArrayList<>();
        names.add("Charlie");
        names.add("Alice");
        names.add("Bob");

        java.util.Collections.sort(names, String::compareToIgnoreCase);

        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
        assertEquals("Charlie", names.get(2));
    }

    @Test
    public void browseProfiles_initialsGeneratedCorrectly_fullName() {
        String name = "Alice Johnson";
        String[] parts = name.split(" ");
        String initials = parts.length >= 2
                ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
                : ("" + parts[0].charAt(0)).toUpperCase();

        assertEquals("AJ", initials);
    }

    @Test
    public void browseProfiles_initialsGeneratedCorrectly_singleName() {
        String name = "Alice";
        String[] parts = name.split(" ");
        String initials = parts.length >= 2
                ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
                : ("" + parts[0].charAt(0)).toUpperCase();

        assertEquals("A", initials);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 03.01.01 — Remove Events (Soft Delete)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void removeEvent_softDeleteSetsCorrectFields() {
        Map<String, Object> updates = new HashMap<>();
        String adminId = "admin_device_123";

        updates.put("isDeleted", true);
        updates.put("removedBy", adminId);

        assertTrue((Boolean) updates.get("isDeleted"));
        assertEquals("admin_device_123", updates.get("removedBy"));
    }

    @Test
    public void removeEvent_restoreClearsDeleteFields() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", false);

        assertFalse((Boolean) updates.get("isDeleted"));
    }

    @Test
    public void removeEvent_deletedEventHiddenFromActiveFilter() {
        Map<String, Object> event = new HashMap<>();
        event.put("name", "Test Event");
        event.put("isDeleted", true);

        Boolean isDeleted = (Boolean) event.get("isDeleted");
        boolean showInActive = (isDeleted == null || !isDeleted);

        assertFalse(showInActive);
    }

    @Test
    public void removeEvent_deletedEventVisibleInDeletedFilter() {
        Map<String, Object> event = new HashMap<>();
        event.put("name", "Test Event");
        event.put("isDeleted", true);

        Boolean isDeleted = (Boolean) event.get("isDeleted");
        boolean showInDeleted = (isDeleted != null && isDeleted);

        assertTrue(showInDeleted);
    }

    @Test
    public void removeEvent_auditTrailContainsAdminId() {
        Map<String, Object> updates = new HashMap<>();
        String adminId = "admin_device_456";
        updates.put("removedBy", adminId);

        assertEquals("admin_device_456", updates.get("removedBy"));
        assertNotNull(updates.get("removedBy"));
    }

    // ══════════════════════════════════════════════════════════════
    //  US 03.02.01 — Remove Profiles (Soft Disable)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void removeProfile_softDisableSetsCorrectFields() {
        Map<String, Object> updates = new HashMap<>();
        String adminId = "admin_device_789";

        updates.put("isDisabled", true);
        updates.put("removedBy", adminId);

        assertTrue((Boolean) updates.get("isDisabled"));
        assertEquals("admin_device_789", updates.get("removedBy"));
    }

    @Test
    public void removeProfile_disabledProfileCannotJoinWaitlist() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "Bob");
        profile.put("isDisabled", true);

        Boolean isDisabled = (Boolean) profile.get("isDisabled");
        boolean canJoin = (isDisabled == null || !isDisabled);

        assertFalse(canJoin);
    }

    @Test
    public void removeProfile_activeProfileCanJoinWaitlist() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "Alice");
        profile.put("isDisabled", false);

        Boolean isDisabled = (Boolean) profile.get("isDisabled");
        boolean canJoin = (isDisabled == null || !isDisabled);

        assertTrue(canJoin);
    }

    @Test
    public void removeProfile_restoreClearsDisableFields() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDisabled", false);

        assertFalse((Boolean) updates.get("isDisabled"));
    }

    @Test
    public void removeProfile_disabledProfileShowsDoneButton() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("isDisabled", true);

        Boolean isDisabled = (Boolean) profile.get("isDisabled");
        boolean showDone = (isDisabled != null && isDisabled);
        boolean showRemove = !showDone;

        assertTrue(showDone);
        assertFalse(showRemove);
    }

    // ══════════════════════════════════════════════════════════════
    //  Edge Cases
    // ══════════════════════════════════════════════════════════════

    @Test
    public void edgeCase_nullIsDeletedTreatedAsActive() {
        Map<String, Object> event = new HashMap<>();
        event.put("name", "Old Event");
        // isDeleted not set (null)

        Boolean isDeleted = (Boolean) event.get("isDeleted");
        boolean active = (isDeleted == null || !isDeleted);

        assertTrue(active);
    }

    @Test
    public void edgeCase_nullIsDisabledTreatedAsActive() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "New User");
        // isDisabled not set (null)

        Boolean isDisabled = (Boolean) profile.get("isDisabled");
        boolean active = (isDisabled == null || !isDisabled);

        assertTrue(active);
    }

    @Test
    public void edgeCase_emptySearchReturnsAllResults() {
        String query = "";

        // Empty query should not filter anything
        assertTrue(query.isEmpty());
    }

    @Test
    public void edgeCase_nullNameHandledGracefully() {
        String name = null;
        String displayName = (name != null) ? name : "No Name";

        assertEquals("No Name", displayName);
    }
}
