package com.example.eventmanager.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying notification log entries in the admin panel.
 * Each item shows the timestamp, organizer name, recipient group, and message body.
 *
 * This is a read-only audit trail — there are no delete or edit actions.
 */
public class AdminNotificationLogsAdapter
        extends RecyclerView.Adapter<AdminNotificationLogsAdapter.LogVH> {

    private final List<AdminNotificationLogItem> items = new ArrayList<>();

    /** Replaces the full list and refreshes the UI. */
    public void setItems(List<AdminNotificationLogItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification, parent, false);
        return new LogVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogVH holder, int position) {
        AdminNotificationLogItem item = items.get(position);
        holder.tvTimestamp.setText(item.formattedTimestamp);
        holder.tvOrganizer.setText("Organizer: " + item.organizerName);
        holder.tvRecipient.setText("→ " + item.recipientGroup);
        holder.tvMessage.setText(item.message);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LogVH extends RecyclerView.ViewHolder {
        final TextView tvTimestamp;
        final TextView tvOrganizer;
        final TextView tvRecipient;
        final TextView tvMessage;

        LogVH(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvOrganizer = itemView.findViewById(R.id.tv_organizer);
            tvRecipient = itemView.findViewById(R.id.tv_recipient);
            tvMessage   = itemView.findViewById(R.id.tv_message);
        }
    }
}
