package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.ui.ProfileActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    @Rule
    public ActivityScenarioRule<ProfileActivity> rule =
            new ActivityScenarioRule<>(ProfileActivity.class);

    @Test
    public void canEnterNameAndPressSave() {
        onView(withId(R.id.nameInput)).perform(typeText("Fahad"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());
        // Basic smoke test: no crash.
        // Later: assert toast or navigation once you add a deterministic behavior.
    }
}