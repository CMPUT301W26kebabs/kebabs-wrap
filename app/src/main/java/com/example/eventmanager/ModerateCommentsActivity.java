package com.example.eventmanager; // Make sure this matches your package name!

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ModerateCommentsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This links to the beautiful XML you just built
        setContentView(R.layout.activity_moderate_comments);
    }
}
