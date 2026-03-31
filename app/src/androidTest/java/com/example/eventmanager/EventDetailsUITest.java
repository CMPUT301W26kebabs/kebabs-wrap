package com.example.eventmanager;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsUITest {

    static Intent intent;
    static {
        intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsActivity.class);
        intent.putExtra("EVENT_ID", "mock_test_event_id");
    }

    @Rule
    public ActivityScenarioRule<EventDetailsActivity> activityRule =
            new ActivityScenarioRule<>(intent);

    @Test
    public void testActivityLaunchesAndImportantUIElementsPresent() {
        onView(withId(R.id.eventTitleText)).check(matches(isDisplayed()));
        onView(withId(R.id.eventDateText)).check(matches(isDisplayed()));
        onView(withId(R.id.eventLocationText)).check(matches(isDisplayed()));

        onView(withId(R.id.tvLotteryGuidelines))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        onView(withId(R.id.leaveWaitlistButton))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }
}