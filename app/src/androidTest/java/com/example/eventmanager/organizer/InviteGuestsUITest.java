package com.example.eventmanager.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.InviteGuestsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Organizer — US 02.01.03 (invite entrants to private event waiting list).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class InviteGuestsUITest {

    static Intent intent;
    static {
        intent = new Intent(ApplicationProvider.getApplicationContext(), InviteGuestsActivity.class);
        intent.putExtra("EVENT_ID", "mock_test_event_id");
    }

    @Rule
    public ActivityScenarioRule<InviteGuestsActivity> activityRule =
            new ActivityScenarioRule<>(intent);

    @Test
    public void testActivityLaunchesWithValidIntent() {
        onView(withId(R.id.inviteGuestsToolbar)).check(matches(isDisplayed()));
        onView(withId(R.id.searchGuestInput)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterButtonsArePresentAndClickable() {
        onView(withId(R.id.nameFilterButton)).check(matches(isDisplayed()));
        onView(withId(R.id.emailFilterButton)).check(matches(isDisplayed()));
        onView(withId(R.id.phoneFilterButton)).check(matches(isDisplayed()));

        onView(withId(R.id.emailFilterButton)).perform(click());
        onView(withId(R.id.phoneFilterButton)).perform(click());
        onView(withId(R.id.nameFilterButton)).perform(click());
    }

    @Test
    public void testSearchInputAcceptsText() {
        onView(withId(R.id.searchGuestInput))
                .perform(typeText("test search"), closeSoftKeyboard())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testRecyclerViewIsPresent() {
        onView(withId(R.id.guestsRecyclerView)).check(matches(isDisplayed()));
    }
}
