package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.ProfileActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Entrant — US 01.02.01 / 01.02.02 (view profile, open editor).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProfileUITest {

    @Rule
    public ActivityScenarioRule<ProfileActivity> profileRule =
            new ActivityScenarioRule<>(ProfileActivity.class);

    @Test
    public void profileScreen_coreElementsDisplayed() {
        onView(withId(R.id.tv_profile_name)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_edit_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
    }

    @Test
    public void profile_editButton_opensEditProfile() {
        onView(withId(R.id.btn_edit_profile)).perform(click());
        onView(withId(R.id.nameInput)).check(matches(isDisplayed()));
        onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
    }
}
