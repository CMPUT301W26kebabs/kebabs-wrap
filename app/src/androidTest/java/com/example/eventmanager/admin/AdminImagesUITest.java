package com.example.eventmanager.admin;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmanager.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Admin — US 03.03.01 / 03.06.01 (browse uploaded images).
 *
 * <p>{@code rv_images} is {@code GONE} while loading and when there are no posters
 * ({@code empty_state} is shown instead); see {@link AdminImagesActivity#loadImages()}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminImagesUITest {

    @Rule
    public ActivityScenarioRule<AdminImagesActivity> rule =
            new ActivityScenarioRule<>(AdminImagesActivity.class);

    @Test
    public void browseImages_headerAndContentStateVisible() {
        onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_menu)).check(matches(isDisplayed()));
        onView(anyOf(
                allOf(withId(R.id.rv_images), isDisplayed()),
                allOf(withId(R.id.empty_state), isDisplayed()),
                allOf(withId(R.id.progress_bar), isDisplayed())
        )).check(matches(isDisplayed()));
    }
}
