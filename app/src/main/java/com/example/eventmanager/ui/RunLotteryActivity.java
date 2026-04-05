package com.example.eventmanager.ui;

import com.example.eventmanager.R;

import com.example.eventmanager.managers.LotteryManager;
import com.example.eventmanager.managers.OrganizerNotificationManager;
import com.example.eventmanager.callbacks.LotteryCallback;
import com.example.eventmanager.adapter.WinnerAdapter;
import com.example.eventmanager.models.Event;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.WinnerAdapter;
import com.example.eventmanager.models.Entrant;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RunLotteryActivity
 *
 * Organizer UI screen for:
 *   US 02.05.02 – Sample a specified number of attendees from the waiting list.
 *   US 02.05.03 – Draw a replacement if someone cancels/declines.
 *   Cancelled entrants are managed from Manage Event.
 *
 * Delegates all business logic to the existing {@link LotteryManager}.
 * Receives eventId + capacity via Intent extras.
 */
public class RunLotteryActivity extends AppCompatActivity {

    // ── Intent extras ─────────────────────────────────────────────────────────
    public static final String EXTRA_EVENT_ID  = "extra_event_id";
    public static final String EXTRA_CAPACITY  = "extra_capacity";
    public static final String EXTRA_EVENT_NAME = "extra_event_name";

    // ── UI ────────────────────────────────────────────────────────────────────
    private ImageButton             btnBack;
    private MaterialButton          btnIncrement, btnDecrement;
    private MaterialButton          btnDrawWinners, btnDrawReplacement, btnSeedWaitlist;
    private TextView                tvWinnerCount, tvWaitlistCount;
    private TextView                tvStatusMessage, tvResultCount;
    private LinearProgressIndicator progressBar;
    private CardView                cardResults;
    private RecyclerView            rvWinners;
    private RecyclerView            rvCancelled;
    private TextView                tvCancelledCount;

    // ── State ─────────────────────────────────────────────────────────────────
    private String        eventId;
    private String        eventName;
    private int           capacity     = Integer.MAX_VALUE;
    private int           winnerCount  = 1;
    private int           waitlistSize = 0;

