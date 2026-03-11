package com.example.eventmanager;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.ui.BrowseImagesActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BrowseImagesActivityTest {

    @Rule
    public ActivityScenarioRule<BrowseImagesActivity> rule =
            new ActivityScenarioRule<>(BrowseImagesActivity.class);

    @Test
    public void recyclerView_isDisplayed() {
        onView(withId(R.id.imagesRecyclerView)).check(matches(isDisplayed()));
    }

    @Test
    public void recyclerView_usesGridLayoutManager() {
        rule.getScenario().onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.imagesRecyclerView);
            assertNotNull(recyclerView.getLayoutManager());
            assertTrue(recyclerView.getLayoutManager() instanceof GridLayoutManager);
        });
    }
}
