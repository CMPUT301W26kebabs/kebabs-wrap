package com.example.eventmanager.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Admin screen for moderating comments across all events.
 * Loads comments via a Firestore {@code collectionGroup("comments")} query and allows
 * deletion with confirmation. Uses client-side sorting to avoid requiring a
 * collection-group index.
 */
public class AdminCommentsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration commentsListener;
    private AdminCommentsAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_comments);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_filter).setOnClickListener(v ->
                Toast.makeText(this, "Comment filters coming soon.", Toast.LENGTH_SHORT).show());

        tvEmpty = findViewById(R.id.tv_comments_empty);
        RecyclerView rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminCommentsAdapter(this::confirmAndDeleteComment);
        rvComments.setAdapter(adapter);

        attachCommentsListener();
    }

    private void attachCommentsListener() {
        if (commentsListener != null) {
            return;
        }
        commentsListener = db.collectionGroup("comments")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this,
                                getString(R.string.admin_comments_load_error, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(R.string.admin_comments_load_failed);
                        adapter.setItems(new ArrayList<>());
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
                    List<AdminCommentListItem> list = new ArrayList<>(docs.size());
                    for (DocumentSnapshot doc : docs) {
                        AdminCommentListItem item = AdminCommentListItem.fromSnapshot(doc);
                        if (item != null) {
                            list.add(item);
                        }
                    }
                    adapter.setItems(list);
                    boolean empty = list.isEmpty();
                    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    if (empty) {
                        tvEmpty.setText(R.string.admin_comments_empty);
                    }
                });
    }

    private void confirmAndDeleteComment(AdminCommentListItem item) {
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
                                                getString(R.string.admin_comments_remove_failed,
                                                        err.getMessage()),
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
