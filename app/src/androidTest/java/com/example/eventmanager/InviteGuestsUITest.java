package com.example.eventmanager;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class InviteGuestsUITest {

    // Provide a mocked intent to launch properly
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
        // Assert the toolbar and main input area are present
        onView(withId(R.id.inviteGuestsToolbar)).check(matches(isDisplayed()));
        onView(withId(R.id.searchGuestInput)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterButtonsArePresentAndClickable() {
        // Assert the filter toggles exist
        onView(withId(R.id.nameFilterButton)).check(matches(isDisplayed()));
        onView(withId(R.id.emailFilterButton)).check(matches(isDisplayed()));
        onView(withId(R.id.phoneFilterButton)).check(matches(isDisplayed()));

        // Assert they can be clicked without crashing
        onView(withId(R.id.emailFilterButton)).perform(click());
        onView(withId(R.id.phoneFilterButton)).perform(click());
        onView(withId(R.id.nameFilterButton)).perform(click());
    }

    @Test
    public void testSearchInputAcceptsText() {
        // Assert user can type in the search bar safely
        onView(withId(R.id.searchGuestInput))
                .perform(typeText("test search"), closeSoftKeyboard())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testRecyclerViewIsPresent() {
        // Assert the guest list recycler view is available in the layout
        onView(withId(R.id.guestsRecyclerView)).check(matches(isDisplayed()));
    }
}
