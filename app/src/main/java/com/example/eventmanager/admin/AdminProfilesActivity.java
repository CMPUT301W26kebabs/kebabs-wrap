package com.example.eventmanager.admin;

import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.repository.FirebaseRepository;
import com.example.eventmanager.R;
import com.example.eventmanager.adapter.ProfileAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for browsing, searching, and soft-deleting user profiles.
 * Displays a searchable list of all registered entrants.
 *
 * <p>Covers user stories US 03.02 and US 03.05.</p>
 */
public class AdminProfilesActivity extends AppCompatActivity implements ProfileAdapter.OnProfileClickListener {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ProfileAdapter adapter;
    private List<DocumentSnapshot> allProfiles = new ArrayList<>();
    private FirebaseRepository repository;
    private final com.example.eventmanager.repository.FirebaseRepository userDataRepository =
            new com.example.eventmanager.repository.FirebaseRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profiles);
        repository = FirebaseRepository.getInstance();

        recyclerView = findViewById(R.id.rv_profiles);
        searchBar = findViewById(R.id.et_search);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        adapter = new ProfileAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { filterProfiles(s.toString().trim()); }
        });
        loadProfiles();
    }

    private void loadProfiles() {
        progressBar.setVisibility(View.VISIBLE); recyclerView.setVisibility(View.GONE);
        repository.fetchAllProfiles(new FirebaseRepository.OnDocumentsLoadedListener() {
            public void onLoaded(List<DocumentSnapshot> docs) {
                allProfiles = docs; filterProfiles(searchBar.getText().toString().trim());
                progressBar.setVisibility(View.GONE); recyclerView.setVisibility(View.VISIBLE);
            }
            public void onError(Exception e) {
                Toast.makeText(AdminProfilesActivity.this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE); emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filterProfiles(String query) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allProfiles) {
            if (!query.isEmpty()) {
                String n = doc.getString("name"); String e = doc.getString("email");
                String s = ((n!=null?n:"")+" "+(e!=null?e:"")+" "+doc.getId()).toLowerCase();
                if (!s.contains(query.toLowerCase())) continue;
            }
            filtered.add(doc);
        }
        adapter.updateList(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override public void onProfileClick(DocumentSnapshot doc) {
        AdminProfileDetailDialog d = AdminProfileDetailDialog.newInstance(doc.getId());
        d.setOnProfileRemovedListener(this::loadProfiles);
        d.show(getSupportFragmentManager(), "profile_detail");
    }

    @Override public void onProfileRemoveClick(DocumentSnapshot doc) {
        String targetId = doc.getId();
        String myId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (myId != null && myId.equals(targetId)) {
            Toast.makeText(this, R.string.admin_cannot_delete_self, Toast.LENGTH_LONG).show();
            return;
        }
        String name = doc.getString("name");
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_delete_profile_title)
                .setMessage(getString(R.string.admin_delete_profile_message)
                        + (name != null ? "\n\n" + name : ""))
                .setPositiveButton(R.string.admin_comments_remove_confirm, (dg, w) ->
                        userDataRepository.deleteUserAndAllRegistrations(targetId,
                                new com.example.eventmanager.repository.FirebaseRepository.RepoCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        FirebaseStorage.getInstance().getReference()
                                                .child("users/" + targetId + "/avatar.jpg")
                                                .delete();
                                        Toast.makeText(AdminProfilesActivity.this,
                                                R.string.admin_profile_deleted, Toast.LENGTH_SHORT).show();
                                        loadProfiles();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(AdminProfilesActivity.this,
                                                e.getMessage() != null ? e.getMessage() : "Delete failed",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
