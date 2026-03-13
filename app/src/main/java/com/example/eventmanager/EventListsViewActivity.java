package com.example.eventmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EventListsViewActivity extends AppCompatActivity {

    private AnasFirebaseRepo repository;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.makeshift_lists_activity);

        repository = new AnasFirebaseRepo();
        eventId = getIntent().getStringExtra("EVENT_ID");

        TabHost tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec waitingTab = tabHost.newTabSpec("waiting");
        waitingTab.setIndicator("Waiting List");
        waitingTab.setContent(R.id.tabWaiting);
        tabHost.addTab(waitingTab);

        TabHost.TabSpec chosenTab = tabHost.newTabSpec("chosen");
        chosenTab.setIndicator("Chosen");
        chosenTab.setContent(R.id.tabChosen);
        tabHost.addTab(chosenTab);

        RecyclerView rvWaitingList = findViewById(R.id.rvWaitingList);
        rvWaitingList.setLayoutManager(new LinearLayoutManager(this));
        TextView tvWaitingEmpty = findViewById(R.id.tvWaitingEmpty);
        tvWaitingEmpty.setVisibility(View.VISIBLE);

        RecyclerView rvChosenList = findViewById(R.id.rvChosenList);
        rvChosenList.setLayoutManager(new LinearLayoutManager(this));
        TextView tvChosenEmpty = findViewById(R.id.tvChosenEmpty);
        tvChosenEmpty.setVisibility(View.VISIBLE);

        if (eventId != null) {
            repository.getWaitingList(eventId, new AnasFirebaseRepo.StatusCallback() {
                @Override public void onSuccess(String msg) { tvWaitingEmpty.setText(msg); }
                @Override public void onFailure(String err) { tvWaitingEmpty.setText("Error: " + err); }
            });
            repository.getChosenEntrants(eventId, new AnasFirebaseRepo.StatusCallback() {
                @Override public void onSuccess(String msg) { tvChosenEmpty.setText(msg); }
                @Override public void onFailure(String err) { tvChosenEmpty.setText("Error: " + err); }
            });
        }
    }
}
