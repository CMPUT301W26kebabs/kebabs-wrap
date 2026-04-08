package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.eventmanager.R;
import com.example.eventmanager.adapter.AvatarOptionAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a grid of DiceBear-generated avatar options for the user to choose from.
 * Returns the selected avatar URL via {@link #EXTRA_AVATAR_URL} in the result intent.
 */
public class AvatarPickerActivity extends AppCompatActivity {

    public static final String EXTRA_AVATAR_URL = "avatar_url";
    public static final String EXTRA_USER_NAME = "user_name";

    private static final String BASE_URL = "https://api.dicebear.com/7.x/";
    private static final String FORMAT = "/png?size=200&seed=";

    private static final String[] STYLES = {
            "adventurer", "adventurer-neutral", "avataaars", "avataaars-neutral",
            "big-ears", "big-ears-neutral", "big-smile", "bottts", "bottts-neutral",
            "croodles", "croodles-neutral", "fun-emoji", "icons",
            "lorelei", "lorelei-neutral", "micah", "miniavs",
            "notionists", "notionists-neutral", "open-peeps",
            "personas", "pixel-art", "pixel-art-neutral", "thumbs"
    };

    private ShapeableImageView ivPreview;
    private MaterialButton btnConfirm;
    private String selectedUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_picker);

        String userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        if (userName == null || userName.isEmpty()) userName = "User";
        String seed = userName.replace(" ", "+");

        ivPreview = findViewById(R.id.ivAvatarPreview);
        btnConfirm = findViewById(R.id.btnConfirmAvatar);
        RecyclerView rvAvatars = findViewById(R.id.rvAvatarGrid);

        findViewById(R.id.btnAvatarBack).setOnClickListener(v -> finish());

        List<AvatarOptionAdapter.AvatarOption> options = new ArrayList<>();
        for (String style : STYLES) {
            String url = BASE_URL + style + FORMAT + seed;
            options.add(new AvatarOptionAdapter.AvatarOption(style, url));
        }

        rvAvatars.setLayoutManager(new GridLayoutManager(this, 4));
        AvatarOptionAdapter adapter = new AvatarOptionAdapter(options, option -> {
            selectedUrl = option.url;
            btnConfirm.setEnabled(true);
            Glide.with(this)
                    .load(option.url)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivPreview);
            ivPreview.setVisibility(View.VISIBLE);
        });
        rvAvatars.setAdapter(adapter);

        btnConfirm.setEnabled(false);
        btnConfirm.setOnClickListener(v -> {
            if (selectedUrl == null) {
                Toast.makeText(this, "Select an avatar first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent result = new Intent();
            result.putExtra(EXTRA_AVATAR_URL, selectedUrl);
            setResult(RESULT_OK, result);
            finish();
        });
    }
}
