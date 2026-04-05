package com.example.eventmanager.admin;

import android.os.Bundle;
import android.provider.Settings;
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

import com.example.eventmanager.FirebaseRepository;
import com.example.eventmanager.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

public class AdminProfileDetailDialog extends BottomSheetDialogFragment {

    private static final String ARG_DEVICE_ID = "device_id";
    private String deviceId;
    private FirebaseRepository repository;
    private final com.example.eventmanager.repository.FirebaseRepository userDataRepository =
            new com.example.eventmanager.repository.FirebaseRepository();
    private Runnable onProfileRemovedListener;

    private TextView tvInitials, tvName, tvEmail, tvPhone, tvDeviceId, tvRoleBadge, tvStatus;
    private LinearLayout statusBadge;
    private Button btnDisable, btnRestore;

    public static AdminProfileDetailDialog newInstance(String deviceId) {
        AdminProfileDetailDialog d = new AdminProfileDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        d.setArguments(args);
        return d;
    }

    public void setOnProfileRemovedListener(Runnable l) { this.onProfileRemovedListener = l; }

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
        btnDisable.setOnClickListener(v -> confirmDeleteProfile());
        btnRestore.setOnClickListener(v -> restoreProfile());
        loadProfileData();
    }

    private void loadProfileData() {
        repository.fetchProfileById(deviceId, new FirebaseRepository.OnDocumentLoadedListener() {
            public void onLoaded(DocumentSnapshot doc) { if (doc.exists() && isAdded()) populateUI(doc); }
            public void onError(Exception e) { if (isAdded()) { Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show(); dismiss(); } }
        });
    }

    private void populateUI(DocumentSnapshot doc) {
        String name = doc.getString("name");
        tvName.setText(name != null ? name : "No Name");
        tvEmail.setText(doc.getString("email") != null ? doc.getString("email") : "No email");
        String phone = doc.getString("phoneNumber");
        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone");
        tvDeviceId.setText("Device: " + doc.getId());

        if (name != null && !name.isEmpty()) {
            String[] p = name.split(" ");
            tvInitials.setText(p.length >= 2 ? (""+p[0].charAt(0)+p[p.length-1].charAt(0)).toUpperCase() : (""+p[0].charAt(0)).toUpperCase());
        } else { tvInitials.setText("?"); }

        Boolean isAdmin = doc.getBoolean("isAdmin"); Boolean isOrg = doc.getBoolean("isOrganizer");
        if (isAdmin != null && isAdmin) { tvRoleBadge.setText("ADMIN"); tvRoleBadge.setBackgroundResource(R.drawable.bg_chip_admin); }
        else if (isOrg != null && isOrg) { tvRoleBadge.setText("ORGANIZER"); tvRoleBadge.setBackgroundResource(R.drawable.bg_chip_organizer); }
        else { tvRoleBadge.setText("ENTRANT"); tvRoleBadge.setBackgroundResource(R.drawable.bg_chip); }
        tvRoleBadge.setVisibility(View.VISIBLE);

        Boolean isDis = doc.getBoolean("isDisabled"); boolean dis = (isDis != null && isDis);
        if (dis) {
            tvStatus.setText("DISABLED"); statusBadge.setBackgroundResource(R.drawable.bg_chip_danger);
            btnDisable.setVisibility(View.GONE); btnRestore.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText("ACTIVE"); statusBadge.setBackgroundResource(R.drawable.bg_chip_active);
            btnDisable.setVisibility(View.VISIBLE); btnRestore.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteProfile() {
        String myId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (myId != null && myId.equals(deviceId)) {
            Toast.makeText(requireContext(), R.string.admin_cannot_delete_self, Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.admin_delete_profile_title)
                .setMessage(R.string.admin_delete_profile_message)
                .setPositiveButton(R.string.admin_comments_remove_confirm, (d, w) ->
                        userDataRepository.deleteUserAndAllRegistrations(deviceId,
                                new com.example.eventmanager.repository.FirebaseRepository.RepoCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        FirebaseStorage.getInstance().getReference()
                                                .child("users/" + deviceId + "/avatar.jpg")
                                                .delete();
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), R.string.admin_profile_deleted, Toast.LENGTH_SHORT).show();
                                            if (onProfileRemovedListener != null) {
                                                onProfileRemovedListener.run();
                                            }
                                            dismiss();
                                        }
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(),
                                                    e.getMessage() != null ? e.getMessage() : "Delete failed",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void restoreProfile() {
        repository.restoreProfile(deviceId, new FirebaseRepository.OnOperationCompleteListener() {
            public void onSuccess() { if (isAdded()) { Toast.makeText(getContext(), "Profile restored", Toast.LENGTH_SHORT).show(); if (onProfileRemovedListener != null) onProfileRemovedListener.run(); dismiss(); } }
            public void onError(Exception e) { if (isAdded()) Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
}
