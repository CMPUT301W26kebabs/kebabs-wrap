package com.example.eventmanager.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.eventmanager.adapter.EventAdapter;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String TAG = "AdminEventsActivity";
    private RecyclerView recyclerView;
    private EditText searchBar;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvEventCount;
    private SwipeRefreshLayout swipeRefresh;
    private Chip chipAll, chipActive, chipDeleted;
    private EventAdapter adapter;
    private List<DocumentSnapshot> allEvents = new ArrayList<>();
    private FirebaseRepository repository;
    private String currentFilter = "active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_events);
        repository = FirebaseRepository.getInstance();
        initViews();
        setupRecyclerView();
        setupSearch();
        setupChipFilters();
        setupSwipeRefresh();
        loadEvents();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_events);
        searchBar = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        tvEventCount = findViewById(R.id.tv_event_count);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        chipAll = findViewById(R.id.chip_all);
        chipActive = findViewById(R.id.chip_active);
        chipDeleted = findViewById(R.id.chip_deleted);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filterEvents(s.toString().trim()); }
        });
    }

    private void setupChipFilters() {
        chipActive.setChecked(true);
        View.OnClickListener chipListener = v -> {
            chipAll.setChecked(false);
            chipActive.setChecked(false);
            chipDeleted.setChecked(false);
            ((Chip) v).setChecked(true);
            if (v.getId() == R.id.chip_all) currentFilter = "all";
            else if (v.getId() == R.id.chip_active) currentFilter = "active";
            else if (v.getId() == R.id.chip_deleted) currentFilter = "deleted";
            filterEvents(searchBar.getText().toString().trim());
        };
        chipAll.setOnClickListener(chipListener);
        chipActive.setOnClickListener(chipListener);
        chipDeleted.setOnClickListener(chipListener);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.admin_primary, R.color.admin_accent);
        swipeRefresh.setOnRefreshListener(this::loadEvents);
    }

    private void loadEvents() {
        showLoading(true);
        repository.fetchAllEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
            @Override public void onLoaded(List<DocumentSnapshot> documents) {
                allEvents = documents;
                filterEvents(searchBar.getText().toString().trim());
                showLoading(false);
                swipeRefresh.setRefreshing(false);
            }
            @Override public void onError(Exception e) {
                Toast.makeText(AdminEventsActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                updateEmptyState(0);
            }
        });
    }

    private void filterEvents(String query) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allEvents) {
            Boolean isDeleted = doc.getBoolean("isDeleted");
            boolean deleted = (isDeleted != null && isDeleted);
            if (currentFilter.equals("active") && deleted) continue;
            if (currentFilter.equals("deleted") && !deleted) continue;
            if (!query.isEmpty()) {
                String name = doc.getString("name");
                String eventId = doc.getId();
                String desc = doc.getString("description");
                String searchable = ((name != null ? name : "") + " " + eventId + " " + (desc != null ? desc : "")).toLowerCase();
                if (!searchable.contains(query.toLowerCase())) continue;
            }
            filtered.add(doc);
        }
        adapter.updateList(filtered);
        updateEmptyState(filtered.size());
        tvEventCount.setText(filtered.size() + " event" + (filtered.size() != 1 ? "s" : ""));
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState(int count) {
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    @Override public void onEventClick(DocumentSnapshot eventDoc) {
        AdminEventDetailDialog dialog = AdminEventDetailDialog.newInstance(eventDoc.getId());
        dialog.setOnEventRemovedListener(this::loadEvents);
        dialog.show(getSupportFragmentManager(), "event_detail");
    }

    @Override public void onEventRemoveClick(DocumentSnapshot eventDoc) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Event")
                .setMessage("Are you sure you want to remove \"" + eventDoc.getString("name") + "\"?\n\nThis will hide the event from all users.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    String adminId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    repository.softDeleteEvent(eventDoc.getId(), adminId, new FirebaseRepository.OnOperationCompleteListener() {
                        @Override public void onSuccess() { Toast.makeText(AdminEventsActivity.this, "Event removed", Toast.LENGTH_SHORT).show(); loadEvents(); }
                        @Override public void onError(Exception e) { Toast.makeText(AdminEventsActivity.this, "Failed to remove event", Toast.LENGTH_LONG).show(); }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }
}
