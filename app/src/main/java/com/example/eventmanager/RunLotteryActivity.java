package com.example.eventmanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eventmanager.models.Entrant;
import java.util.List;

public class RunLotteryActivity extends AppCompatActivity {

    private EditText editTargetCount;
    private Button btnRunLottery;
    private LotteryManager lotteryManager;
    private String eventId = "SAMPLE_EVENT_ID"; // In the real app, pass this via Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_lottery);

        editTargetCount = findViewById(R.id.editTargetCount);
        btnRunLottery = findViewById(R.id.btnRunLottery);
        lotteryManager = new LotteryManager();

        btnRunLottery.setOnClickListener(v -> {
            String countStr = editTargetCount.getText().toString();
            if (countStr.isEmpty()) {
                Toast.makeText(this, "Please enter a number", Toast.LENGTH_SHORT).show();
                return;
            }

            int targetCount = Integer.parseInt(countStr);

            // Call the engine you built!
            lotteryManager.drawWinners(eventId, targetCount, new LotteryCallback() {
                @Override
                public void onSuccess(List<Entrant> selectedWinners) {
                    Toast.makeText(RunLotteryActivity.this, "Successfully drew " + selectedWinners.size() + " winners!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(RunLotteryActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}