package com.example.eventmanager.admin;

import android.os.Bundle;
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

import com.example.eventmanager.R;
import com.example.eventmanager.adapter.ProfileAdapter;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminProfilesActivity extends AppCompatActivity implements ProfileAdapter.OnProfileClickListener {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ProfileAdapter adapter;
    private List<DocumentSnapshot> allProfiles = new ArrayList<>();
    private FirebaseRepository repository;

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
        new AlertDialog.Builder(this).setTitle("Disable Profile")
            .setMessage("Disable \"" + doc.getString("name") + "\"?\nThey won't be able to join waiting lists.")
            .setPositiveButton("Disable", (dg, w) -> {
                String aid = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                repository.softDeleteProfile(doc.getId(), aid, new FirebaseRepository.OnOperationCompleteListener() {
                    public void onSuccess() { Toast.makeText(AdminProfilesActivity.this, "Profile disabled", Toast.LENGTH_SHORT).show(); loadProfiles(); }
                    public void onError(Exception e) { Toast.makeText(AdminProfilesActivity.this, "Failed", Toast.LENGTH_LONG).show(); }
                });
            }).setNegativeButton("Cancel", null).show();
    }
}
