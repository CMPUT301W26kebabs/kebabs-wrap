package com.example.eventmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    public interface OnProfileClickListener {
        void onProfileClick(DocumentSnapshot profileDoc);
        void onProfileRemoveClick(DocumentSnapshot profileDoc);
    }

    private final Context context;
    private List<DocumentSnapshot> profiles;
    private final OnProfileClickListener listener;

    public ProfileAdapter(Context context, List<DocumentSnapshot> profiles, OnProfileClickListener listener) {
        this.context = context;
        this.profiles = profiles;
        this.listener = listener;
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.profiles = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile_card, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        DocumentSnapshot doc = profiles.get(position);

        String name = doc.getString("name");
        holder.tvName.setText(name != null ? name : "No Name");

        String email = doc.getString("email");
        holder.tvEmail.setText(email != null ? email : "No email");

        String deviceId = doc.getId();
        holder.tvDeviceId.setText(deviceId.length() > 16 ? deviceId.substring(0, 16) + "..." : deviceId);

        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            String initials = parts.length >= 2
                    ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
                    : ("" + parts[0].charAt(0)).toUpperCase();
            holder.tvInitials.setText(initials);
        } else {
            holder.tvInitials.setText("?");
        }

        Boolean isAdmin = doc.getBoolean("isAdmin");
        Boolean isOrganizer = doc.getBoolean("isOrganizer");
        if (isAdmin != null && isAdmin) {
            holder.tvRole.setText("ADMIN");
            holder.tvRole.setBackgroundResource(R.drawable.bg_chip_admin);
            holder.tvRole.setVisibility(View.VISIBLE);
        } else if (isOrganizer != null && isOrganizer) {
            holder.tvRole.setText("ORGANIZER");
            holder.tvRole.setBackgroundResource(R.drawable.bg_chip_organizer);
            holder.tvRole.setVisibility(View.VISIBLE);
        } else {
            holder.tvRole.setVisibility(View.GONE);
        }

        Boolean isDisabled = doc.getBoolean("isDisabled");
        boolean disabled = (isDisabled != null && isDisabled);
        if (disabled) {
            holder.statusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusText.setText("DISABLED");
            holder.statusBadge.setBackgroundResource(R.drawable.bg_chip_danger);
            holder.cardRoot.setAlpha(0.6f);
        } else {
            holder.statusBadge.setVisibility(View.GONE);
            holder.cardRoot.setAlpha(1.0f);
        }

        holder.cardRoot.setOnClickListener(v -> listener.onProfileClick(doc));
        holder.btnRemove.setOnClickListener(v -> listener.onProfileRemoveClick(doc));
        holder.btnRemove.setVisibility(disabled ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return profiles.size(); }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView tvInitials, tvName, tvEmail, tvDeviceId, tvRole, tvStatusText;
        LinearLayout statusBadge;
        ImageButton btnRemove;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            tvInitials = itemView.findViewById(R.id.tv_avatar_initials);
            tvName = itemView.findViewById(R.id.tv_profile_name);
            tvEmail = itemView.findViewById(R.id.tv_profile_email);
            tvDeviceId = itemView.findViewById(R.id.tv_device_id);
            tvRole = itemView.findViewById(R.id.tv_role_badge);
            tvStatusText = itemView.findViewById(R.id.tv_status_text);
            statusBadge = itemView.findViewById(R.id.status_badge);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
