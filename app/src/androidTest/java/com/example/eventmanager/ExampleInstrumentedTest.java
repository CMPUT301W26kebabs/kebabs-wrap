package com.example.eventmanager;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.eventmanager", appContext.getPackageName());
    }
}

@RunWith(AndroidJUnit4.class)
public class CreateEventUITest {
    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

    @Test
    public void testCreateEventFlow() {
        onView(withId(R.id.edit_event_name)).perform(typeText("Winter Gala"));
        onView(withId(R.id.edit_capacity)).perform(typeText("100"), closeSoftKeyboard());
        onView(withId(R.id.btn_save_event)).perform(click());

        // Check if QR code image is now populated
        onView(withId(R.id.image_qr_preview)).check(matches(isDisplayed()));
    }
}

@RunWith(AndroidJUnit4.class)
public class ManagePosterActivityTest {

    @Rule
    public ActivityScenarioRule<ManagePosterActivity> activityRule =
            new ActivityScenarioRule<>(ManagePosterActivity.class);

    @Test
    public void testSelectImageButtonLaunchesGallery() {
        // Initialize Intents to capture the outgoing intent
        Intents.init();

        // Click the select image button
        onView(withId(R.id.btn_select_image)).perform(click());

        // Verify an intent was sent to pick an image from external storage
        intended(allOf(
                hasAction(Intent.ACTION_PICK),
                hasData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        ));

        Intents.release();
    }
}