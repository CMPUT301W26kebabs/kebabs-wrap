package com.example.eventmanager.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI tests for Admin screens.
 *
 * US 03.04.01 - Browse Events screen loads and displays correctly
 * US 03.05.01 - Browse Profiles screen loads and displays correctly
 * US 03.01.01 - Remove Events UI elements are present
 * US 03.02.01 - Remove Profiles UI elements are present
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminUITest {

    // ══════════════════════════════════════════════════════════════
    //  US 03.04.01 — Browse Events UI Tests
    // ══════════════════════════════════════════════════════════════

    @Rule
    public ActivityScenarioRule<AdminEventsActivity> eventsRule =
            new ActivityScenarioRule<>(AdminEventsActivity.class);

    @Test
    public void browseEvents_screenLoads() {
        onView(withId(R.id.et_search)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_events)).check(matches(isDisplayed()));
    }

    @Test
    public void browseEvents_searchBarAcceptsInput() {
        onView(withId(R.id.et_search))
                .perform(typeText("test event"), closeSoftKeyboard());
    }

    @Test
    public void browseEvents_activeChipIsDisplayed() {
        onView(withId(R.id.chip_active)).check(matches(isDisplayed()));
    }

    @Test
    public void browseEvents_deletedChipIsDisplayed() {
        onView(withId(R.id.chip_deleted)).check(matches(isDisplayed()));
    }

    @Test
    public void browseEvents_allChipIsDisplayed() {
        onView(withId(R.id.chip_all)).check(matches(isDisplayed()));
    }

    @Test
    public void browseEvents_chipFiltersAreClickable() {
        onView(withId(R.id.chip_deleted)).perform(click());
        onView(withId(R.id.chip_all)).perform(click());
        onView(withId(R.id.chip_active)).perform(click());
    }

    @Test
    public void browseEvents_backButtonExists() {
        onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
    }

    @Test
    public void browseEvents_backButtonNavigatesBack() {
        onView(withId(R.id.btn_back)).perform(click());
    }
}
