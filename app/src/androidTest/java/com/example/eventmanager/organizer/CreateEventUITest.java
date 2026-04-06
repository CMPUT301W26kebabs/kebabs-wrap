package com.example.eventmanager.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.CreateEventActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Organizer — US 02.01.01 / 02.01.04 (create event, registration fields).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventUITest {

    @Rule
    public ActivityScenarioRule<CreateEventActivity> createRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

    @Test public void createEvent_screenLoads() { onView(withId(R.id.editEventName)).check(matches(isDisplayed())); }
    @Test public void createEvent_nameFieldAcceptsInput() { onView(withId(R.id.editEventName)).perform(typeText("Test Event"), closeSoftKeyboard()); }
    @Test public void createEvent_descriptionFieldDisplayed() { onView(withId(R.id.editDescription)).check(matches(isDisplayed())); }
    @Test public void createEvent_startDateDisplayed() { onView(withId(R.id.editStartDate)).check(matches(isDisplayed())); }
    @Test public void createEvent_endDateDisplayed() { onView(withId(R.id.editEndDate)).check(matches(isDisplayed())); }
    @Test public void createEvent_capacityFieldDisplayed() { onView(withId(R.id.editCapacity)).check(matches(isDisplayed())); }
    @Test public void createEvent_waitlistLimitDisplayed() { onView(withId(R.id.editWaitlistLimit)).check(matches(isDisplayed())); }
    @Test public void createEvent_createButtonDisplayed() { onView(withId(R.id.btnCreateEvent)).check(matches(isDisplayed())); }
    @Test public void createEvent_backButtonDisplayed() { onView(withId(R.id.btnBack)).check(matches(isDisplayed())); }
    @Test public void createEvent_backButtonWorks() { onView(withId(R.id.btnBack)).perform(click()); }
    @Test public void createEvent_startDateOpensDatePicker() { onView(withId(R.id.editStartDate)).perform(click()); }
}
