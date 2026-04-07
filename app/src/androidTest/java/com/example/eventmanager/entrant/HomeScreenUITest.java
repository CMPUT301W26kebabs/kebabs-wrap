package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.HomeActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Entrant — US 01.01.03 (discover events), navigation, admin shortcut (US 03.09.01).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeScreenUITest {

    @Rule
    public ActivityScenarioRule<HomeActivity> homeRule =
            new ActivityScenarioRule<>(HomeActivity.class);

    @Test
    public void home_welcomeMessageDisplayed() {
        onView(withId(R.id.tv_welcome)).check(matches(isDisplayed()));
    }

    @Test
    public void home_feedChipsDisplayed() {
        onView(withId(R.id.chip_all_events)).check(matches(isDisplayed()));
        onView(withId(R.id.chip_following)).check(matches(isDisplayed()));
    }

    @Test
    public void home_upcomingAndNearbyListsDisplayed() {
        onView(withId(R.id.rv_upcoming_events)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_nearby_events)).check(matches(isDisplayed()));
    }

    @Test
    public void home_bottomNavDisplayed() {
        onView(withId(R.id.nav_explore)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_events)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_scan)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_profile)).check(matches(isDisplayed()));
    }

    @Test
    public void home_adminShortcut_opensAdminDashboard() {
        onView(withId(R.id.btn_admin_home)).perform(click());
        onView(withId(R.id.card_events)).check(matches(isDisplayed()));
    }

    @Test
    public void home_navMyEvents_opensMyEvents() {
        onView(withId(R.id.nav_events)).perform(click());
        onView(withId(R.id.recycler_my_events)).check(matches(isDisplayed()));
    }
}
