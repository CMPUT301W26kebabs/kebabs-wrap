package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeScreenUITest {

    @Rule
    public ActivityScenarioRule<HomeActivity> homeRule =
            new ActivityScenarioRule<>(HomeActivity.class);

    @Test public void home_welcomeMessageDisplayed() { onView(withId(R.id.tv_welcome)).check(matches(isDisplayed())); }
    @Test public void home_entrantChipDisplayed() { onView(withId(R.id.chip_entrant)).check(matches(isDisplayed())); }
    @Test public void home_organizerChipDisplayed() { onView(withId(R.id.chip_organizer)).check(matches(isDisplayed())); }
    @Test public void home_adminChipDisplayed() { onView(withId(R.id.chip_admin)).check(matches(isDisplayed())); }
    @Test public void home_upcomingEventsDisplayed() { onView(withId(R.id.rv_upcoming_events)).check(matches(isDisplayed())); }
    @Test public void home_nearbyEventsDisplayed() { onView(withId(R.id.rv_nearby_events)).check(matches(isDisplayed())); }
    @Test public void home_bottomNavExploreDisplayed() { onView(withId(R.id.nav_explore)).check(matches(isDisplayed())); }
    @Test public void home_bottomNavEventsDisplayed() { onView(withId(R.id.nav_events)).check(matches(isDisplayed())); }
    @Test public void home_bottomNavScanDisplayed() { onView(withId(R.id.nav_scan)).check(matches(isDisplayed())); }
    @Test public void home_bottomNavProfileDisplayed() { onView(withId(R.id.nav_profile)).check(matches(isDisplayed())); }
    @Test public void home_adminChipNavigates() { onView(withId(R.id.chip_admin)).perform(click()); onView(withId(R.id.card_events)).check(matches(isDisplayed())); }
    @Test public void home_organizerChipNavigates() { onView(withId(R.id.chip_organizer)).perform(click()); onView(withId(R.id.recycler_my_events)).check(matches(isDisplayed())); }
}
