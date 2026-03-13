package com.example.eventmanager.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.MainActivity;
import com.example.eventmanager.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI tests for Admin Dashboard (MainActivity).
 * Verifies navigation cards and live counts are displayed.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDashboardUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> mainRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void dashboard_eventCardIsDisplayed() {
        onView(withId(R.id.card_events)).check(matches(isDisplayed()));
    }

    @Test
    public void dashboard_profileCardIsDisplayed() {
        onView(withId(R.id.card_profiles)).check(matches(isDisplayed()));
    }

    @Test
    public void dashboard_imageCardIsDisplayed() {
        onView(withId(R.id.card_images)).check(matches(isDisplayed()));
    }

    @Test
    public void dashboard_eventCountIsDisplayed() {
        onView(withId(R.id.tv_event_count)).check(matches(isDisplayed()));
    }

    @Test
    public void dashboard_profileCountIsDisplayed() {
        onView(withId(R.id.tv_profile_count)).check(matches(isDisplayed()));
    }

    @Test
    public void dashboard_eventCardNavigatesToEvents() {
        onView(withId(R.id.card_events)).perform(click());
        // Should open AdminEventsActivity
        onView(withId(R.id.rv_events)).check(matches(isDisplayed()));
    }

    @Test
    public void dashboard_profileCardNavigatesToProfiles() {
        onView(withId(R.id.card_profiles)).perform(click());
        // Should open AdminProfilesActivity
        onView(withId(R.id.rv_profiles)).check(matches(isDisplayed()));
    }
}
