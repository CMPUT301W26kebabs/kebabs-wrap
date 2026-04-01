package com.example.eventmanager.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen that displays a chronological log of every notification
 * dispatched by organizers to entrants.
 *
 * Reads from the top-level Firestore collection {@code notificationLogs}.
 * Supports live updates, search-by-organizer-name, and paginated loading.
 *
 * US 03.08.01 — As an administrator, I want to review logs of all
 * notifications sent to entrants by organizers.
 */
public class AdminNotificationLogsActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 50;

    private FirebaseFirestore db;
    private ListenerRegistration logsListener;
    private AdminNotificationLogsAdapter adapter;
    private TextView tvEmpty;
    private View btnLoadMore;

    /** Master list of ALL items fetched so far (before search filter). */
    private final List<AdminNotificationLogItem> allItems = new ArrayList<>();

    /** The last document snapshot used for Firestore cursor-based pagination. */
    private DocumentSnapshot lastVisible;

    private String currentSearchQuery = "";

    // ─────────────────────────────────────────────────────────────────
    //  LIFECYCLE
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notificationlogs);

        db = FirebaseFirestore.getInstance();

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Empty state
        tvEmpty = findViewById(R.id.tv_empty);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rv_notification_logs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminNotificationLogsAdapter();
        rv.setAdapter(adapter);

        // Search bar
        EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                applyFilter(currentSearchQuery);
            }
        });

        // Filter buttons (placeholder – can be extended later)
        findViewById(R.id.btn_filter_date).setOnClickListener(v ->
                Toast.makeText(this, "Date filter coming soon.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_export).setOnClickListener(v ->
                Toast.makeText(this, "Export coming soon.", Toast.LENGTH_SHORT).show());

        // Load-more pagination
        btnLoadMore = findViewById(R.id.btn_load_more);
        btnLoadMore.setOnClickListener(v -> loadMoreLogs());

        // Initial data load
        attachLogsListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logsListener != null) {
            logsListener.remove();
            logsListener = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  FIRESTORE — REAL-TIME LISTENER (first page)
    // ─────────────────────────────────────────────────────────────────

    private void attachLogsListener() {
        if (logsListener != null) return;

        logsListener = db.collection("notificationLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this,
                                "Could not load notification logs: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Could not load logs. Check your connection.");
                        adapter.setItems(new ArrayList<>());
                        return;
                    }

                    if (snapshot == null) return;

                    allItems.clear();
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    for (DocumentSnapshot doc : docs) {
                        AdminNotificationLogItem item =
                                AdminNotificationLogItem.fromSnapshot(doc);
                        if (item != null) {
                            allItems.add(item);
                        }
                    }

                    // Track last document for pagination
                    if (!docs.isEmpty()) {
                        lastVisible = docs.get(docs.size() - 1);
                        btnLoadMore.setVisibility(
                                docs.size() >= PAGE_SIZE ? View.VISIBLE : View.GONE);
                    } else {
                        lastVisible = null;
                        btnLoadMore.setVisibility(View.GONE);
                    }

                    applyFilter(currentSearchQuery);
                });
    }

    // ─────────────────────────────────────────────────────────────────
    //  FIRESTORE — LOAD MORE (pagination)
    // ─────────────────────────────────────────────────────────────────

    private void loadMoreLogs() {
        if (lastVisible == null) {
            Toast.makeText(this, "No more logs to load.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("notificationLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    for (DocumentSnapshot doc : docs) {
                        AdminNotificationLogItem item =
                                AdminNotificationLogItem.fromSnapshot(doc);
                        if (item != null) {
                            allItems.add(item);
                        }
                    }

                    if (!docs.isEmpty()) {
                        lastVisible = docs.get(docs.size() - 1);
                    }

                    btnLoadMore.setVisibility(
                            docs.size() >= PAGE_SIZE ? View.VISIBLE : View.GONE);

                    applyFilter(currentSearchQuery);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load more: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─────────────────────────────────────────────────────────────────
    //  CLIENT-SIDE SEARCH FILTER
    // ─────────────────────────────────────────────────────────────────

    private void applyFilter(String query) {
        String q = query.toLowerCase();
        List<AdminNotificationLogItem> filtered = new ArrayList<>();

        for (AdminNotificationLogItem item : allItems) {
            if (q.isEmpty()) {
                filtered.add(item);
            } else {
                // Search across organizer name, recipient group, and message
                if (item.organizerName.toLowerCase().contains(q)
                        || item.recipientGroup.toLowerCase().contains(q)
                        || item.message.toLowerCase().contains(q)) {
                    filtered.add(item);
                }
            }
        }

        adapter.setItems(filtered);

        boolean empty = filtered.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && !q.isEmpty()) {
            tvEmpty.setText("No logs matching \"" + query + "\".");
        } else if (empty) {
            tvEmpty.setText("No notification logs yet.");
        }
    }
}
