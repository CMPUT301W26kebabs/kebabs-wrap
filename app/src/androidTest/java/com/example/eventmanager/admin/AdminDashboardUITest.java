package com.example.eventmanager.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDashboardUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> mainRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test public void dashboard_eventCardDisplayed() { onView(withId(R.id.card_events)).check(matches(isDisplayed())); }
    @Test public void dashboard_profileCardDisplayed() { onView(withId(R.id.card_profiles)).check(matches(isDisplayed())); }
    @Test public void dashboard_imageCardDisplayed() { onView(withId(R.id.card_images)).check(matches(isDisplayed())); }
    @Test public void dashboard_commentsCardDisplayed() { onView(withId(R.id.card_comments)).check(matches(isDisplayed())); }
    @Test public void dashboard_notificationLogsCardDisplayed() { onView(withId(R.id.card_notification_logs)).check(matches(isDisplayed())); }
    @Test public void dashboard_backButtonExists() { onView(withId(R.id.btn_back_home)).check(matches(isDisplayed())); }
    /**
     * Asserts {@link AdminEventsActivity} opened. {@code rv_events} is GONE while loading, on load
     * errors, and when the filtered list is empty ({@code empty_state} is shown instead).
     */
    @Test
    public void dashboard_eventCardNavigates() {
        onView(withId(R.id.card_events)).perform(click());
        onView(withId(R.id.et_search)).check(matches(isDisplayed()));
        onView(withId(R.id.chip_active)).check(matches(isDisplayed()));
    }
}
