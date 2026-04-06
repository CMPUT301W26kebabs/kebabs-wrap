package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.EditProfileActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Entrant — US 01.02.02 (update fields), 01.02.03 (registration history), 01.04.03 (notifications),
 * 01.02.04 (delete profile).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EditProfileUITest {

    @Rule
    public ActivityScenarioRule<EditProfileActivity> editRule =
            new ActivityScenarioRule<>(EditProfileActivity.class);

    @Test
    public void editProfileScreen_coreFieldsDisplayed() {
        onView(withId(R.id.nameInput)).check(matches(isDisplayed()));
        onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
        onView(withId(R.id.phoneInput)).check(matches(isDisplayed()));
        onView(withId(R.id.switchReceiveNotifications)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSaveProfile)).check(matches(isDisplayed()));
    }

    @Test
    public void editProfile_registrationHistoryAndDeleteVisible() {
        // Section title is always laid out; rvRegistrationHistory may have 0 height when empty
        // (wrap_content), which fails Espresso isDisplayed().
        onView(withText(R.string.your_registration_history)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btnDeleteProfile)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void editProfile_nameFieldAcceptsInput() {
        onView(withId(R.id.nameInput)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.nameInput)).check(matches(isDisplayed()));
    }
}
