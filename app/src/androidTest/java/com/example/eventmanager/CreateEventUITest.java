package com.example.eventmanager;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.times;

@RunWith(AndroidJUnit4.class)
public class CreateEventUITest {

    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ==========================================
    // EASY TEST (The Happy Path)
    // ==========================================
    @Test
    public void test01_CreateEvent_AllInputsValid_Success() {
        // User types everything perfectly
        onView(withId(R.id.edit_event_name)).perform(typeText("Winter Hackathon"), closeSoftKeyboard());
        onView(withId(R.id.edit_capacity)).perform(typeText("50"), closeSoftKeyboard());

        onView(withId(R.id.btn_create_generate)).perform(click());

        // Verify it successfully navigated to the QR screen
        intended(IntentMatchers.hasComponent(EventQRActivity.class.getName()));
    }

    // ==========================================
    // HARD TEST 1 (The Sad Path: Missing Name)
    // ==========================================
    @Test
    public void test02_CreateEvent_EmptyName_FailsGracefully() {
        // User forgets to type a name, only types capacity
        onView(withId(R.id.edit_capacity)).perform(typeText("100"), closeSoftKeyboard());

        onView(withId(R.id.btn_create_generate)).perform(click());

        // Verify the app DID NOT navigate to the next screen (times(0) means zero intents fired)
        intended(IntentMatchers.hasComponent(EventQRActivity.class.getName()), times(0));

        // Verify we are still safely on the Create Event screen
        onView(withId(R.id.btn_create_generate)).check(matches(isDisplayed()));
    }

    // ==========================================
    // HARD TEST 2 (The Sad Path: Missing Capacity)
    // ==========================================
    @Test
    public void test03_CreateEvent_EmptyCapacity_FailsGracefully() {
        // User types a name but forgets the capacity
        onView(withId(R.id.edit_event_name)).perform(typeText("Spring Gala"), closeSoftKeyboard());

        onView(withId(R.id.btn_create_generate)).perform(click());

        // Verify the app DID NOT navigate to the next screen
        intended(IntentMatchers.hasComponent(EventQRActivity.class.getName()), times(0));
    }
}