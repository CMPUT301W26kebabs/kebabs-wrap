package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI tests for EventActivity (US1 - Join Waiting List).
 *
 * Firestore setup required:
 *   "test_event_open"   — open window, waitlistLimit=0, waitingList=[] (empty)
 *   "test_event_closed" — registrationEnd in the past
 *
 * Before running testJoinButton_DisabledAfterClick:
 *   Clear the waitingList array in test_event_open in Firestore.
 */
@RunWith(AndroidJUnit4.class)
public class EventActivityJoinTest {

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Intent buildIntent(String eventId) {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), EventActivity.class);
        i.putExtra("EVENT_ID", eventId);
        return i;
    }

    /**
     * Force-clicks a view by calling performClick() directly on it.
     * This bypasses Espresso's visibility/scroll constraints so it works
     * regardless of where the button sits in the layout hierarchy.
     */
    private static ViewAction forceClick() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return Matchers.any(View.class); // no constraints — click anything
            }
            @Override
            public String getDescription() {
                return "force click ignoring visibility";
            }
            @Override
            public void perform(UiController uiController, View view) {
                view.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /** Polls every 500ms until view is enabled, up to maxWaitMs. */
    private void waitForEnabled(int viewId, long maxWaitMs) throws InterruptedException {
        long waited = 0;
        while (waited < maxWaitMs) {
            try {
                onView(withId(viewId)).check(matches(isEnabled()));
                return;
            } catch (AssertionError e) {
                Thread.sleep(500);
                waited += 500;
            }
        }
    }

    /** Polls every 500ms until view is disabled, up to maxWaitMs. */
    private void waitForDisabled(int viewId, long maxWaitMs) throws InterruptedException {
        long waited = 0;
        while (waited < maxWaitMs) {
            try {
                onView(withId(viewId)).check(matches(not(isEnabled())));
                return;
            } catch (AssertionError e) {
                Thread.sleep(500);
                waited += 500;
            }
        }
    }

    // ---------------------------------------------------------------
    // US1-T1: Event info renders on screen
    // ---------------------------------------------------------------

    @Test
    public void testEventInfo_IsDisplayed() {
        try (ActivityScenario<EventActivity> s =
                     ActivityScenario.launch(buildIntent("test_event_open"))) {
            onView(withId(R.id.tvEventTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.tvEventDescription)).check(matches(isDisplayed()));
            onView(withId(R.id.btnJoinWaitingList)).check(matches(isDisplayed()));
        }
    }

    // ---------------------------------------------------------------
    // US1-T2: Button active when registration is open
    // ---------------------------------------------------------------

    @Test
    public void testJoinButton_EnabledWhenRegistrationOpen() throws InterruptedException {
        try (ActivityScenario<EventActivity> s =
                     ActivityScenario.launch(buildIntent("test_event_open"))) {

            waitForEnabled(R.id.btnJoinWaitingList, 10000);

            onView(withId(R.id.btnJoinWaitingList))
                    .check(matches(isEnabled()))
                    .check(matches(withText("JOIN WAITING LIST")));

            onView(withId(R.id.tvWaitingListStatus))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.tvJoinErrorMessage))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    // ---------------------------------------------------------------
    // US1-T3: Button greyed out when registration is closed
    // ---------------------------------------------------------------

    @Test
    public void testJoinButton_DisabledWhenRegistrationClosed() throws InterruptedException {
        try (ActivityScenario<EventActivity> s =
                     ActivityScenario.launch(buildIntent("test_event_closed"))) {

            waitForDisabled(R.id.btnJoinWaitingList, 10000);

            onView(withId(R.id.btnJoinWaitingList))
                    .check(matches(not(isEnabled())))
                    .check(matches(withText("REGISTRATION CLOSED")));

            onView(withId(R.id.tvJoinErrorMessage))
                    .check(matches(isDisplayed()));
        }
    }

    // ---------------------------------------------------------------
    // US1-T4: Button greyed out immediately after tap (prevents double-join)
    // BEFORE RUNNING: clear waitingList in test_event_open in Firestore
    // ---------------------------------------------------------------

    @Test
    public void testJoinButton_DisabledAfterClick() throws InterruptedException {
        try (ActivityScenario<EventActivity> s =
                     ActivityScenario.launch(buildIntent("test_event_open"))) {

            // Wait for Firestore to load and button to become active
            waitForEnabled(R.id.btnJoinWaitingList, 10000);

            // Force-click bypasses scroll/visibility constraints on MaterialButton
            onView(withId(R.id.btnJoinWaitingList))
                    .perform(forceClick());

            // Button must be disabled immediately after tap
            waitForDisabled(R.id.btnJoinWaitingList, 5000);
            onView(withId(R.id.btnJoinWaitingList))
                    .check(matches(not(isEnabled())));
        }
    }
}