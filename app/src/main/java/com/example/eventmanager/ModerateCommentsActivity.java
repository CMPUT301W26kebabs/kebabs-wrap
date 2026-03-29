package com.example.eventmanager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.admin.AdminCommentListItem;
import com.example.eventmanager.admin.AdminCommentsAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Organizer view: lists comments for one event, allows removal, and posting (US 02.08.02).
 */
public class ModerateCommentsActivity extends AppCompatActivity {

    private String eventId;
    private String eventName;
    private FirebaseFirestore db;
    private ListenerRegistration commentsListener;
    private AdminCommentsAdapter adapter;
    private final List<AdminCommentListItem> allItems = new ArrayList<>();
    private TextView tvEmpty;
    private EditText editSearch;
    private EditText editCompose;
    private View btnSendComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moderate_comments);

        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.text_app_bar_title);
        String label = eventName != null && !eventName.isEmpty() ? eventName : "Event";
        title.setText("Moderate — " + label);

        tvEmpty = findViewById(R.id.tv_comments_empty);
        RecyclerView rv = findViewById(R.id.recycler_comments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminCommentsAdapter(this::confirmDelete);
        rv.setAdapter(adapter);

        editSearch = findViewById(R.id.edit_search_comments);
        editCompose = findViewById(R.id.edit_compose_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        btnSendComment.setOnClickListener(v -> submitOrganizerComment());

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s != null ? s.toString() : "");
            }
        });

        attachListener();
    }

    /** US 02.08.02 — same sub-collection as entrant comments; gated to event organizer when {@code organizerId} is set. */
    private void submitOrganizerComment() {
        if (editCompose == null) {
            return;
        }
        String text = editCompose.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Write a comment first", Toast.LENGTH_SHORT).show();
            return;
        }
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        btnSendComment.setEnabled(false);

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    String orgId = eventDoc != null ? eventDoc.getString("organizerId") : null;
                    if (orgId != null && !orgId.trim().isEmpty() && !orgId.equals(deviceId)) {
                        btnSendComment.setEnabled(true);
                        Toast.makeText(this, "Only the event organizer can post here.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    db.collection("users").document(deviceId).get()
                            .addOnSuccessListener(userDoc -> {
                                String authorLabel = "Organizer";
                                if (userDoc.exists()) {
                                    String n = userDoc.getString("name");
                                    if (n != null && !n.trim().isEmpty()) {
                                        authorLabel = n.trim();
                                    }
                                }
                                Map<String, Object> data = new HashMap<>();
                                data.put("deviceId", deviceId);
                                data.put("authorName", authorLabel);
                                data.put("text", text);
                                data.put("timestamp", FieldValue.serverTimestamp());
                                data.put("eventId", eventId);
                                if (eventName != null && !eventName.isEmpty()) {
                                    data.put("eventName", eventName);
                                }
                                data.put("authorRole", "organizer");

                                db.collection("events").document(eventId).collection("comments").add(data)
                                        .addOnSuccessListener(docRef -> {
                                            editCompose.setText("");
                                            btnSendComment.setEnabled(true);
                                        })
                                        .addOnFailureListener(err -> {
                                            btnSendComment.setEnabled(true);
                                            Toast.makeText(this,
                                                    getString(R.string.admin_comments_remove_failed, err.getMessage()),
                                                    Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(err -> {
                                btnSendComment.setEnabled(true);
                                Toast.makeText(this, "Could not load profile.", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(err -> {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(this, "Could not verify organizer.", Toast.LENGTH_LONG).show();
                });
    }

    private void attachListener() {
        if (commentsListener != null) {
            return;
        }
        commentsListener = db.collection("events").document(eventId)
                .collection("comments")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this,
                                getString(R.string.admin_comments_load_error, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) {
                        return;
                    }
                    List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
                    Collections.sort(docs, (a, b) -> {
                        Timestamp ta = a.getTimestamp("timestamp");
                        Timestamp tb = b.getTimestamp("timestamp");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });
                    allItems.clear();
                    for (DocumentSnapshot doc : docs) {
                        AdminCommentListItem item = AdminCommentListItem.fromSnapshot(doc);
                        if (item != null) {
                            allItems.add(item);
                        }
                    }
                    applyFilter(editSearch != null ? editSearch.getText().toString() : "");
                });
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<AdminCommentListItem> filtered = new ArrayList<>();
        for (AdminCommentListItem item : allItems) {
            if (q.isEmpty()) {
                filtered.add(item);
            } else {
                String hay = (item.authorName + " " + item.commentText + " " + item.eventLine)
                        .toLowerCase(Locale.getDefault());
                if (hay.contains(q)) {
                    filtered.add(item);
                }
            }
        }
        adapter.setItems(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(AdminCommentListItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_comments_remove_title)
                .setMessage(R.string.admin_comments_remove_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_comments_remove_confirm, (d, w) ->
                        item.ref.delete()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, R.string.admin_comments_removed, Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(err ->
                                        Toast.makeText(this,
                                                getString(R.string.admin_comments_remove_failed, err.getMessage()),
                                                Toast.LENGTH_LONG).show()))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }
}
