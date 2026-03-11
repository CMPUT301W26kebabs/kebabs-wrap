package com.example.eventmanager;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.ui.EventDetailsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EventDetailsActivityTest {

    @Test
    public void missingEventId_finishesActivity() {
        try (ActivityScenario<EventDetailsActivity> scenario = ActivityScenario.launch(
                new Intent(ApplicationProvider.getApplicationContext(), EventDetailsActivity.class))) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void emptyEventId_finishesActivity() {
        try (ActivityScenario<EventDetailsActivity> scenario = ActivityScenario.launch(
                new Intent(ApplicationProvider.getApplicationContext(), EventDetailsActivity.class)
                        .putExtra(EventDetailsActivity.EXTRA_EVENT_ID, ""))) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }
}
