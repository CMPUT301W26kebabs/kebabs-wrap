package com.example.eventmanager.admin;
import com.example.eventmanager.models.Event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.eventmanager.repository.FirebaseRepository;
import com.example.eventmanager.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Bottom-sheet dialog displayed when an admin taps an event card.
 * Shows event details (title, date, poster) and provides soft-delete and restore actions.
 */
public class AdminEventDetailDialog extends BottomSheetDialogFragment {

    private static final String ARG_EVENT_ID = "event_id";
    private String eventId;
    private FirebaseRepository repository;
    private Runnable onEventRemovedListener;

    private ImageView ivPoster;
    private TextView tvName, tvDescription, tvEventId, tvOrganizer, tvCapacity, tvWaitlistCap, tvRegStart, tvRegEnd, tvStatus;
    private LinearLayout statusBadge;
    private Button btnRemove, btnRestore;

    public static AdminEventDetailDialog newInstance(String eventId) {
        AdminEventDetailDialog d = new AdminEventDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        d.setArguments(args);
        return d;
    }

    public void setOnEventRemovedListener(Runnable l) { this.onEventRemovedListener = l; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = FirebaseRepository.getInstance();
        eventId = getArguments() != null ? getArguments().getString(ARG_EVENT_ID) : "";
        ivPoster = view.findViewById(R.id.iv_event_poster);
        tvName = view.findViewById(R.id.tv_event_name);
        tvDescription = view.findViewById(R.id.tv_event_description);
        tvEventId = view.findViewById(R.id.tv_event_id);
        tvOrganizer = view.findViewById(R.id.tv_organizer_id);
        tvCapacity = view.findViewById(R.id.tv_capacity);
        tvWaitlistCap = view.findViewById(R.id.tv_waitlist_cap);
        tvRegStart = view.findViewById(R.id.tv_reg_start);
        tvRegEnd = view.findViewById(R.id.tv_reg_end);
        tvStatus = view.findViewById(R.id.tv_status);
        statusBadge = view.findViewById(R.id.status_badge);
        btnRemove = view.findViewById(R.id.btn_remove_event);
        btnRestore = view.findViewById(R.id.btn_restore_event);
        btnRemove.setOnClickListener(v -> confirmRemove());
        btnRestore.setOnClickListener(v -> restoreEvent());
        loadEventData();
    }

    private void loadEventData() {
        repository.fetchEventById(eventId, new FirebaseRepository.OnDocumentLoadedListener() {
            public void onLoaded(DocumentSnapshot doc) { if (doc.exists() && isAdded()) populateUI(doc); }
            public void onError(Exception e) { if (isAdded()) { Toast.makeText(getContext(), "Failed to load event", Toast.LENGTH_SHORT).show(); dismiss(); } }
        });
    }

    private void populateUI(DocumentSnapshot doc) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        tvName.setText(doc.getString("name") != null ? doc.getString("name") : "Unnamed Event");
        tvDescription.setText(doc.getString("description") != null ? doc.getString("description") : "No description");
        tvEventId.setText("ID: " + doc.getId());
        tvOrganizer.setText("Organizer: " + (doc.getString("organizerId") != null ? doc.getString("organizerId") : "N/A"));
        Long cap = doc.getLong("capacity");
        tvCapacity.setText("Capacity: " + (cap != null ? cap : "Unlimited"));
        Long wl = doc.getLong("maxWaitlistCapacity");
        tvWaitlistCap.setText("Waitlist Limit: " + (wl != null ? wl : "Unlimited"));
        Timestamp rs = doc.getTimestamp("registrationStart");
        tvRegStart.setText("Opens: " + (rs != null ? sdf.format(rs.toDate()) : "N/A"));
        Timestamp re = doc.getTimestamp("registrationEnd");
        tvRegEnd.setText("Closes: " + (re != null ? sdf.format(re.toDate()) : "N/A"));

        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty() && isAdded()) {
            ivPoster.setVisibility(View.VISIBLE);
            Glide.with(this).load(posterUrl).placeholder(R.drawable.ic_event_placeholder).centerCrop().into(ivPoster);
        } else { ivPoster.setVisibility(View.GONE); }

        Boolean isDel = doc.getBoolean("isDeleted"); boolean del = (isDel != null && isDel);
        if (del) {
            tvStatus.setText("REMOVED"); statusBadge.setBackgroundResource(R.drawable.bg_chip_danger);
            btnRemove.setVisibility(View.GONE); btnRestore.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText("ACTIVE"); statusBadge.setBackgroundResource(R.drawable.bg_chip_active);
            btnRemove.setVisibility(View.VISIBLE); btnRestore.setVisibility(View.GONE);
        }
    }

    private void confirmRemove() {
        new AlertDialog.Builder(requireContext()).setTitle("Remove Event").setMessage("Hide this event from all users?")
            .setPositiveButton("Remove", (d, w) -> {
                String aid = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                repository.softDeleteEvent(eventId, aid, new FirebaseRepository.OnOperationCompleteListener() {
                    public void onSuccess() { if (isAdded()) { Toast.makeText(getContext(), "Event removed", Toast.LENGTH_SHORT).show(); if (onEventRemovedListener != null) onEventRemovedListener.run(); dismiss(); } }
                    public void onError(Exception e) { if (isAdded()) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
                });
            }).setNegativeButton("Cancel", null).show();
    }

    private void restoreEvent() {
        repository.restoreEvent(eventId, new FirebaseRepository.OnOperationCompleteListener() {
            public void onSuccess() { if (isAdded()) { Toast.makeText(getContext(), "Event restored", Toast.LENGTH_SHORT).show(); if (onEventRemovedListener != null) onEventRemovedListener.run(); dismiss(); } }
            public void onError(Exception e) { if (isAdded()) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
}
