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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Organizer view: lists comments for one event and allows removal.
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
