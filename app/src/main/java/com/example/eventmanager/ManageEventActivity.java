package com.example.eventmanager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.repository.FollowRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ManageEventActivity extends AppCompatActivity {

    private String eventId;
    private String eventName;
    private TextView textEventTitle;
    private TextView textListHeader;
    private TextView badgeTotal;
    private TextView textEmptyList;
    private TextView tabWaiting;
    private TextView tabChosen;
    private TextView tabInvitees;
    private TextView tabEnrolled;
    private TextView tabCancelled;
    private RecyclerView recyclerChosenEntrants;
    private MaterialButton btnRunLottery;
    private MaterialButton btnNotify;
    private MaterialButton btnNotifyCancelled;
    private MaterialButton btnExportCsv;
    private MaterialButton btnCancelNonSignup;
    private FirebaseFirestore db;
    private OrganizerNotificationManager organizerNotificationManager;
    private EventRepository eventRepository;
    private String currentTab = "waiting";
    private int eventCapacity = Integer.MAX_VALUE;
    private boolean previewWaitingMode;
    private int waitingCount;
    private int chosenCount;
    private int inviteeCount;
    private int enrolledCount;
    private int cancelledCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

        db = FirebaseFirestore.getInstance();
        organizerNotificationManager = new OrganizerNotificationManager();
        eventRepository = new EventRepository();
        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event information.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        textEventTitle = findViewById(R.id.text_event_title);
        textListHeader = findViewById(R.id.text_list_header);
        badgeTotal = findViewById(R.id.badge_total);
        textEmptyList = findViewById(R.id.text_empty_list);
        tabWaiting = findViewById(R.id.tab_waiting);
        tabChosen = findViewById(R.id.tab_chosen);
        tabInvitees = findViewById(R.id.tab_invitees);
        tabEnrolled = findViewById(R.id.tab_enrolled);
        tabCancelled = findViewById(R.id.tab_cancelled);

        textEventTitle.setText(eventName != null ? eventName : "Event");

        recyclerChosenEntrants = findViewById(R.id.recycler_chosen_entrants);
        recyclerChosenEntrants.setLayoutManager(new LinearLayoutManager(this));
        recyclerChosenEntrants.setHasFixedSize(false);

        btnRunLottery = findViewById(R.id.btn_run_lottery);
        btnRunLottery.setOnClickListener(v -> {
            Intent intent = new Intent(this, RunLotteryActivity.class);
            intent.putExtra(RunLotteryActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(RunLotteryActivity.EXTRA_EVENT_NAME, eventName);
            intent.putExtra(RunLotteryActivity.EXTRA_CAPACITY, eventCapacity);
            startActivity(intent);
        });

        btnNotify = findViewById(R.id.btn_notify);
        btnNotify.setOnClickListener(v -> showNotifyAudienceChooser());


        ImageButton btnAdd = findViewById(R.id.btn_action_add);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, InviteGuestsActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("EVENT_NAME", eventName);
            startActivity(intent);
        });

        ImageButton btnQr = findViewById(R.id.btn_action_qr);
        btnQr.setOnClickListener(v -> openEventQr());

        ImageButton btnEdit = findViewById(R.id.btn_action_edit);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent i = new Intent(this, CreateEventActivity.class);
                i.putExtra(CreateEventActivity.EXTRA_EDIT_EVENT_ID, eventId);
                startActivity(i);
            });
        }

        ImageButton btnDownload = findViewById(R.id.btn_action_download);
        btnDownload.setOnClickListener(v -> exportFinalRosterCsv()); // Now the blue icon downloads the CSV!

        ImageButton btnLocation = findViewById(R.id.btn_action_location);
        btnLocation.setContentDescription("Open entrant location map");
        btnLocation.setOnClickListener(v -> openEntrantMap());

        ImageButton btnChat = findViewById(R.id.btn_action_chat);
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ModerateCommentsActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("EVENT_NAME", eventName != null ? eventName : "Event");
            startActivity(intent);
        });

        btnCancelNonSignup = findViewById(R.id.btnCancelNonSignup);
        if (btnCancelNonSignup != null) {
            btnCancelNonSignup.setOnClickListener(v -> confirmCancelNonSignup());
        }

        MaterialButton btnBroadcast = findViewById(R.id.btn_broadcast_followers);
        if (btnBroadcast != null) {
            btnBroadcast.setOnClickListener(v ->
                    startActivity(new Intent(this, FollowerBroadcastActivity.class)));
        }

        MaterialButton btnInviteFollowers = findViewById(R.id.btn_invite_followers);
        if (btnInviteFollowers != null) {
            btnInviteFollowers.setOnClickListener(v -> inviteFollowersToEvent());
        }

        tabWaiting.setOnClickListener(v -> {
            currentTab = "waiting";
            loadAttendees();
            highlightTab();
        });
        tabWaiting.setOnLongClickListener(v -> {
            previewWaitingMode = !previewWaitingMode;
            currentTab = "waiting";
            loadCounts();
            loadAttendees();
            highlightTab();
            Toast.makeText(this,
                    previewWaitingMode
                            ? "Sample waiting list preview enabled."
                            : "Sample waiting list preview disabled.",
                    Toast.LENGTH_SHORT).show();
            return true;
        });
        tabChosen.setOnClickListener(v -> {
            currentTab = "selected";
            loadAttendees();
            highlightTab();
        });
        tabEnrolled.setOnClickListener(v -> {
            currentTab = "enrolled";
            loadAttendees();
            highlightTab();
        });
        tabCancelled.setOnClickListener(v -> {
            currentTab = "cancelled";
            loadAttendees();
            highlightTab();
        });
        if (tabInvitees != null) {
            tabInvitees.setOnClickListener(v -> {
                currentTab = "invitees";
                loadAttendees();
                highlightTab();
            });
        }

        loadEventMeta();
        loadCounts();
        loadAttendees();
        highlightTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCounts();
        loadAttendees();
    }

    private void loadEventMeta() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }
                    String loadedName = doc.getString("name");
                    if (!TextUtils.isEmpty(loadedName)) {
                        eventName = loadedName;
                        textEventTitle.setText(loadedName);
                    }
                    Long cap = doc.getLong("capacity");
                    if (cap != null && cap > 0) {
                        eventCapacity = cap.intValue();
                    }

                    ImageButton btnQr = findViewById(R.id.btn_action_qr);
                    if (btnQr != null) {
                        boolean isPrivate = Boolean.TRUE.equals(doc.getBoolean("privateEvent"));
                        btnQr.setVisibility(isPrivate ? View.GONE : View.VISIBLE);
                    }
                });
    }

    private void loadCounts() {
        if (previewWaitingMode) {
            waitingCount = getPreviewEntrants().size();
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            if (!previewWaitingMode) {
                Toast.makeText(this, "This event is missing its database id.", Toast.LENGTH_SHORT).show();
            }
            updateTabLabels();
            return;
        }

        if (!previewWaitingMode) {
            db.collection("events").document(eventId).collection("waitingList").get()
                    .addOnSuccessListener(qs -> {
                        waitingCount = qs.size();
                        updateTabLabels();
                    });
        } else {
            updateTabLabels();
        }

        db.collection("events").document(eventId).collection("selected").get()
                .addOnSuccessListener(qs -> {
                    chosenCount = qs.size();
                    updateTabLabels();
                });

        db.collection("events").document(eventId).collection("inviteeList").get()
                .addOnSuccessListener(qs -> {
                    inviteeCount = qs.size();
                    updateTabLabels();
                });

        db.collection("events").document(eventId).collection("enrolled").get()
                .addOnSuccessListener(qs -> {
                    enrolledCount = qs.size();
                    updateTabLabels();
                });

        db.collection("events").document(eventId).collection("cancelled").get()
                .addOnSuccessListener(qs -> {
                    cancelledCount = qs.size();
                    updateTabLabels();
                });
    }

    private void updateTabLabels() {
        tabWaiting.setText(String.format(Locale.getDefault(), "Waiting (%d)", waitingCount));
        tabChosen.setText(String.format(Locale.getDefault(), "Chosen (%d)", chosenCount));
        if (tabInvitees != null) {
            tabInvitees.setText(String.format(Locale.getDefault(), "Invitees (%d)", inviteeCount));
        }
        tabEnrolled.setText(String.format(Locale.getDefault(), "Enrolled (%d)", enrolledCount));
        tabCancelled.setText(String.format(Locale.getDefault(), "Cancelled (%d)", cancelledCount));
        highlightTab();
    }

    private void loadAttendees() {
        if (previewWaitingMode && "waiting".equals(currentTab)) {
            List<AnasEntrant> preview = getPreviewEntrants();
            recyclerChosenEntrants.setAdapter(new EntrantAdapter(preview));
            badgeTotal.setText(preview.size() + " Total");
            hideEmptyState();
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            recyclerChosenEntrants.setAdapter(new EntrantAdapter(new ArrayList<>()));
            badgeTotal.setText("0 Total");
            showEmptyState("This event could not be loaded correctly.");
            return;
        }

        String collection = collectionForTab(currentTab);

        db.collection("events").document(eventId).collection(collection).get()
                .addOnSuccessListener(qs -> {
                    List<AnasEntrant> attendeeRows = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String deviceId = doc.getString("deviceId");
                        if (deviceId == null || deviceId.trim().isEmpty()) {
                            deviceId = doc.getId();
                        }
                        String tabLabel = currentTab;
                        AnasEntrant e = new AnasEntrant(deviceId,
                                name != null ? name : deviceId,
                                email != null ? email : "",
                                tabLabel);
                        attendeeRows.add(e);
                    }
                    Collections.sort(attendeeRows, Comparator.comparing(
                            entrant -> entrant.getName() == null ? "" : entrant.getName().toLowerCase(Locale.getDefault())
                    ));
                    List<AnasEntrant> groupedEntrants = buildGroupedEntrants(attendeeRows);
                    recyclerChosenEntrants.setAdapter(new EntrantAdapter(groupedEntrants));
                    badgeTotal.setText(attendeeRows.size() + " Total");
                    if (groupedEntrants.isEmpty()) {
                        showEmptyState(getEmptyMessage());
                    } else {
                        hideEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    badgeTotal.setText("0 Total");
                    showEmptyState("Failed to load people for this section.");
                    Toast.makeText(this, "Failed to load attendees", Toast.LENGTH_SHORT).show();
                });
    }

    private String collectionForTab(String tab) {
        switch (tab) {
            case "waiting":
                return "waitingList";
            case "selected":
                return "selected";
            case "invitees":
                return "inviteeList";
            case "enrolled":
                return "enrolled";
            case "cancelled":
                return "cancelled";
            default:
                return "waitingList";
        }
    }

    private void highlightTab() {
        setTabStyle(tabWaiting, "waiting".equals(currentTab));
        setTabStyle(tabChosen, "selected".equals(currentTab));
        if (tabInvitees != null) {
            setTabStyle(tabInvitees, "invitees".equals(currentTab));
        }
        setTabStyle(tabEnrolled, "enrolled".equals(currentTab));
        setTabStyle(tabCancelled, "cancelled".equals(currentTab));

        if ("waiting".equals(currentTab)) {
            textListHeader.setText("Waiting List");
        } else if ("selected".equals(currentTab)) {
            textListHeader.setText("Chosen Entrants");
        } else if ("invitees".equals(currentTab)) {
            textListHeader.setText("Invitees");
        } else if ("enrolled".equals(currentTab)) {
            textListHeader.setText("Enrolled Entrants");
        } else {
            textListHeader.setText("Cancelled Entrants");
        }

        int totalForBadge;
        switch (currentTab) {
            case "waiting":
                totalForBadge = previewWaitingMode ? waitingCount : waitingCount;
                break;
            case "selected":
                totalForBadge = chosenCount;
                break;
            case "invitees":
                totalForBadge = inviteeCount;
                break;
            case "enrolled":
                totalForBadge = enrolledCount;
                break;
            case "cancelled":
                totalForBadge = cancelledCount;
                break;
            default:
                totalForBadge = 0;
        }
        badgeTotal.setText(totalForBadge + " Total");
    }

    private void setTabStyle(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tab.setTextColor(selected ? Color.WHITE : Color.parseColor("#5A5C69"));
        tab.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void showEmptyState(String message) {
        textEmptyList.setText(message);
        textEmptyList.setVisibility(View.VISIBLE);
        recyclerChosenEntrants.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        textEmptyList.setVisibility(View.GONE);
        recyclerChosenEntrants.setVisibility(View.VISIBLE);
    }

    private String getEmptyMessage() {
        if ("selected".equals(currentTab)) {
            return "No chosen entrants yet.";
        }
        if ("invitees".equals(currentTab)) {
            return "No pending invitees yet.";
        }
        if ("enrolled".equals(currentTab)) {
            return "No enrolled entrants yet.";
        }
        if ("cancelled".equals(currentTab)) {
            return "No cancelled entrants yet.";
        }
        return "No one is on the waiting list yet.";
    }

    private List<AnasEntrant> buildGroupedEntrants(List<AnasEntrant> attendees) {
        List<AnasEntrant> grouped = new ArrayList<>();
        String currentHeader = "";
        for (AnasEntrant attendee : attendees) {
            String displayName = attendee.getName() == null ? "" : attendee.getName().trim();
            String letter = displayName.isEmpty()
                    ? "#"
                    : String.valueOf(Character.toUpperCase(displayName.charAt(0)));
            if (!letter.equals(currentHeader)) {
                currentHeader = letter;
                AnasEntrant header = new AnasEntrant("", letter, "", "header");
                header.setSectionHeader(true);
                grouped.add(header);
            }
            grouped.add(attendee);
        }
        return grouped;
    }

    private void showNotifyAudienceChooser() {
        // 1. We added "Cancelled Entrants" as the 4th option in the list
        String[] options = {"Waiting List", "Chosen", "Enrolled", "Cancelled Entrants"};

        new AlertDialog.Builder(this)
                .setTitle("Send notification")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            organizerNotificationManager.notifyWaitingList(
                                    eventId,
                                    eventName != null ? eventName : "Event",
                                    createNotificationCallback("waiting list")
                            );
                            break;
                        case 1:
                            organizerNotificationManager.notifySelected(
                                    eventId,
                                    eventName != null ? eventName : "Event",
                                    createNotificationCallback("chosen entrants")
                            );
                            break;
                        case 2:
                            organizerNotificationManager.notifyEnrolled(
                                    eventId,
                                    eventName != null ? eventName : "Event",
                                    createNotificationCallback("enrolled entrants")
                            );
                            break;
                        case 3: // 2. This handles the new Cancelled Entrants option!
                            organizerNotificationManager.notifyCancelled(
                                    eventId,
                                    eventName != null ? eventName : "Event",
                                    createNotificationCallback("cancelled entrants")
                            );
                            break;
                    }
                })
                // 3. This adds a safe "Cancel" button to close the menu
                .setNegativeButton("Cancel", null)
                .show();
    }

    private OrganizerNotificationManager.NotificationDispatchCallback createNotificationCallback(String audienceLabel) {
        return new OrganizerNotificationManager.NotificationDispatchCallback() {
            @Override
            public void onSuccess(int recipientCount) {
                Toast.makeText(ManageEventActivity.this,
                        "Notification sent to " + recipientCount + " " + audienceLabel + ".",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(ManageEventActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
    }

    /** US 02.07.03 — notify everyone in {@code events/{eventId}/cancelled}. */
    private void confirmNotifyCancelled() {
        String label = eventName != null ? eventName : "this event";
        new AlertDialog.Builder(this)
                .setTitle("Notify cancelled entrants")
                .setMessage("Send an in-app notification to everyone in the cancelled list for "
                        + label + "?")
                .setPositiveButton("Send", (d, w) -> organizerNotificationManager.notifyCancelled(
                        eventId,
                        eventName != null ? eventName : "Event",
                        new OrganizerNotificationManager.NotificationDispatchCallback() {
                            @Override
                            public void onSuccess(int recipientCount) {
                                Toast.makeText(ManageEventActivity.this,
                                        "Notification sent to " + recipientCount + " cancelled entrants.",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String message) {
                                Toast.makeText(ManageEventActivity.this,
                                        message,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                ))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** US 02.06.05 — export final enrolled roster as CSV. */
    private void exportFinalRosterCsv() {
        Toast.makeText(this, "Building CSV…", Toast.LENGTH_SHORT).show();
        db.collection("events").document(eventId).collection("enrolled")
                .get()
                .addOnSuccessListener(qs -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("name,email,deviceId\n");
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        sb.append(escapeCsvField(doc.getString("name")))
                                .append(',')
                                .append(escapeCsvField(doc.getString("email")))
                                .append(',')
                                .append(escapeCsvField(
                                        doc.getString("deviceId") != null ? doc.getString("deviceId") : doc.getId()))
                                .append('\n');
                    }

                    String safeName = (eventName != null ? eventName : "event")
                            .replaceAll("[^a-zA-Z0-9_-]", "_");
                    File file = new File(getCacheDir(), "roster_" + safeName + ".csv");
                    try (FileWriter fw = new FileWriter(file)) {
                        fw.write(sb.toString());
                    } catch (IOException e) {
                        Toast.makeText(this, "Could not write CSV file.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Uri uri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", file);

                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/csv");
                    share.putExtra(Intent.EXTRA_SUBJECT, "Final roster — "
                            + (eventName != null ? eventName : "Event"));
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "Export CSV"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load enrolled list.",
                                Toast.LENGTH_SHORT).show());
    }

    private static String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** US 02.06.04 — cancel invited entrants who never enrolled. */
    private void confirmCancelNonSignup() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel non-enrolled invitees")
                .setMessage("Move everyone who is still in “chosen” but has not enrolled into the cancelled list? "
                        + "This matches invited entrants who never completed enrollment.")
                .setPositiveButton("Cancel them", (d, w) -> executeCancelNonSignup())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void executeCancelNonSignup() {
        if (btnCancelNonSignup != null) {
            btnCancelNonSignup.setEnabled(false);
        }
        Toast.makeText(this, "Updating roster…", Toast.LENGTH_SHORT).show();
        eventRepository.cancelSelectedWithoutEnrollment(eventId, new EventRepository.CancelNonEnrollmentCallback() {
            @Override
            public void onSuccess(@NonNull List<String> movedDeviceIds) {
                String label = eventName != null ? eventName : "Event";
                organizerNotificationManager.notifyOrganizerRevokedNonEnrollment(
                        eventId, label, movedDeviceIds,
                        new OrganizerNotificationManager.NotificationDispatchCallback() {
                            @Override
                            public void onSuccess(int recipientCount) {
                                runOnUiThread(() -> {
                                    if (btnCancelNonSignup != null) {
                                        btnCancelNonSignup.setEnabled(true);
                                    }
                                    Toast.makeText(ManageEventActivity.this,
                                            "Moved to cancelled and notified " + movedDeviceIds.size()
                                                    + " entrant(s).",
                                            Toast.LENGTH_SHORT).show();
                                    loadCounts();
                                    loadAttendees();
                                    highlightTab();
                                });
                            }

                            @Override
                            public void onFailure(@NonNull String message) {
                                runOnUiThread(() -> {
                                    if (btnCancelNonSignup != null) {
                                        btnCancelNonSignup.setEnabled(true);
                                    }
                                    Toast.makeText(ManageEventActivity.this,
                                            "Roster updated but notifications failed: " + message,
                                            Toast.LENGTH_LONG).show();
                                    loadCounts();
                                    loadAttendees();
                                    highlightTab();
                                });
                            }
                        });
            }

            @Override
            public void onFailure(String message) {
                if (btnCancelNonSignup != null) {
                    btnCancelNonSignup.setEnabled(true);
                }
                Toast.makeText(ManageEventActivity.this,
                        message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openEventQr() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("privateEvent"))) {
                        Toast.makeText(this, "QR codes are not available for private events.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent intent = new Intent(this, EventQRActivity.class);
                    intent.putExtra("EVENT_ID", eventId);
                    intent.putExtra("EVENT_NAME", eventName);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not verify event.", Toast.LENGTH_SHORT).show());
    }

    private void openEntrantMap() {
        Intent intent = new Intent(this, EntrantMapActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("EVENT_NAME", eventName != null ? eventName : "Event");
        startActivity(intent);
    }

    private void shareCurrentTabList() {
        if (previewWaitingMode && "waiting".equals(currentTab)) {
            StringBuilder sb = new StringBuilder();
            for (AnasEntrant e : getPreviewEntrants()) {
                sb.append(e.getName()).append(" — ").append(e.getEmail()).append('\n');
            }
            shareText("Waiting list (preview)\n\n" + sb);
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Event not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        String collection = collectionForTab(currentTab);
        db.collection("events").document(eventId).collection(collection).get()
                .addOnSuccessListener(qs -> {
                    StringBuilder sb = new StringBuilder();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String line = (name != null ? name : doc.getId())
                                + (email != null && !email.isEmpty() ? " — " + email : "");
                        sb.append(line).append('\n');
                    }
                    String title = sectionTitleForShare();
                    shareText(title + "\n\n" + (sb.length() > 0 ? sb.toString().trim() : "(empty)"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load list to share.", Toast.LENGTH_SHORT).show());
    }

    private String sectionTitleForShare() {
        switch (currentTab) {
            case "waiting":
                return "Waiting list — " + (eventName != null ? eventName : "Event");
            case "selected":
                return "Chosen entrants — " + (eventName != null ? eventName : "Event");
            case "invitees":
                return "Invitees — " + (eventName != null ? eventName : "Event");
            case "enrolled":
                return "Enrolled entrants — " + (eventName != null ? eventName : "Event");
            case "cancelled":
                return "Cancelled entrants — " + (eventName != null ? eventName : "Event");
            default:
                return eventName != null ? eventName : "Event";
        }
    }

    private void shareText(String text) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Share list"));
    }

    private void inviteFollowersToEvent() {
        String organizerId = new DeviceAuthManager().getDeviceId(this);
        FollowRepository followRepo = new FollowRepository();

        followRepo.getFollowerCount(organizerId, count -> runOnUiThread(() -> {
            if (count == 0) {
                Toast.makeText(this, R.string.invite_followers_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.invite_followers)
                    .setMessage(getString(R.string.invite_followers_confirm, count))
                    .setPositiveButton("Invite", (d, w) -> {
                        followRepo.inviteAllFollowersToWaitlist(eventId, organizerId,
                                new FollowRepository.FollowCallback() {
                                    @Override
                                    public void onSuccess() {
                                        runOnUiThread(() -> {
                                            Toast.makeText(ManageEventActivity.this,
                                                    R.string.invite_followers_success,
                                                    Toast.LENGTH_SHORT).show();
                                            loadCounts();
                                            loadAttendees();
                                        });
                                    }

                                    @Override
                                    public void onFailure(@NonNull String message) {
                                        runOnUiThread(() -> Toast.makeText(
                                                ManageEventActivity.this, message,
                                                Toast.LENGTH_LONG).show());
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }));
    }

    private List<AnasEntrant> getPreviewEntrants() {
        List<String> previewNames = Arrays.asList(
                "Alex Jim", "Adrian Oliveira", "Adnan Sajid", "Bao Shu",
                "Bella Ahmed", "Chris Wong", "Dina Qureshi", "Ethan Cole",
                "Fatima Noor", "Hassan Ali", "Ivy Chen", "Jason Park",
                "Kira Singh", "Liam Brown", "Maya Patel", "Noah Smith"
        );
        List<AnasEntrant> entrants = new ArrayList<>();
        for (int i = 0; i < previewNames.size(); i++) {
            String name = previewNames.get(i);
            entrants.add(new AnasEntrant(
                    "preview-" + i,
                    name,
                    name.toLowerCase().replace(" ", ".") + "@spotly.test",
                    "waiting"
            ));
        }
        return entrants;
    }
}
