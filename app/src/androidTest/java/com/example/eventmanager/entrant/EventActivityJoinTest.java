package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.ui.EventDetailsActivity;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/**
 * Entrant — US 01.01.01, 01.06.02 (join waiting list from event details).
 *
 * <p>Seeds {@link EventDetailsTestFixtures} in Firestore before each test and clears the
 * emulator's device from the open event's waiting list so join tests start from a clean state.
 */
@RunWith(AndroidJUnit4.class)
public class EventActivityJoinTest {

    private String deviceId;

    private Intent buildIntent(String eventId) {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsActivity.class);
        i.putExtra("EVENT_ID", eventId);
        return i;
    }

    private static ViewAction forceClick() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return Matchers.any(View.class);
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

    private void waitForButtonText(int viewId, String text, long maxWaitMs) throws InterruptedException {
        long waited = 0;
        while (waited < maxWaitMs) {
            try {
                onView(withId(viewId)).check(matches(withText(text)));
                return;
            } catch (AssertionError e) {
                Thread.sleep(400);
                waited += 400;
            }
        }
        throw new AssertionError("Timed out waiting for button text: " + text);
    }

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        Context ctx = ApplicationProvider.getApplicationContext();
        deviceId = new DeviceAuthManager().getDeviceId(ctx);
        EventDetailsTestFixtures.seedAllStandardFixtures();
        EventDetailsTestFixtures.removeUserFromEventWaitingList(
                EventDetailsTestFixtures.OPEN_EVENT_ID, deviceId);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (deviceId != null) {
            EventDetailsTestFixtures.removeUserFromEventWaitingList(
                    EventDetailsTestFixtures.OPEN_EVENT_ID, deviceId);
        }
    }

    @Test
    public void testEventInfo_IsDisplayed() {
        try (ActivityScenario<EventDetailsActivity> s =
                     ActivityScenario.launch(buildIntent(EventDetailsTestFixtures.OPEN_EVENT_ID))) {
            onView(withId(R.id.eventTitleText)).check(matches(isDisplayed()));
            onView(withId(R.id.aboutEventText)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.leaveWaitlistButton)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testJoinButton_EnabledWhenRegistrationOpen() throws InterruptedException {
        try (ActivityScenario<EventDetailsActivity> s =
                     ActivityScenario.launch(buildIntent(EventDetailsTestFixtures.OPEN_EVENT_ID))) {

            waitForEnabled(R.id.leaveWaitlistButton, 20000);

            onView(withId(R.id.leaveWaitlistButton))
                    .check(matches(isEnabled()))
                    .check(matches(withText("JOIN WAITING LIST")));
        }
    }

    @Test
    public void testJoinButton_DisabledWhenRegistrationClosed() throws InterruptedException {
        try (ActivityScenario<EventDetailsActivity> s =
                     ActivityScenario.launch(buildIntent(EventDetailsTestFixtures.CLOSED_EVENT_ID))) {

            waitForDisabled(R.id.leaveWaitlistButton, 20000);

            onView(withId(R.id.leaveWaitlistButton))
                    .check(matches(not(isEnabled())))
                    .check(matches(withText("REGISTRATION CLOSED")));
        }
    }

    /**
     * After a successful join the app enables the button with label "LEAVE WAITING LIST"
     * (see {@code EventDetailsActivity#writeJoinToFirestore}).
     */
    @Test
    public void testJoinButton_afterJoin_showsLeaveWaitlist() throws InterruptedException {
        try (ActivityScenario<EventDetailsActivity> s =
                     ActivityScenario.launch(buildIntent(EventDetailsTestFixtures.OPEN_EVENT_ID))) {

            waitForEnabled(R.id.leaveWaitlistButton, 20000);

            onView(withId(R.id.leaveWaitlistButton)).perform(scrollTo(), forceClick());

            waitForButtonText(R.id.leaveWaitlistButton, "LEAVE WAITING LIST", 15000);
            onView(withId(R.id.leaveWaitlistButton)).check(matches(isEnabled()));
        }
    }
}
