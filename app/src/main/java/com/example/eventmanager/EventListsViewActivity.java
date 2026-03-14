package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class EventListsViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent manageIntent = new Intent(this, ManageEventActivity.class);
        manageIntent.putExtra("EVENT_ID", getIntent().getStringExtra("EVENT_ID"));
        manageIntent.putExtra("EVENT_NAME", getIntent().getStringExtra("EVENT_NAME"));
        startActivity(manageIntent);
        finish();
    }
}
