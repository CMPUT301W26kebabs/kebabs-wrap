package com.example.eventmanager.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.ManageEventActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Organizer — US 02.02.01 / 02.06.x (entrant tabs, list chrome). Uses a placeholder event id;
 * lists may be empty if the document does not exist in Firestore.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ManageEventUITest {

    static Intent intent;
    static {
        intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "mock_test_event_id");
        intent.putExtra("EVENT_NAME", "Mock Event");
    }

    @Rule
    public ActivityScenarioRule<ManageEventActivity> rule =
            new ActivityScenarioRule<>(intent);

    @Test
    public void manageEvent_tabsAndListChromeDisplayed() {
        onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
        onView(withId(R.id.text_event_title)).check(matches(isDisplayed()));
        onView(withId(R.id.tab_waiting)).check(matches(isDisplayed()));
        onView(withId(R.id.tab_chosen)).check(matches(isDisplayed()));
        onView(withId(R.id.recycler_chosen_entrants)).check(matches(isDisplayed()));
    }
}
