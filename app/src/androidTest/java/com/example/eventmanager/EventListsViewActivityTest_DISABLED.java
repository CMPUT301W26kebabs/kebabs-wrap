package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI tests for OrganizerEventDetailActivity.
 * Tests US2: View Waiting List and US3: View Chosen List.
 *
 * NOTE: These tests use a real Firestore connection.
 * Ensure your Firestore has a document with id "test_event_open"
 * with populated waitingList and chosenList arrays.
 */
@RunWith(AndroidJUnit4.class)
public class EventListsViewActivityTest_DISABLED {

    // US2 & US3: Tab layout
    /**
     * The screen should display two tabs: Waiting List and Chosen List.
     */
    @Test
    public void testTabsAreDisplayed() {
        try (ActivityScenario<EventListsViewActivity> scenario =
                     ActivityScenario.launch(EventListsViewActivity.class)) {

            onView(withId(R.id.tabHost))
                    .check(matches(isDisplayed()));
        }
    }

    // US2: Waiting List tab
    /**
     * The waiting list count TextView should be visible on the Waiting tab.
     */
    @Test
    public void testWaitingListCount_IsDisplayed() {
        try (ActivityScenario<EventListsViewActivity> scenario =
                     ActivityScenario.launch(EventListsViewActivity.class)) {

            onView(withId(R.id.tvWaitingCount))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * When the waiting list is empty, the empty state message should be shown
     * and the RecyclerView should be hidden.
     */
    @Test
    public void testWaitingList_EmptyStateShown() {
        try (ActivityScenario<EventListsViewActivity> scenario =
                     ActivityScenario.launch(EventListsViewActivity.class)) {

            onView(withId(R.id.tvWaitingEmpty))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.rvWaitingList))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    /**
     * When the waiting list has entrants, the RecyclerView should be visible
     * and the empty state should be hidden.
     * Requires "test_event_open" in Firestore to have at least one waitingList entry.
     */
    @Test
    public void testWaitingList_RecyclerViewShownWhenPopulated() {
        try (ActivityScenario<EventListsViewActivity> scenario =
                     ActivityScenario.launch(EventListsViewActivity.class)) {

            onView(withId(R.id.rvWaitingList))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvWaitingEmpty))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    // US3: Chosen List tab
    /**
     * Tapping the Chosen List tab should make the chosen list UI visible.
     */
    @Test
    public void testChosenTab_IsClickable() {
        try (ActivityScenario<EventListsViewActivity> scenario =
                     ActivityScenario.launch(EventListsViewActivity.class)) {

            // Click the second tab (Chosen List)
            onView(withText("Chosen List")).perform(click());

            onView(withId(R.id.tvChosenCount))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * When the chosen list is empty, the empty state message should be shown.
     */
    @Test
    public void testChosenList_EmptyStateShown() {
        try (ActivityScenario<EventListsViewActivity> scenario =
                     ActivityScenario.launch(EventListsViewActivity.class)) {

            onView(withText("Chosen List")).perform(click());

            onView(withId(R.id.tvChosenEmpty))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * When the chosen list has entrants, the RecyclerView should be visible.
     * Requires "test_event_open" in Firestore to have at least one chosenList entry.
     */
    @Test
    public void testChosenList_RecyclerViewShownWhenPopulated() {
        try (ActivityScenario<EventListsViewActivity> scenario =
                     ActivityScenario.launch(EventListsViewActivity.class)) {

            onView(withText("Chosen List")).perform(click());

            onView(withId(R.id.rvChosenList))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.tvChosenEmpty))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }
}
