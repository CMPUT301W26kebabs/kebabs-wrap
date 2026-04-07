package com.example.eventmanager.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke UI tests for {@link AdminNotificationLogsActivity} (US 03.08.01).
 */
@RunWith(AndroidJUnit4.class)
public class AdminNotificationLogsUITest {

    @Rule
    public ActivityScenarioRule<AdminNotificationLogsActivity> rule =
            new ActivityScenarioRule<>(AdminNotificationLogsActivity.class);

    @Test
    public void notificationLogs_searchAndListVisible() {
        onView(withId(R.id.et_search)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_notification_logs)).check(matches(isDisplayed()));
    }

    @Test
    public void notificationLogs_loadMoreButtonVisible() {
        onView(withId(R.id.btn_load_more)).check(matches(isDisplayed()));
        onView(withText("Load Previous 50 Notifications")).check(matches(isDisplayed()));
    }
}
