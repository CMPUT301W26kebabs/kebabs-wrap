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

import com.example.eventmanager.R;
import com.example.eventmanager.adapter.EventAdapter;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvEventCount;
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

        recyclerView = findViewById(R.id.rv_events);
        searchBar = findViewById(R.id.et_search);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        tvEventCount = findViewById(R.id.tv_event_count);
        chipAll = findViewById(R.id.chip_all);
        chipActive = findViewById(R.id.chip_active);
        chipDeleted = findViewById(R.id.chip_deleted);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        adapter = new EventAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { filterEvents(s.toString().trim()); }
        });

        chipActive.setChecked(true);
        View.OnClickListener cl = v -> {
            chipAll.setChecked(false); chipActive.setChecked(false); chipDeleted.setChecked(false);
            ((Chip) v).setChecked(true);
            if (v.getId() == R.id.chip_all) currentFilter = "all";
            else if (v.getId() == R.id.chip_active) currentFilter = "active";
            else if (v.getId() == R.id.chip_deleted) currentFilter = "deleted";
            filterEvents(searchBar.getText().toString().trim());
        };
        chipAll.setOnClickListener(cl); chipActive.setOnClickListener(cl); chipDeleted.setOnClickListener(cl);

        loadEvents();
    }

    private void loadEvents() {
        progressBar.setVisibility(View.VISIBLE); recyclerView.setVisibility(View.GONE);
        repository.fetchAllEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
            public void onLoaded(List<DocumentSnapshot> docs) {
                allEvents = docs; filterEvents(searchBar.getText().toString().trim());
                progressBar.setVisibility(View.GONE); recyclerView.setVisibility(View.VISIBLE);
            }
            public void onError(Exception e) {
                Toast.makeText(AdminEventsActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE); emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filterEvents(String query) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allEvents) {
            Boolean isDel = doc.getBoolean("isDeleted"); boolean del = (isDel != null && isDel);
            if (currentFilter.equals("active") && del) continue;
            if (currentFilter.equals("deleted") && !del) continue;
            if (!query.isEmpty()) {
                String n = doc.getString("name"); String d = doc.getString("description");
                String s = ((n!=null?n:"")+" "+doc.getId()+" "+(d!=null?d:"")).toLowerCase();
                if (!s.contains(query.toLowerCase())) continue;
            }
            filtered.add(doc);
        }
        adapter.updateList(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        tvEventCount.setText(filtered.size() + " event" + (filtered.size() != 1 ? "s" : ""));
        tvEventCount.setVisibility(View.VISIBLE);
    }

    @Override public void onEventClick(DocumentSnapshot doc) {
        AdminEventDetailDialog d = AdminEventDetailDialog.newInstance(doc.getId());
        d.setOnEventRemovedListener(this::loadEvents);
        d.show(getSupportFragmentManager(), "event_detail");
    }

    @Override public void onEventRemoveClick(DocumentSnapshot doc) {
        new AlertDialog.Builder(this).setTitle("Remove Event")
            .setMessage("Remove \"" + doc.getString("name") + "\"?\nThis hides it from all users.")
            .setPositiveButton("Remove", (dg, w) -> {
                String aid = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                repository.softDeleteEvent(doc.getId(), aid, new FirebaseRepository.OnOperationCompleteListener() {
                    public void onSuccess() { Toast.makeText(AdminEventsActivity.this, "Event removed", Toast.LENGTH_SHORT).show(); loadEvents(); }
                    public void onError(Exception e) { Toast.makeText(AdminEventsActivity.this, "Failed", Toast.LENGTH_LONG).show(); }
                });
            }).setNegativeButton("Cancel", null).show();
    }
}
