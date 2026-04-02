package com.example.eventmanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Shown when a user taps an invitation notification.
 * Presents the event name and two choices: Accept or Decline.
 *
 * Accept flow:
 *   If private invite ({@code inviteeList}): {@code acceptInviteToWaitingList()}.
 *   If lottery winner ({@code selected}): {@code enrollUser()} → enrolled.
 *
 * Decline flow:
 *   {@code declineInvitation()} (clears invitee or selected, records cancelled).
 */
public class AcceptDeclineActivity extends AppCompatActivity {

    private EventRepository eventRepository;

    private String eventId;
    private String eventName;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_decline);

        // Unpack Intent extras passed from NotificationsActivity
        eventId   = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        deviceId  = getIntent().getStringExtra("deviceId");
        if (eventId == null || deviceId == null) {
            Toast.makeText(this, "Invitation data is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        eventRepository = new EventRepository();

        // Populate the event name on screen
        TextView textEventName = findViewById(R.id.text_event_name);
        textEventName.setText(eventName != null ? eventName : "Event");
        ImageButton backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        Button btnAccept  = findViewById(R.id.btn_accept);
        Button btnDecline = findViewById(R.id.btn_decline);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<DocumentSnapshot> taskInv =
                db.collection("events").document(eventId).collection("inviteeList").document(deviceId).get();
        Task<DocumentSnapshot> taskSel =
                db.collection("events").document(eventId).collection("selected").document(deviceId).get();
        Tasks.whenAllComplete(taskInv, taskSel).addOnCompleteListener(t -> {
            if (!taskInv.isSuccessful() || !taskSel.isSuccessful()) {
                return;
            }
            DocumentSnapshot inv = taskInv.getResult();
            DocumentSnapshot sel = taskSel.getResult();
            boolean active = (inv != null && inv.exists()) || (sel != null && sel.exists());
            if (!active) {
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
                Toast.makeText(this,
                        "This invitation is no longer active.",
                        Toast.LENGTH_LONG).show();
            }
        });

        // ── Accept ──────────────────────────────────────────────────────────
        btnAccept.setOnClickListener(v -> {
            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);
            Task<DocumentSnapshot> invGet = db.collection("events").document(eventId)
                    .collection("inviteeList").document(deviceId).get();
            Task<DocumentSnapshot> selGet = db.collection("events").document(eventId)
                    .collection("selected").document(deviceId).get();
            Tasks.whenAllComplete(invGet, selGet).addOnCompleteListener(ignored -> {
                if (!invGet.isSuccessful() || !selGet.isSuccessful()) {
                    btnAccept.setEnabled(true);
                    btnDecline.setEnabled(true);
                    Toast.makeText(AcceptDeclineActivity.this, "Could not verify invitation.", Toast.LENGTH_LONG).show();
                    return;
                }
                DocumentSnapshot inv = invGet.getResult();
                DocumentSnapshot sel = selGet.getResult();
                boolean fromInvitee = inv != null && inv.exists();
                boolean fromSelected = sel != null && sel.exists();
                if (fromInvitee) {
                    eventRepository.acceptInviteToWaitingList(eventId, deviceId, new EventRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AcceptDeclineActivity.this, "You joined the waiting list.", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String message) {
                            btnAccept.setEnabled(true);
                            btnDecline.setEnabled(true);
                            Toast.makeText(AcceptDeclineActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else if (fromSelected) {
                    eventRepository.enrollUser(eventId, deviceId, new EventRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AcceptDeclineActivity.this, "Invitation accepted.", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String message) {
                            btnAccept.setEnabled(true);
                            btnDecline.setEnabled(true);
                            Toast.makeText(AcceptDeclineActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    btnAccept.setEnabled(true);
                    btnDecline.setEnabled(true);
                    Toast.makeText(AcceptDeclineActivity.this,
                            "This invitation is no longer active.", Toast.LENGTH_LONG).show();
                }
            });
        });

        // ── Decline ─────────────────────────────────────────────────────────
        btnDecline.setOnClickListener(v -> {
            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);
            eventRepository.declineInvitation(eventId, deviceId, new EventRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AcceptDeclineActivity.this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    btnAccept.setEnabled(true);
                    btnDecline.setEnabled(true);
                    Toast.makeText(AcceptDeclineActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}