package com.example.eventmanager.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminUITest {

    @Rule
    public ActivityScenarioRule<AdminEventsActivity> eventsRule =
            new ActivityScenarioRule<>(AdminEventsActivity.class);

    @Test public void browseEvents_screenLoads() { onView(withId(R.id.et_search)).check(matches(isDisplayed())); }
    @Test public void browseEvents_searchBarAcceptsInput() { onView(withId(R.id.et_search)).perform(typeText("test"), closeSoftKeyboard()); }
    @Test public void browseEvents_activeChipDisplayed() { onView(withId(R.id.chip_active)).check(matches(isDisplayed())); }
    @Test public void browseEvents_deletedChipDisplayed() { onView(withId(R.id.chip_deleted)).check(matches(isDisplayed())); }
    @Test public void browseEvents_allChipDisplayed() { onView(withId(R.id.chip_all)).check(matches(isDisplayed())); }
    @Test public void browseEvents_chipsClickable() { onView(withId(R.id.chip_deleted)).perform(click()); onView(withId(R.id.chip_active)).perform(click()); }
    @Test public void browseEvents_backButtonExists() { onView(withId(R.id.btn_back)).check(matches(isDisplayed())); }
    @Test public void browseEvents_backButtonWorks() { onView(withId(R.id.btn_back)).perform(click()); }
}
