package com.example.eventmanager;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class CreateEventUITest {

    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

    @Test
    public void testCreateEventAndGenerateQR() {
        // 1. Type in the event details
        onView(withId(R.id.edit_event_name))
                .perform(typeText("Winter Hackathon"), closeSoftKeyboard());
        onView(withId(R.id.edit_capacity))
                .perform(typeText("50"), closeSoftKeyboard());

        // 2. Click the generate button
        onView(withId(R.id.btn_create_generate)).perform(click());

        // Note: In a full test, you would use Intents to verify EventQRActivity launched successfully!
    }
}