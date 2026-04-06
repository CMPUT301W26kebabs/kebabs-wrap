package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.EventDetailsActivity;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/**
 * Entrant — US 01.05.05 (lottery guidelines), event details layout smoke.
 *
 * <p>Requires {@link EventDetailsTestFixtures#MOCK_EVENT_ID} in Firestore (seeded in {@link #seedFirestore()}).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsUITest {

    @Rule
    public ActivityScenarioRule<EventDetailsActivity> activityRule =
            new ActivityScenarioRule<>(buildIntent());

    private static Intent buildIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsActivity.class);
        intent.putExtra("EVENT_ID", EventDetailsTestFixtures.MOCK_EVENT_ID);
        return intent;
    }

    @BeforeClass
    public static void seedFirestore() throws ExecutionException, InterruptedException {
        EventDetailsTestFixtures.seedAllStandardFixtures();
    }

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
