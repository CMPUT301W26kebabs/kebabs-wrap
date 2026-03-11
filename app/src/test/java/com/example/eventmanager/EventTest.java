package com.example.eventmanager;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import java.util.ArrayList;

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
        pastDate = new Date(now - 100000);
        futureDate = new Date(now + 100000);
    }

    @Test
    public void testRegistrationWindow() {
        event.setRegistrationStart(pastDate);
        event.setRegistrationEnd(futureDate);
        assertTrue("Registration should be open", event.isRegistrationWindowActive());

        event.setRegistrationStart(futureDate);
        assertFalse("Registration should be closed (hasn't started)", event.isRegistrationWindowActive());
    }

    @Test
    public void testWaitlistCapacity() {
        event.setRegistrationStart(pastDate);
        event.setRegistrationEnd(futureDate);

        // Fill waitlist to limit (5)
        ArrayList<String> fullList = new ArrayList<>();
        for(int i=0; i<5; i++) fullList.add("User" + i);
        event.setWaitingList(fullList);

        assertFalse("Should not be able to join full waitlist", event.canUserJoin("NewUser", event.getWaitingListCount()));
    }

    @Test
    public void testDuplicateJoin() {
        event.setRegistrationStart(pastDate);
        event.setRegistrationEnd(futureDate);

        ArrayList<String> list = new ArrayList<>();
        list.add("ExistingUser");
        event.setWaitingList(list);

        assertFalse("User already on list should not be able to join twice", event.canUserJoin("ExistingUser", event.getWaitingListCount()));
    }
}
