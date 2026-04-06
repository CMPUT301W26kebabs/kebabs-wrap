package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.NotificationsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Entrant — US 01.04.x (notification inbox UI).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationsUITest {

    @Rule
    public ActivityScenarioRule<NotificationsActivity> rule =
            new ActivityScenarioRule<>(NotificationsActivity.class);

    @Test
    public void notificationsScreen_headerAndBackDisplayed() {
        onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
        onView(withText("Notifications")).check(matches(isDisplayed()));
    }
}
