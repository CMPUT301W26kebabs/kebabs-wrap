package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.not;
/**
 * Espresso UI tests for InvitationActivity.
 * Tests US4: Accept Invitation.
 *
 * NOTE: These tests use a real Firestore connection.
 * Ensure your Firestore has a document with id "test_event_open"
 * and the current device's ID is in the chosenList array.
 */
@RunWith(AndroidJUnit4.class)
public class InvitationActivityTest_DISABLED {

    // ---------------------------------------------------------------
    // US4: Invitation screen layout
    // ---------------------------------------------------------------

    /**
     * The event name should be visible on the invitation screen.
     */
    @Test
    public void testEventName_IsDisplayed() {
        try (ActivityScenario<InvitationActivity> scenario =
                     ActivityScenario.launch(InvitationActivity.class)) {

            onView(withId(R.id.tvInvitationEventName))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * The Accept button should be visible and enabled on launch.
     */
    @Test
    public void testAcceptButton_IsDisplayed() {
        try (ActivityScenario<InvitationActivity> scenario =
                     ActivityScenario.launch(InvitationActivity.class)) {

            onView(withId(R.id.btnAcceptInvitation))
                    .check(matches(isDisplayed()))
                    .check(matches(isEnabled()));
        }
    }

    /**
     * The Decline button should be visible and enabled on launch.
     */
    @Test
    public void testDeclineButton_IsDisplayed() {
        try (ActivityScenario<InvitationActivity> scenario =
                     ActivityScenario.launch(InvitationActivity.class)) {

            onView(withId(R.id.btnDeclineInvitation))
                    .check(matches(isDisplayed()))
                    .check(matches(isEnabled()));
        }
    }

    // ---------------------------------------------------------------
    // US4: Accept flow
    // ---------------------------------------------------------------

    /**
     * Tapping Accept should show the enrollment confirmation message.
     * Requires the device to be in the event's chosenList in Firestore.
     */
    @Test
    public void testAcceptButton_ShowsConfirmationMessage() {
        try (ActivityScenario<InvitationActivity> scenario =
                     ActivityScenario.launch(InvitationActivity.class)) {

            onView(withId(R.id.btnAcceptInvitation)).perform(click());

            onView(withId(R.id.tvInvitationStatus))
                    .check(matches(isDisplayed()))
                    .check(matches(withText("You're enrolled! See you there.")));
        }
    }

    /**
     * Tapping Accept should disable both buttons to prevent double submission.
     */
    @Test
    public void testAcceptButton_DisablesButtonsAfterTap() {
        try (ActivityScenario<InvitationActivity> scenario =
                     ActivityScenario.launch(InvitationActivity.class)) {

            onView(withId(R.id.btnAcceptInvitation)).perform(click());

            onView(withId(R.id.btnAcceptInvitation))
                    .check(matches(not(isEnabled())));

            onView(withId(R.id.btnDeclineInvitation))
                    .check(matches(not(isEnabled())));
        }
    }

    // ---------------------------------------------------------------
    // US4: Decline flow
    // ---------------------------------------------------------------

    /**
     * Tapping Decline should show a decline confirmation message.
     */
    @Test
    public void testDeclineButton_ShowsDeclineMessage() {
        try (ActivityScenario<InvitationActivity> scenario =
                     ActivityScenario.launch(InvitationActivity.class)) {

            onView(withId(R.id.btnDeclineInvitation)).perform(click());

            onView(withId(R.id.tvInvitationStatus))
                    .check(matches(isDisplayed()))
                    .check(matches(withText("You've declined the invitation.")));
        }
    }

    /**
     * The status message should be hidden before any button is tapped.
     */
    @Test
    public void testStatusMessage_HiddenOnLaunch() {
        try (ActivityScenario<InvitationActivity> scenario =
                     ActivityScenario.launch(InvitationActivity.class)) {

            onView(withId(R.id.tvInvitationStatus))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }
}