    // ── Collaborators ─────────────────────────────────────────────────────────
    private LotteryManager                  lotteryManager;
    private WinnerAdapter                   winnerAdapter;
    private final List<Entrant>             winnerList = new ArrayList<>();
    private OrganizerNotificationManager    organizerNotificationManager;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_lottery);

        // Accept both the normalized extras and the legacy keys used elsewhere in the app.
        eventId  = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            eventId = getIntent().getStringExtra("EVENT_ID");
        }
        capacity = getIntent().getIntExtra(EXTRA_CAPACITY, Integer.MAX_VALUE);
        eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);
        if (eventName == null) {
            eventName = getIntent().getStringExtra("EVENT_NAME");
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event information for lottery.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        lotteryManager = new LotteryManager();
        organizerNotificationManager = new OrganizerNotificationManager();

        bindViews();
        setupRecyclerView();
        setupClickListeners();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });
        loadWaitlistCount();
    }

    // ── Bind views ────────────────────────────────────────────────────────────
    private void bindViews() {
        btnBack            = findViewById(R.id.btnBack);
        btnIncrement       = findViewById(R.id.btnIncrement);
        btnDecrement       = findViewById(R.id.btnDecrement);
        btnDrawWinners     = findViewById(R.id.btnDrawWinners);
        btnDrawReplacement  = findViewById(R.id.btnDrawReplacement);
        btnSeedWaitlist    = findViewById(R.id.btnSeedWaitlist);
        tvWinnerCount      = findViewById(R.id.tvWinnerCount);
        tvWaitlistCount    = findViewById(R.id.tvWaitlistCount);
        tvStatusMessage    = findViewById(R.id.tvStatusMessage);
        tvResultCount      = findViewById(R.id.tvResultCount);
        progressBar        = findViewById(R.id.progressBarDraw);
        cardResults        = findViewById(R.id.cardResults);
        rvWinners          = findViewById(R.id.rvWinners);

        tvWinnerCount.setText(String.valueOf(winnerCount));
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        winnerAdapter = new WinnerAdapter(winnerList);
        rvWinners.setLayoutManager(new LinearLayoutManager(this));
        rvWinners.setAdapter(winnerAdapter);
        rvWinners.setNestedScrollingEnabled(false);
    }

    // ── Click listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navigateBack());

        // Decrement — floor at 1
        btnDecrement.setOnClickListener(v -> {
            if (winnerCount > 1) {
                winnerCount--;
                animateCounter(tvWinnerCount, String.valueOf(winnerCount));
                refreshDrawButton();
            }
        });

        // Increment — cap at min(waitlistSize, capacity)
        btnIncrement.setOnClickListener(v -> {
            int max = Math.min(waitlistSize, capacity);
            if (winnerCount < max) {
                winnerCount++;
                animateCounter(tvWinnerCount, String.valueOf(winnerCount));
                refreshDrawButton();
            } else {
                showStatus("Max is " + max + " (waitlist / capacity limit)", true);
            }
        });

        // US 02.05.02 – Draw N winners
        btnDrawWinners.setOnClickListener(v -> confirmDraw());

        // US 02.05.03 – Draw 1 replacement
        btnDrawReplacement.setOnClickListener(v -> confirmReplacement());

        // Debug: seed waitlist with test entrants (no extra devices needed)
        btnSeedWaitlist.setOnClickListener(v -> confirmSeedWaitlist());
    }

    /** Debug: add 10 test entrants to the current event's waiting list. */
    private void confirmSeedWaitlist() {
        new AlertDialog.Builder(this)
                .setTitle("Seed waitlist")
                .setMessage("Add 10 test users to the waiting list for this event? (For testing only.)")
                .setPositiveButton("Seed", (d, w) -> seedWaitlist())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void seedWaitlist() {
        btnSeedWaitlist.setEnabled(false);
        showStatus("Seeding waitlist…", false);

        CollectionReference waitlistRef = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("waitingList");

        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (int i = 1; i <= 10; i++) {
            String docId = "test_waitlist_" + i;
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", docId);
            data.put("name", "Test User " + i);
            data.put("email", "test" + i + "@test.com");
            data.put("joinedAt", FieldValue.serverTimestamp());
            batch.set(waitlistRef.document(docId), data);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    showStatus("✓ 10 test users added to waitlist.", false);
                    loadWaitlistCount();
                    btnSeedWaitlist.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    showStatus("Seed failed: " + e.getMessage(), true);
                    btnSeedWaitlist.setEnabled(true);
                });
    }

    // ── Load live waitlist count from Firestore ───────────────────────────────
    private void loadWaitlistCount() {
        FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snap -> {
                    waitlistSize = snap.size();
                    tvWaitlistCount.setText("from " + waitlistSize + " waiting entrants");
                    // Clamp current winnerCount to new size
                    winnerCount = Math.max(1, Math.min(winnerCount, waitlistSize));
                    tvWinnerCount.setText(String.valueOf(winnerCount));
                    refreshDrawButton();
                })
                .addOnFailureListener(e ->
                        showStatus("Could not load waitlist: " + e.getMessage(), true));
    }

    // ── US 02.05.02 ───────────────────────────────────────────────────────────
    private void confirmDraw() {
        if (waitlistSize == 0) {
            showStatus("Waiting list is empty — nothing to draw!", true);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Confirm Draw")
                .setMessage("Draw " + winnerCount + " winner(s) from "
                        + waitlistSize + " waiting entrants?\n\nThis cannot be undone.")
                .setPositiveButton("Draw", (d, w) -> executeDraw())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDraw() {
        setLoading(true);
        hideStatus();

        // Uses your existing LotteryManager.drawWinners() + LotteryCallback exactly
        lotteryManager.drawWinners(eventId, winnerCount, new LotteryCallback() {
            @Override
            public void onSuccess(List<Entrant> selectedWinners, List<String> winnerDeviceIds) {
                setLoading(false);

                winnerList.clear();
                winnerList.addAll(selectedWinners);
                winnerAdapter.notifyDataSetChanged();

                tvResultCount.setText(selectedWinners.size() + " drawn");
                cardResults.setVisibility(View.VISIBLE);
                animateCardIn(cardResults);

                showStatus("✓ " + selectedWinners.size() + " winner(s) selected!", false);
                loadWaitlistCount(); // Refresh count after draw

                String en = eventName != null ? eventName : "Event";
                organizerNotificationManager.notifyAfterLotteryDraw(eventId, en, winnerDeviceIds,
                        new OrganizerNotificationManager.NotificationDispatchCallback() {
                            @Override
                            public void onSuccess(int totalNotified) {
                                runOnUiThread(() -> Toast.makeText(RunLotteryActivity.this,
                                        "Notifications sent (winners + not chosen).",
                                        Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onFailure(@NonNull String message) {
                                runOnUiThread(() -> Toast.makeText(RunLotteryActivity.this,
                                        message, Toast.LENGTH_LONG).show());
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                showStatus("Draw failed: " + errorMessage, true);
            }
        });
    }

    // ── US 02.05.03 ───────────────────────────────────────────────────────────
    private void confirmReplacement() {
        new AlertDialog.Builder(this)
                .setTitle("Draw Replacement")
                .setMessage("Randomly select 1 replacement from the remaining waiting list?")
                .setPositiveButton("Draw", (d, w) -> executeReplacement())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeReplacement() {
        setLoading(true);
        hideStatus();

        // Uses your existing LotteryManager.drawReplacement() which calls drawWinners(eventId, 1, ...)
        lotteryManager.drawReplacement(eventId, new LotteryCallback() {
            @Override
            public void onSuccess(List<Entrant> selectedWinners, List<String> replacementWinnerIds) {
                setLoading(false);

                if (!selectedWinners.isEmpty()) {
                    Entrant replacement = selectedWinners.get(0);
                    showStatus("✓ Replacement drawn: "
                                    + (replacement.getName() != null ? replacement.getName() : replacement.getDeviceId()),
                            false);

                    // Append to results card if already visible
                    if (cardResults.getVisibility() == View.VISIBLE) {
                        winnerList.addAll(selectedWinners);
                        winnerAdapter.notifyItemInserted(winnerList.size() - 1);
                        tvResultCount.setText(winnerList.size() + " drawn");
                    } else {
                        winnerList.addAll(selectedWinners);
                        winnerAdapter.notifyDataSetChanged();
                        tvResultCount.setText(winnerList.size() + " drawn");
                        cardResults.setVisibility(View.VISIBLE);
                        animateCardIn(cardResults);
                    }
                    loadWaitlistCount();

                    String enRep = eventName != null ? eventName : "Event";
                    organizerNotificationManager.notifyAfterLotteryDraw(eventId, enRep, replacementWinnerIds,
                            new OrganizerNotificationManager.NotificationDispatchCallback() {
                                @Override
                                public void onSuccess(int totalNotified) {
                                    runOnUiThread(() -> Toast.makeText(RunLotteryActivity.this,
                                            "Notifications sent (winners + not chosen).",
                                            Toast.LENGTH_SHORT).show());
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    runOnUiThread(() -> Toast.makeText(RunLotteryActivity.this,
                                            message, Toast.LENGTH_LONG).show());
                                }
                            });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                showStatus("Replacement failed: " + errorMessage, true);
            }
        });
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    /** Enable/disable draw button based on waitlist availability. */
    private void refreshDrawButton() {
        boolean canDraw = waitlistSize > 0;
        btnDrawWinners.setEnabled(canDraw);
        btnDrawWinners.setAlpha(canDraw ? 1f : 0.45f);
        btnDrawReplacement.setEnabled(canDraw);
        btnDrawReplacement.setAlpha(canDraw ? 1f : 0.45f);
    }

    /** Show/hide progress bar and lock controls during async operation. */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnDrawWinners.setEnabled(!loading);
        btnDrawReplacement.setEnabled(!loading);
        btnIncrement.setEnabled(!loading);
        btnDecrement.setEnabled(!loading);
    }

    private void showStatus(String msg, boolean isError) {
        tvStatusMessage.setText(msg);
        tvStatusMessage.setTextColor(isError ? 0xFFE53935 : 0xFF43A047);
        tvStatusMessage.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        tvStatusMessage.setVisibility(View.GONE);
    }

    /** Bounce animation on the counter TextView when value changes. */
    private void animateCounter(TextView tv, String newValue) {
        tv.setText(newValue);
        ObjectAnimator sx = ObjectAnimator.ofFloat(tv, "scaleX", 1f, 1.35f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(tv, "scaleY", 1f, 1.35f, 1f);
        sx.setDuration(260);
        sy.setDuration(260);
        sx.setInterpolator(new OvershootInterpolator(2f));
        sy.setInterpolator(new OvershootInterpolator(2f));
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy);
        set.start();
    }

    /** Slide-up + fade-in entrance for the results card. */
    private void animateCardIn(View view) {
        view.setTranslationY(50f);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(380)
                .setInterpolator(new OvershootInterpolator(0.9f))
                .start();
    }

    private void navigateBack() {
        Intent intent = new Intent(this, ManageEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("EVENT_NAME", eventName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
