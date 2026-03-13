package com.example.eventmanager;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Date;

/**
 * Unit tests for the Event model class.
 * Covers all functional logic for US1–US4:
 *   US1 - canUserJoin / isRegistrationWindowActive
 *   US2 - getWaitingList size and contents
 *   US3 - isUserChosen / getChosenList
 *   US4 - hasAttendeesSpace / attendee count
 */
public class EventTest {

    private Event event;
    private Date futureDate;
    private Date pastDate;

    @Before
    public void setUp() {
        event = new Event();
        event.setCapacity(2);
        event.setWaitlistLimit(5);

        long now = System.currentTimeMillis();
        pastDate  = new Date(now - 100000);
        futureDate = new Date(now + 100000);
    }


    // US1: Registration Window
    @Test
    public void testRegistrationWindowOpen() {
        event.setRegistrationStart(pastDate);
        event.setRegistrationEnd(futureDate);
        assertTrue("Registration should be open", event.isRegistrationWindowActive());
    }

    @Test
    public void testRegistrationWindowNotStarted() {
        event.setRegistrationStart(futureDate);
        event.setRegistrationEnd(new Date(futureDate.getTime() + 100000));
        assertFalse("Registration should not be open yet", event.isRegistrationWindowActive());
    }

    @Test
    public void testRegistrationWindowClosed() {
        event.setRegistrationStart(new Date(pastDate.getTime() - 100000));
        event.setRegistrationEnd(pastDate);
        assertFalse("Registration window should be closed", event.isRegistrationWindowActive());
    }

    @Test
    public void testRegistrationWindowNullDates() {
        // No dates set — window should not be active
        assertFalse("Null dates should mean window is not active", event.isRegistrationWindowActive());
    }


    // US1: canUserJoin
    @Test
    public void testCanUserJoin_Success() {
        event.setRegistrationStart(pastDate);
        event.setRegistrationEnd(futureDate);
        event.setWaitingList(new ArrayList<>());
        assertTrue("New user should be able to join", event.canUserJoin("device1", 5));
    }

    @Test
    public void testCanUserJoin_RegistrationClosed() {
        event.setRegistrationStart(futureDate);
        event.setRegistrationEnd(new Date(futureDate.getTime() + 100000));
        assertFalse("User should not join when registration is closed", event.canUserJoin("device1", 5));
    }

    @Test
    public void testCanUserJoin_DuplicateJoin() {
        event.setRegistrationStart(pastDate);
        event.setRegistrationEnd(futureDate);

        ArrayList<String> list = new ArrayList<>();
        list.add("ExistingUser");
        event.setWaitingList(list);

        assertFalse("User already on list should not join twice",
                event.canUserJoin("ExistingUser", 5));
    }

    @Test
    public void testCanUserJoin_WaitlistFull() {
        event.setRegistrationStart(pastDate);
        event.setRegistrationEnd(futureDate);

        ArrayList<String> fullList = new ArrayList<>();
        for (int i = 0; i < 5; i++) fullList.add("User" + i);
        event.setWaitingList(fullList);

        assertFalse("Should not join a full waitlist", event.canUserJoin("NewUser", 5));
    }


    // US2: Waiting List contents
    @Test
    public void testWaitingListInitiallyEmpty() {
        assertEquals("Waiting list should start empty", 0, event.getWaitingList().size());
    }

    @Test
    public void testWaitingListContainsAddedUser() {
        ArrayList<String> list = new ArrayList<>();
        list.add("device123");
        event.setWaitingList(list);
        assertTrue("Waiting list should contain added user",
                event.getWaitingList().contains("device123"));
    }

    @Test
    public void testWaitingListCount() {
        ArrayList<String> list = new ArrayList<>();
        list.add("d1");
        list.add("d2");
        list.add("d3");
        event.setWaitingList(list);
        assertEquals("Waiting list count should be 3", 3, event.getWaitingList().size());
    }

    @Test
    public void testWaitingListDoesNotContainNonMember() {
        ArrayList<String> list = new ArrayList<>();
        list.add("device123");
        event.setWaitingList(list);
        assertFalse("Non-member should not be on waiting list",
                event.getWaitingList().contains("unknownDevice"));
    }


    // US3: Chosen List
    @Test
    public void testIsUserChosen_True() {
        ArrayList<String> chosen = new ArrayList<>();
        chosen.add("selectedUser");
        event.setChosenList(chosen);
        assertTrue("User should be marked as chosen", event.isUserChosen("selectedUser"));
    }

    @Test
    public void testIsUserChosen_False() {
        event.setChosenList(new ArrayList<>());
        assertFalse("User not in chosen list should return false",
                event.isUserChosen("randomUser"));
    }

    @Test
    public void testChosenListInitiallyEmpty() {
        assertEquals("Chosen list should start empty", 0, event.getChosenList().size());
    }

    @Test
    public void testChosenListCount() {
        ArrayList<String> chosen = new ArrayList<>();
        chosen.add("u1");
        chosen.add("u2");
        event.setChosenList(chosen);
        assertEquals("Chosen list should have 2 entrants", 2, event.getChosenList().size());
    }

    @Test
    public void testIsUserChosen_NullList() {
        event.setChosenList(null);
        assertFalse("isUserChosen should handle null list gracefully",
                event.isUserChosen("anyUser"));
    }


    // US4: Accept Invitation — attendee space
    @Test
    public void testHasAttendeesSpace_WhenEmpty() {
        event.setCapacity(2);
        event.setAttendees(new ArrayList<>());
        assertTrue("Should have space when attendees list is empty", event.hasAttendeesSpace());
    }

    @Test
    public void testHasAttendeesSpace_WhenFull() {
        event.setCapacity(2);
        ArrayList<String> attendees = new ArrayList<>();
        attendees.add("user1");
        attendees.add("user2");
        event.setAttendees(attendees);
        assertFalse("Should not have space when at full capacity", event.hasAttendeesSpace());
    }

    @Test
    public void testHasAttendeesSpace_OneSlotLeft() {
        event.setCapacity(3);
        ArrayList<String> attendees = new ArrayList<>();
        attendees.add("user1");
        attendees.add("user2");
        event.setAttendees(attendees);
        assertTrue("Should still have space with one slot remaining", event.hasAttendeesSpace());
    }

    @Test
    public void testAcceptInvitation_UserMovedFromChosenToAttendees() {

        ArrayList<String> chosen = new ArrayList<>();
        chosen.add("device1");
        event.setChosenList(chosen);
        event.setAttendees(new ArrayList<>());
        event.setCapacity(5);

        // Verify preconditions
        assertTrue("User should be in chosen list before accepting", event.isUserChosen("device1"));
        assertTrue("There should be space to accept", event.hasAttendeesSpace());

        event.getChosenList().remove("device1");
        event.getAttendees().add("device1");

        // Verify postconditions
        assertFalse("User should no longer be in chosen list", event.isUserChosen("device1"));
        assertTrue("User should now be in attendees list", event.getAttendees().contains("device1"));
    }

    @Test
    public void testAcceptInvitation_CapacityNotExceeded() {
        event.setCapacity(1);
        ArrayList<String> attendees = new ArrayList<>();
        attendees.add("existingUser");
        event.setAttendees(attendees);

        assertFalse("Should not be able to accept when event is full", event.hasAttendeesSpace());
    }
}
