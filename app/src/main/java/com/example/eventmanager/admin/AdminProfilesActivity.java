package com.example.eventmanager.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.eventmanager.R;
import com.example.eventmanager.adapter.ProfileAdapter;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminProfilesActivity extends AppCompatActivity implements ProfileAdapter.OnProfileClickListener {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvProfileCount;
    private SwipeRefreshLayout swipeRefresh;
    private Chip chipAll, chipActive, chipDisabled;
    private ProfileAdapter adapter;
    private List<DocumentSnapshot> allProfiles = new ArrayList<>();
    private FirebaseRepository repository;
    private String currentFilter = "active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profiles);
        repository = FirebaseRepository.getInstance();
        initViews();
        setupRecyclerView();
        setupSearch();
        setupChipFilters();
        setupSwipeRefresh();
        loadProfiles();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_profiles);
        searchBar = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        tvProfileCount = findViewById(R.id.tv_profile_count);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        chipAll = findViewById(R.id.chip_all);
        chipActive = findViewById(R.id.chip_active);
        chipDisabled = findViewById(R.id.chip_disabled);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ProfileAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filterProfiles(s.toString().trim()); }
        });
    }

    private void setupChipFilters() {
        chipActive.setChecked(true);
        View.OnClickListener chipListener = v -> {
            chipAll.setChecked(false);
            chipActive.setChecked(false);
            chipDisabled.setChecked(false);
            ((Chip) v).setChecked(true);
            if (v.getId() == R.id.chip_all) currentFilter = "all";
            else if (v.getId() == R.id.chip_active) currentFilter = "active";
            else if (v.getId() == R.id.chip_disabled) currentFilter = "disabled";
            filterProfiles(searchBar.getText().toString().trim());
        };
        chipAll.setOnClickListener(chipListener);
        chipActive.setOnClickListener(chipListener);
        chipDisabled.setOnClickListener(chipListener);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.admin_primary, R.color.admin_accent);
        swipeRefresh.setOnRefreshListener(this::loadProfiles);
    }

    private void loadProfiles() {
        showLoading(true);
        repository.fetchAllProfiles(new FirebaseRepository.OnDocumentsLoadedListener() {
            @Override public void onLoaded(List<DocumentSnapshot> documents) {
                allProfiles = documents;
                filterProfiles(searchBar.getText().toString().trim());
                showLoading(false);
                swipeRefresh.setRefreshing(false);
            }
            @Override public void onError(Exception e) {
                Toast.makeText(AdminProfilesActivity.this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                updateEmptyState(0);
            }
        });
    }

    private void filterProfiles(String query) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allProfiles) {
            Boolean isDisabled = doc.getBoolean("isDisabled");
            boolean disabled = (isDisabled != null && isDisabled);
            if (currentFilter.equals("active") && disabled) continue;
            if (currentFilter.equals("disabled") && !disabled) continue;
            if (!query.isEmpty()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String searchable = ((name != null ? name : "") + " " + (email != null ? email : "") + " " + doc.getId()).toLowerCase();
                if (!searchable.contains(query.toLowerCase())) continue;
            }
            filtered.add(doc);
        }
        adapter.updateList(filtered);
        updateEmptyState(filtered.size());
        tvProfileCount.setText(filtered.size() + " profile" + (filtered.size() != 1 ? "s" : ""));
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState(int count) {
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    @Override public void onProfileClick(DocumentSnapshot profileDoc) {
        AdminProfileDetailDialog dialog = AdminProfileDetailDialog.newInstance(profileDoc.getId());
        dialog.setOnProfileRemovedListener(this::loadProfiles);
        dialog.show(getSupportFragmentManager(), "profile_detail");
    }

    @Override public void onProfileRemoveClick(DocumentSnapshot profileDoc) {
        new AlertDialog.Builder(this)
                .setTitle("Disable Profile")
                .setMessage("Are you sure you want to disable \"" + profileDoc.getString("name") + "\"?\n\nThis user will no longer be able to join waiting lists.")
                .setPositiveButton("Disable", (dialog, which) -> {
                    String adminId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    repository.softDeleteProfile(profileDoc.getId(), adminId, new FirebaseRepository.OnOperationCompleteListener() {
                        @Override public void onSuccess() { Toast.makeText(AdminProfilesActivity.this, "Profile disabled", Toast.LENGTH_SHORT).show(); loadProfiles(); }
                        @Override public void onError(Exception e) { Toast.makeText(AdminProfilesActivity.this, "Failed to disable profile", Toast.LENGTH_LONG).show(); }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }
}
