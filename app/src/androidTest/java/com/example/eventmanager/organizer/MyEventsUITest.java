package com.example.eventmanager.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.MyEventsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Organizer — US 02.01.01 (organizer event list, create flow entry). */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyEventsUITest {

    @Rule
    public ActivityScenarioRule<MyEventsActivity> myEventsRule =
            new ActivityScenarioRule<>(MyEventsActivity.class);

    @Test public void myEvents_screenLoads() { onView(withId(R.id.recycler_my_events)).check(matches(isDisplayed())); }
    @Test public void myEvents_fabDisplayed() { onView(withId(R.id.fab_add_event)).check(matches(isDisplayed())); }
    @Test public void myEvents_backButtonDisplayed() { onView(withId(R.id.btn_back_my_events)).check(matches(isDisplayed())); }
    @Test public void myEvents_backButtonWorks() { onView(withId(R.id.btn_back_my_events)).perform(click()); }
    @Test public void myEvents_fabNavigatesToCreateEvent() { onView(withId(R.id.fab_add_event)).perform(click()); onView(withId(R.id.editEventName)).check(matches(isDisplayed())); }
}
