package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.ui.BookedEventsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BookedEventsActivityTest {

    @Rule
    public ActivityScenarioRule<BookedEventsActivity> rule =
            new ActivityScenarioRule<>(BookedEventsActivity.class);

    @Test
    public void bookedEventsScreen_coreElementsAreDisplayed() {
        onView(withText(R.string.booked_events_title)).check(matches(isDisplayed()));
        onView(withId(R.id.bookedEventsNext)).check(matches(isDisplayed()));
        onView(withId(R.id.calendarPhoneCard)).check(matches(isDisplayed()));
    }
}
