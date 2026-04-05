package com.example.eventmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class EntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String eventId;
    private String eventName;
    private GoogleMap map;
    private FirebaseFirestore db;

    private View loadingOverlay;
    private View cardBottomInfo;
    private View cardEmptyState;
    private TextView tvEntrantCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_map);

        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");
        db = FirebaseFirestore.getInstance();

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event information.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvEventTitle = findViewById(R.id.tvEventTitle);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        cardBottomInfo = findViewById(R.id.cardBottomInfo);
        cardEmptyState = findViewById(R.id.cardEmptyState);
        tvEntrantCount = findViewById(R.id.tvEntrantCount);

        btnBack.setOnClickListener(v -> finish());
        tvEventTitle.setText(eventName != null && !eventName.trim().isEmpty() ? eventName : "Event");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment == null) {
            Toast.makeText(this, "Could not load map.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        loadEntrantLocations();
    }

    private void loadEntrantLocations() {
        setLoading(true);
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<EntrantPin> pins = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        EntrantPin pin = toPin(doc);
                        if (pin != null) {
                            pins.add(pin);
                        }
                    }
                    renderPins(pins);
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to load entrant locations.", Toast.LENGTH_LONG).show();
                    showEmptyState();
                });
    }

    private EntrantPin toPin(DocumentSnapshot doc) {
        GeoPoint geoPoint = doc.getGeoPoint("location");
        Double latitude = doc.getDouble("latitude");
        Double longitude = doc.getDouble("longitude");

        double lat;
        double lng;
        if (geoPoint != null) {
            lat = geoPoint.getLatitude();
            lng = geoPoint.getLongitude();
        } else if (latitude != null && longitude != null) {
            lat = latitude;
            lng = longitude;
        } else {
            return null;
        }

        String name = doc.getString("name");
        String label = (name != null && !name.trim().isEmpty()) ? name.trim() : "Entrant";
        return new EntrantPin(label, lat, lng);
    }

    private void renderPins(List<EntrantPin> pins) {
        if (map == null) return;
        map.clear();
        tvEntrantCount.setText(String.valueOf(pins.size()));

        if (pins.isEmpty()) {
            showEmptyState();
            return;
        }

        cardBottomInfo.setVisibility(View.VISIBLE);
        cardEmptyState.setVisibility(View.GONE);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (EntrantPin pin : pins) {
            LatLng latLng = new LatLng(pin.latitude, pin.longitude);
            map.addMarker(new MarkerOptions().position(latLng).title(pin.name));
            boundsBuilder.include(latLng);
        }

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 160));
    }

    private void showEmptyState() {
        cardBottomInfo.setVisibility(View.GONE);
        cardEmptyState.setVisibility(View.VISIBLE);
        if (map != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(53.5461, -113.4938), 10f));
        }
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private static class EntrantPin {
        final String name;
        final double latitude;
        final double longitude;

        EntrantPin(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
