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
public class AdminProfilesUITest {

    @Rule
    public ActivityScenarioRule<AdminProfilesActivity> profilesRule =
            new ActivityScenarioRule<>(AdminProfilesActivity.class);

    @Test public void browseProfiles_screenLoads() { onView(withId(R.id.et_search)).check(matches(isDisplayed())); }
    @Test public void browseProfiles_searchBarAcceptsInput() { onView(withId(R.id.et_search)).perform(typeText("alice"), closeSoftKeyboard()); }
    @Test public void browseProfiles_backButtonExists() { onView(withId(R.id.btn_back)).check(matches(isDisplayed())); }
    @Test public void browseProfiles_backButtonWorks() { onView(withId(R.id.btn_back)).perform(click()); }
}
