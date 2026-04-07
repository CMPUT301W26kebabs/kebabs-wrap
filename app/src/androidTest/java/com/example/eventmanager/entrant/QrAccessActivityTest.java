package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.R;
import com.example.eventmanager.ui.QrAccessActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Entrant — US 01.06.01 (QR onboarding toward event details). */
@RunWith(AndroidJUnit4.class)
public class QrAccessActivityTest {

    @Rule
    public ActivityScenarioRule<QrAccessActivity> rule =
            new ActivityScenarioRule<>(QrAccessActivity.class);

    @Test
    public void qrScreen_coreElementsAreDisplayed() {
        onView(withText(R.string.qr_message)).check(matches(isDisplayed()));
        onView(withId(R.id.qrNext)).check(matches(isDisplayed()));
        onView(withId(R.id.qrIllustration)).check(matches(isDisplayed()));
    }
}
