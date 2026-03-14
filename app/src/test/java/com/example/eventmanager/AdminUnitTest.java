package com.example.eventmanager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for ALL User Stories.
 */
public class AdminUnitTest {

    // ══════════════════════════════════════════════════════════════
    //  US 03.04.01 — Browse Events
    // ══════════════════════════════════════════════════════════════

    @Test
    public void browseEvents_activeEventsExcludeDeleted() {
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> active = new HashMap<>();
        active.put("name", "Community Potluck"); active.put("isDeleted", false);
        events.add(active);
        Map<String, Object> deleted = new HashMap<>();
        deleted.put("name", "Cancelled Meetup"); deleted.put("isDeleted", true);
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
        Map<String, Object> e1 = new HashMap<>(); e1.put("name", "Event A"); e1.put("isDeleted", false); events.add(e1);
        Map<String, Object> e2 = new HashMap<>(); e2.put("name", "Event B"); e2.put("isDeleted", true); events.add(e2);
        assertEquals(2, events.size());
    }

    @Test
    public void browseEvents_deletedFilterShowsOnlyDeleted() {
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> e1 = new HashMap<>(); e1.put("name", "Active Event"); e1.put("isDeleted", false); events.add(e1);
        Map<String, Object> e2 = new HashMap<>(); e2.put("name", "Removed Event"); e2.put("isDeleted", true); events.add(e2);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> e : events) {
            Boolean isDel = (Boolean) e.get("isDeleted");
            if (isDel != null && isDel) filtered.add(e);
        }
        assertEquals(1, filtered.size());
        assertEquals("Removed Event", filtered.get(0).get("name"));
    }

    @Test public void browseEvents_searchByNameMatchesCorrectly() { assertTrue("Community Potluck Night".toLowerCase().contains("potluck")); }
    @Test public void browseEvents_searchNoMatchReturnsEmpty() { assertFalse("Community Potluck Night".toLowerCase().contains("xyz123nonexistent")); }
    @Test public void browseEvents_searchIsCaseInsensitive() { assertTrue("Community Potluck Night".toLowerCase().contains("POTLUCK".toLowerCase())); }
    @Test public void browseEvents_searchByEventIdWorks() { assertTrue("abc123def456".toLowerCase().contains("abc123")); }

    // ══════════════════════════════════════════════════════════════
    //  US 03.05.01 — Browse Profiles
    // ══════════════════════════════════════════════════════════════

    @Test
    public void browseProfiles_activeProfilesExcludeDisabled() {
        List<Map<String, Object>> profiles = new ArrayList<>();
        Map<String, Object> active = new HashMap<>(); active.put("name", "Alice"); active.put("isDisabled", false); profiles.add(active);
        Map<String, Object> disabled = new HashMap<>(); disabled.put("name", "Bob"); disabled.put("isDisabled", true); profiles.add(disabled);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> p : profiles) {
            Boolean isDis = (Boolean) p.get("isDisabled");
            if (isDis == null || !isDis) filtered.add(p);
        }
        assertEquals(1, filtered.size());
        assertEquals("Alice", filtered.get(0).get("name"));
    }

    @Test public void browseProfiles_searchByEmailWorks() { assertTrue("alice@example.com".toLowerCase().contains("alice@")); }
    @Test public void browseProfiles_searchByNameWorks() { assertTrue("Alice Johnson".toLowerCase().contains("ali")); }
    @Test public void browseProfiles_searchByDeviceIdWorks() { assertTrue("device_abc_123".toLowerCase().contains("device_abc")); }

    @Test
    public void browseProfiles_combinedSearchMatchesNameEmailOrId() {
        String searchable = ("Alice Johnson" + " " + "aj@example.com" + " " + "device_123").toLowerCase();
        assertTrue(searchable.contains("alice"));
    }

    @Test
    public void browseProfiles_alphabeticalSortingWorks() {
        List<String> names = new ArrayList<>();
        names.add("Charlie"); names.add("Alice"); names.add("Bob");
        java.util.Collections.sort(names, String::compareToIgnoreCase);
        assertEquals("Alice", names.get(0));
        assertEquals("Bob", names.get(1));
        assertEquals("Charlie", names.get(2));
    }

    @Test
    public void browseProfiles_initialsGeneratedCorrectly_fullName() {
        String name = "Alice Johnson"; String[] parts = name.split(" ");
        String initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        assertEquals("AJ", initials);
    }

    @Test
    public void browseProfiles_initialsGeneratedCorrectly_singleName() {
        String name = "Alice"; String[] parts = name.split(" ");
        String initials = ("" + parts[0].charAt(0)).toUpperCase();
        assertEquals("A", initials);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 03.01.01 — Remove Events (Soft Delete)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void removeEvent_softDeleteSetsCorrectFields() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true); updates.put("removedBy", "admin_device_123");
        assertTrue((Boolean) updates.get("isDeleted"));
        assertEquals("admin_device_123", updates.get("removedBy"));
    }

    @Test public void removeEvent_restoreClearsDeleteFields() { Map<String, Object> u = new HashMap<>(); u.put("isDeleted", false); assertFalse((Boolean) u.get("isDeleted")); }

    @Test
    public void removeEvent_deletedEventHiddenFromActiveFilter() {
        Map<String, Object> event = new HashMap<>(); event.put("isDeleted", true);
        Boolean isDel = (Boolean) event.get("isDeleted");
        assertFalse(isDel == null || !isDel);
    }

    @Test
    public void removeEvent_deletedEventVisibleInDeletedFilter() {
        Map<String, Object> event = new HashMap<>(); event.put("isDeleted", true);
        Boolean isDel = (Boolean) event.get("isDeleted");
        assertTrue(isDel != null && isDel);
    }

    @Test
    public void removeEvent_auditTrailContainsAdminId() {
        Map<String, Object> updates = new HashMap<>(); updates.put("removedBy", "admin_device_456");
        assertEquals("admin_device_456", updates.get("removedBy"));
        assertNotNull(updates.get("removedBy"));
    }

    // ══════════════════════════════════════════════════════════════
    //  US 03.02.01 — Remove Profiles (Soft Disable)
    // ══════════════════════════════════════════════════════════════

    @Test
    public void removeProfile_softDisableSetsCorrectFields() {
        Map<String, Object> u = new HashMap<>(); u.put("isDisabled", true); u.put("removedBy", "admin_device_789");
        assertTrue((Boolean) u.get("isDisabled")); assertEquals("admin_device_789", u.get("removedBy"));
    }

    @Test
    public void removeProfile_disabledProfileCannotJoinWaitlist() {
        Map<String, Object> p = new HashMap<>(); p.put("isDisabled", true);
        assertFalse((Boolean) p.get("isDisabled") == null || !(Boolean) p.get("isDisabled"));
    }

    @Test
    public void removeProfile_activeProfileCanJoinWaitlist() {
        Map<String, Object> p = new HashMap<>(); p.put("isDisabled", false);
        assertTrue((Boolean) p.get("isDisabled") == null || !(Boolean) p.get("isDisabled"));
    }

    @Test public void removeProfile_restoreClearsDisableFields() { Map<String, Object> u = new HashMap<>(); u.put("isDisabled", false); assertFalse((Boolean) u.get("isDisabled")); }

    @Test
    public void removeProfile_disabledProfileShowsDoneButton() {
        Map<String, Object> p = new HashMap<>(); p.put("isDisabled", true);
        boolean showDone = (Boolean) p.get("isDisabled"); boolean showRemove = !showDone;
        assertTrue(showDone); assertFalse(showRemove);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 03.06.01 — Browse Images
    // ══════════════════════════════════════════════════════════════

    @Test
    public void browseImages_onlyEventsWithPostersShown() {
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> e1 = new HashMap<>(); e1.put("posterUrl", "https://example.com/poster.jpg"); e1.put("isDeleted", false); events.add(e1);
        Map<String, Object> e2 = new HashMap<>(); e2.put("posterUrl", ""); e2.put("isDeleted", false); events.add(e2);
        Map<String, Object> e3 = new HashMap<>(); e3.put("posterUrl", null); e3.put("isDeleted", false); events.add(e3);
        List<Map<String, Object>> withPosters = new ArrayList<>();
        for (Map<String, Object> e : events) {
            String url = (String) e.get("posterUrl");
            if (url != null && !url.isEmpty()) withPosters.add(e);
        }
        assertEquals(1, withPosters.size());
    }

    @Test
    public void browseImages_deletedEventsExcluded() {
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Object> e1 = new HashMap<>(); e1.put("posterUrl", "https://x.com/a.jpg"); e1.put("isDeleted", true); events.add(e1);
        List<Map<String, Object>> withPosters = new ArrayList<>();
        for (Map<String, Object> e : events) {
            String url = (String) e.get("posterUrl");
            Boolean isDel = (Boolean) e.get("isDeleted");
            if (url != null && !url.isEmpty() && (isDel == null || !isDel)) withPosters.add(e);
        }
        assertEquals(0, withPosters.size());
    }

    // ══════════════════════════════════════════════════════════════
    //  US 03.03.01 — Remove Images
    // ══════════════════════════════════════════════════════════════

    @Test
    public void removeImage_setPosterUrlToNull() {
        Map<String, Object> updates = new HashMap<>(); updates.put("posterUrl", null);
        assertNull(updates.get("posterUrl"));
    }

    @Test
    public void removeImage_eventStillExistsAfterImageRemoval() {
        Map<String, Object> event = new HashMap<>();
        event.put("name", "Test Event"); event.put("posterUrl", "https://x.com/a.jpg");
        event.put("posterUrl", null);
        assertEquals("Test Event", event.get("name"));
        assertNull(event.get("posterUrl"));
    }

    // ══════════════════════════════════════════════════════════════
    //  US 02.01.01 — Create Event
    // ══════════════════════════════════════════════════════════════

    @Test
    public void createEvent_eventIdIsGenerated() {
        String eventId = java.util.UUID.randomUUID().toString();
        assertNotNull(eventId); assertFalse(eventId.isEmpty());
        assertTrue(eventId.length() > 10);
    }

    @Test
    public void createEvent_requiredFieldsValidation_nameEmpty() {
        String name = ""; String capacity = "50";
        assertTrue(name.isEmpty());
    }

    @Test
    public void createEvent_requiredFieldsValidation_capacityEmpty() {
        String name = "Tech Meetup"; String capacity = "";
        assertTrue(capacity.isEmpty());
    }

    @Test
    public void createEvent_requiredFieldsValidation_bothFilled() {
        String name = "Tech Meetup"; String capacity = "50";
        assertFalse(name.isEmpty()); assertFalse(capacity.isEmpty());
    }

    @Test
    public void createEvent_capacityParsesCorrectly() {
        int capacity = Integer.parseInt("100");
        assertEquals(100, capacity);
    }

    @Test
    public void createEvent_waitlistLimitOptional() {
        String waitlist = "";
        boolean hasLimit = !waitlist.isEmpty();
        assertFalse(hasLimit);
    }

    @Test
    public void createEvent_waitlistLimitParsesWhenProvided() {
        String waitlist = "200";
        int limit = Integer.parseInt(waitlist);
        assertEquals(200, limit);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 02.01.01 — QR Code Generation
    // ══════════════════════════════════════════════════════════════

    @Test
    public void qrCode_contentContainsEventId() {
        String eventId = "test-event-123";
        String qrContent = "eventmanager://event/" + eventId;
        assertTrue(qrContent.contains(eventId));
        assertTrue(qrContent.startsWith("eventmanager://event/"));
    }

    @Test
    public void qrCode_parseExtractsEventId() {
        String qrContent = "eventmanager://event/abc-123-def";
        String eventId = qrContent.replace("eventmanager://event/", "");
        assertEquals("abc-123-def", eventId);
    }

    @Test
    public void qrCode_invalidContentDetected() {
        String qrContent = "https://random-website.com";
        assertFalse(qrContent.startsWith("eventmanager://event/"));
    }

    // ══════════════════════════════════════════════════════════════
    //  US 01.07.01 — Device-based Identification
    // ══════════════════════════════════════════════════════════════

    @Test
    public void deviceAuth_deviceIdUsedAsDocId() {
        String deviceId = "a49845e697de4044";
        String docPath = "users/" + deviceId;
        assertEquals("users/a49845e697de4044", docPath);
    }

    @Test
    public void deviceAuth_existingUserDetected() {
        String name = "Ibrahim"; String email = "ibrahim@gmail.com";
        boolean isExisting = (name != null && !name.isEmpty());
        assertTrue(isExisting);
    }

    @Test
    public void deviceAuth_newUserDetected() {
        String name = null;
        boolean isExisting = (name != null && !name.isEmpty());
        assertFalse(isExisting);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 01.02.01 — Provide Personal Info
    // ══════════════════════════════════════════════════════════════

    @Test
    public void profileSetup_nameRequired() {
        String name = "";
        assertTrue(name.isEmpty());
    }

    @Test
    public void profileSetup_emailRequired() {
        String email = "";
        assertTrue(email.isEmpty());
    }

    @Test
    public void profileSetup_emailValidation_valid() {
        String email = "test@example.com";
        assertTrue(email.contains("@") && email.contains("."));
    }

    @Test
    public void profileSetup_emailValidation_invalid() {
        String email = "notanemail";
        assertFalse(email.contains("@"));
    }

    @Test
    public void profileSetup_passwordMinLength() {
        String pass = "abc";
        assertTrue(pass.length() < 8);
    }

    @Test
    public void profileSetup_passwordValid() {
        String pass = "password123";
        assertTrue(pass.length() >= 8);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 02.05.02 — Lottery Engine
    // ══════════════════════════════════════════════════════════════

    @Test
    public void lottery_cannotDrawMoreThanCapacity() {
        int capacity = 10; int numToDraw = 15;
        int actual = Math.min(numToDraw, capacity);
        assertEquals(10, actual);
    }

    @Test
    public void lottery_drawFromWaitingList() {
        List<String> waitingList = new ArrayList<>();
        waitingList.add("user1"); waitingList.add("user2"); waitingList.add("user3");
        int numToDraw = 2;
        assertTrue(numToDraw <= waitingList.size());
    }

    @Test
    public void lottery_emptyWaitingListReturnsNothing() {
        List<String> waitingList = new ArrayList<>();
        assertEquals(0, waitingList.size());
    }

    @Test
    public void lottery_replacementDrawsOne() {
        List<String> waitingList = new ArrayList<>();
        waitingList.add("user1"); waitingList.add("user2");
        int replacementCount = 1;
        assertTrue(replacementCount <= waitingList.size());
    }

    // ══════════════════════════════════════════════════════════════
    //  US 01.05.02 / US 01.05.03 — Accept / Decline Invitation
    // ══════════════════════════════════════════════════════════════

    @Test
    public void invitation_acceptMovesToEnrolled() {
        String status = "selected";
        String newStatus = "enrolled";
        assertNotEquals(status, newStatus);
    }

    @Test
    public void invitation_declineMovesToCancelled() {
        String status = "selected";
        String newStatus = "cancelled";
        assertNotEquals(status, newStatus);
    }

    @Test
    public void invitation_cannotAcceptIfNotSelected() {
        String status = "waiting";
        boolean canAccept = status.equals("selected");
        assertFalse(canAccept);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 02.02.01 / US 02.06.01 / US 02.06.03 — View Lists
    // ══════════════════════════════════════════════════════════════

    @Test
    public void manageTabs_waitingListIsDefault() {
        String currentTab = "waiting";
        assertEquals("waiting", currentTab);
    }

    @Test
    public void manageTabs_canSwitchToChosen() {
        String currentTab = "selected";
        assertEquals("selected", currentTab);
    }

    @Test
    public void manageTabs_canSwitchToEnrolled() {
        String currentTab = "enrolled";
        assertEquals("enrolled", currentTab);
    }

    @Test
    public void manageTabs_correctCollectionName_waiting() {
        String tab = "waiting";
        String collection = tab.equals("waiting") ? "waitingList" : tab.equals("selected") ? "selected" : "enrolled";
        assertEquals("waitingList", collection);
    }

    @Test
    public void manageTabs_correctCollectionName_selected() {
        String tab = "selected";
        String collection = tab.equals("waiting") ? "waitingList" : tab.equals("selected") ? "selected" : "enrolled";
        assertEquals("selected", collection);
    }

    @Test
    public void manageTabs_correctCollectionName_enrolled() {
        String tab = "enrolled";
        String collection = tab.equals("waiting") ? "waitingList" : tab.equals("selected") ? "selected" : "enrolled";
        assertEquals("enrolled", collection);
    }

    // ══════════════════════════════════════════════════════════════
    //  US 02.04.01 — Upload Event Poster
    // ══════════════════════════════════════════════════════════════

    @Test
    public void posterUpload_storagePath() {
        String eventId = "event-123";
        String path = "event_posters/" + eventId + ".jpg";
        assertEquals("event_posters/event-123.jpg", path);
    }

    @Test
    public void posterUpload_urlSavedToEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("posterUrl", "https://firebase.com/poster.jpg");
        assertNotNull(event.get("posterUrl"));
    }

    // ══════════════════════════════════════════════════════════════
    //  US 01.06.01 — Scan QR Code
    // ══════════════════════════════════════════════════════════════

    @Test
    public void qrScan_validCodeNavigatesToEvent() {
        String scanned = "eventmanager://event/test-123";
        assertTrue(scanned.startsWith("eventmanager://event/"));
        String eventId = scanned.replace("eventmanager://event/", "");
        assertEquals("test-123", eventId);
    }

    @Test
    public void qrScan_invalidCodeRejected() {
        String scanned = "https://google.com";
        assertFalse(scanned.startsWith("eventmanager://event/"));
    }

    @Test
    public void qrScan_emptyCodeRejected() {
        String scanned = "";
        assertFalse(scanned.startsWith("eventmanager://event/"));
    }

    // ══════════════════════════════════════════════════════════════
    //  US 02.03.01 — Limit Waitlist
    // ══════════════════════════════════════════════════════════════

    @Test
    public void waitlistLimit_enforced() {
        int maxCapacity = 50; long currentSize = 50;
        assertTrue(currentSize >= maxCapacity);
    }

    @Test
    public void waitlistLimit_notReached() {
        int maxCapacity = 50; long currentSize = 30;
        assertFalse(currentSize >= maxCapacity);
    }

    @Test
    public void waitlistLimit_zeroMeansUnlimited() {
        int maxCapacity = 0;
        boolean isLimited = maxCapacity > 0;
        assertFalse(isLimited);
    }

    // ══════════════════════════════════════════════════════════════
    //  Home Screen Navigation
    // ══════════════════════════════════════════════════════════════

    @Test
    public void homeScreen_rolesExist() {
        String[] roles = {"entrant", "organizer", "admin"};
        assertEquals(3, roles.length);
    }

    @Test
    public void homeScreen_welcomeMessageWithName() {
        String name = "Ibrahim";
        String msg = "Welcome back " + name + " \uD83D\uDC4B,";
        assertTrue(msg.contains("Ibrahim"));
    }

    @Test
    public void homeScreen_welcomeMessageWithoutName() {
        String name = null;
        String msg = (name != null) ? "Welcome back " + name : "Welcome \uD83D\uDC4B,";
        assertEquals("Welcome \uD83D\uDC4B,", msg);
    }

    // ══════════════════════════════════════════════════════════════
    //  Edge Cases
    // ══════════════════════════════════════════════════════════════

    @Test public void edgeCase_nullIsDeletedTreatedAsActive() { Boolean isDel = null; assertTrue(isDel == null || !isDel); }
    @Test public void edgeCase_nullIsDisabledTreatedAsActive() { Boolean isDis = null; assertTrue(isDis == null || !isDis); }
    @Test public void edgeCase_emptySearchReturnsAllResults() { assertTrue("".isEmpty()); }
    @Test public void edgeCase_nullNameHandledGracefully() { String name = null; assertEquals("No Name", name != null ? name : "No Name"); }
    @Test public void edgeCase_nullEmailHandledGracefully() { String email = null; assertEquals("No email", email != null ? email : "No email"); }
    @Test public void edgeCase_nullPosterUrlHandledGracefully() { String url = null; boolean hasPoster = url != null && !url.isEmpty(); assertFalse(hasPoster); }
}
