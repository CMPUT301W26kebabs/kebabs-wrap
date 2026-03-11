package com.example.eventmanager.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.eventmanager.R;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;

public class AdminProfileDetailDialog extends BottomSheetDialogFragment {

    private static final String ARG_DEVICE_ID = "device_id";
    private String deviceId;
    private FirebaseRepository repository;
    private Runnable onProfileRemovedListener;

    private TextView tvInitials, tvName, tvEmail, tvPhone, tvDeviceId, tvRoleBadge, tvStatus;
    private LinearLayout statusBadge;
    private Button btnDisable, btnRestore;

    public static AdminProfileDetailDialog newInstance(String deviceId) {
        AdminProfileDetailDialog dialog = new AdminProfileDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnProfileRemovedListener(Runnable listener) { this.onProfileRemovedListener = listener; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_profile_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = FirebaseRepository.getInstance();
        deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID) : "";
        tvInitials = view.findViewById(R.id.tv_avatar_initials);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvDeviceId = view.findViewById(R.id.tv_device_id);
        tvRoleBadge = view.findViewById(R.id.tv_role_badge);
        tvStatus = view.findViewById(R.id.tv_status);
        statusBadge = view.findViewById(R.id.status_badge);
        btnDisable = view.findViewById(R.id.btn_disable_profile);
        btnRestore = view.findViewById(R.id.btn_restore_profile);
        btnDisable.setOnClickListener(v -> confirmDisable());
        btnRestore.setOnClickListener(v -> restoreProfile());
        loadProfileData();
    }

    private void loadProfileData() {
        repository.fetchProfileById(deviceId, new FirebaseRepository.OnDocumentLoadedListener() {
            @Override public void onLoaded(DocumentSnapshot doc) { if (doc.exists() && isAdded()) populateUI(doc); }
            @Override public void onError(Exception e) { if (isAdded()) { Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show(); dismiss(); } }
        });
    }

    private void populateUI(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String email = doc.getString("email");
        String phone = doc.getString("phoneNumber");
        tvName.setText(name != null ? name : "No Name");
        tvEmail.setText(email != null ? email : "No email");
        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone");
        tvDeviceId.setText("Device: " + doc.getId());

        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            tvInitials.setText(parts.length >= 2 ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase() : ("" + parts[0].charAt(0)).toUpperCase());
        } else { tvInitials.setText("?"); }

        Boolean isAdmin = doc.getBoolean("isAdmin");
        Boolean isOrganizer = doc.getBoolean("isOrganizer");
        if (isAdmin != null && isAdmin) { tvRoleBadge.setText("ADMIN"); tvRoleBadge.setBackgroundResource(R.drawable.bg_chip_admin); }
        else if (isOrganizer != null && isOrganizer) { tvRoleBadge.setText("ORGANIZER"); tvRoleBadge.setBackgroundResource(R.drawable.bg_chip_organizer); }
        else { tvRoleBadge.setText("ENTRANT"); tvRoleBadge.setBackgroundResource(R.drawable.bg_chip); }
        tvRoleBadge.setVisibility(View.VISIBLE);

        Boolean isDisabled = doc.getBoolean("isDisabled");
        boolean disabled = (isDisabled != null && isDisabled);
        if (disabled) {
            tvStatus.setText("DISABLED"); statusBadge.setBackgroundResource(R.drawable.bg_chip_danger);
            btnDisable.setVisibility(View.GONE); btnRestore.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText("ACTIVE"); statusBadge.setBackgroundResource(R.drawable.bg_chip_active);
            btnDisable.setVisibility(View.VISIBLE); btnRestore.setVisibility(View.GONE);
        }
    }

    private void confirmDisable() {
        new AlertDialog.Builder(requireContext()).setTitle("Disable Profile").setMessage("This user will no longer be able to join waiting lists. Continue?")
                .setPositiveButton("Disable", (d, w) -> {
                    String adminId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    repository.softDeleteProfile(deviceId, adminId, new FirebaseRepository.OnOperationCompleteListener() {
                        @Override public void onSuccess() { if (isAdded()) { Toast.makeText(getContext(), "Profile disabled", Toast.LENGTH_SHORT).show(); if (onProfileRemovedListener != null) onProfileRemovedListener.run(); dismiss(); } }
                        @Override public void onError(Exception e) { if (isAdded()) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
                    });
                }).setNegativeButton("Cancel", null).show();
    }

    private void restoreProfile() {
        repository.restoreProfile(deviceId, new FirebaseRepository.OnOperationCompleteListener() {
            @Override public void onSuccess() { if (isAdded()) { Toast.makeText(getContext(), "Profile restored", Toast.LENGTH_SHORT).show(); if (onProfileRemovedListener != null) onProfileRemovedListener.run(); dismiss(); } }
            @Override public void onError(Exception e) { if (isAdded()) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
}
