package com.example.eventmanager.entrant;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.ui.EntrantSignUpActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/**
 * Entrant — US 01.02.01 (name & email on sign-up; layout has no password field).
 *
 * <p>Clears {@code users/{deviceId}} before launch so {@link EntrantSignUpActivity} does not
 * redirect existing users away from this screen.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantSignUpActivityTest {

    private ActivityScenario<EntrantSignUpActivity> scenario;

    @Before
    public void launchFreshSignUp() throws ExecutionException, InterruptedException {
        Context ctx = ApplicationProvider.getApplicationContext();
        String deviceId = new DeviceAuthManager().getDeviceId(ctx);
        Tasks.await(FirebaseFirestore.getInstance()
                .collection("users")
                .document(deviceId)
                .delete());
        scenario = ActivityScenario.launch(EntrantSignUpActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void signUpScreen_coreElementsAreDisplayed() {
        onView(withId(R.id.signUpTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.nameInput)).check(matches(isDisplayed()));
        onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
        onView(withId(R.id.signUpButton)).check(matches(isDisplayed()));
        onView(withId(R.id.loginLink)).check(matches(isDisplayed()));
    }

    @Test
    public void signUpButton_emptyName_showsNameValidationError() {
        onView(withId(R.id.nameInput)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.emailInput))
                .perform(clearText(), replaceText("a@b.com"), closeSoftKeyboard());

        onView(withId(R.id.signUpButton)).perform(click());

        scenario.onActivity(activity -> {
            TextInputLayout nameLayout = activity.findViewById(R.id.nameInputLayout);
            assertNotNull(nameLayout.getError());
        });
    }

    @Test
    public void signUpButton_invalidEmail_showsEmailValidationError() {
        onView(withId(R.id.nameInput))
                .perform(clearText(), replaceText("Fahad"), closeSoftKeyboard());

        onView(withId(R.id.emailInput))
                .perform(clearText(), replaceText("not-an-email"), closeSoftKeyboard());

        onView(withId(R.id.signUpButton)).perform(click());

        scenario.onActivity(activity -> {
            TextInputLayout emailLayout = activity.findViewById(R.id.emailInputLayout);
            assertNotNull(emailLayout.getError());
        });
    }
}
