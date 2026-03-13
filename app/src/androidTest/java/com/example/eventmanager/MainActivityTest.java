package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for MainActivity.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Verifies that the core UI elements appear on the polished sign up screen.
     */
    @Test
    public void signUpScreen_coreElementsAreDisplayed() {
        onView(withId(R.id.signUpTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.nameInput)).check(matches(isDisplayed()));
        onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
        onView(withId(R.id.passwordInput)).check(matches(isDisplayed()));
        onView(withId(R.id.signUpButton)).check(matches(isDisplayed()));
        onView(withId(R.id.loginLink)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that a short password triggers validation error.
     */
    @Test
    public void signUpButton_shortPassword_showsPasswordValidationError() {
        onView(withId(R.id.nameInput))
                .perform(clearText(), replaceText("Fahad"), closeSoftKeyboard());

        onView(withId(R.id.emailInput))
                .perform(clearText(), replaceText("fahad@example.com"), closeSoftKeyboard());

        onView(withId(R.id.passwordInput))
                .perform(clearText(), replaceText("123"), closeSoftKeyboard());

        onView(withId(R.id.signUpButton)).perform(click());

        rule.getScenario().onActivity(activity -> {
            TextInputLayout passwordLayout = activity.findViewById(R.id.passwordInputLayout);
            assertNotNull(passwordLayout.getError());
        });
    }

    /**
     * Verifies that an invalid email triggers validation error.
     */
    @Test
    public void signUpButton_invalidEmail_showsEmailValidationError() {
        onView(withId(R.id.nameInput))
                .perform(clearText(), replaceText("Fahad"), closeSoftKeyboard());

        onView(withId(R.id.emailInput))
                .perform(clearText(), replaceText("not-an-email"), closeSoftKeyboard());

        onView(withId(R.id.passwordInput))
                .perform(clearText(), replaceText("Kebabs123$"), closeSoftKeyboard());

        onView(withId(R.id.signUpButton)).perform(click());

        rule.getScenario().onActivity(activity -> {
            TextInputLayout emailLayout = activity.findViewById(R.id.emailInputLayout);
            assertNotNull(emailLayout.getError());
        });
    }
}