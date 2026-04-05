package com.example.eventmanager.adapter;

import com.example.eventmanager.R;

import com.example.eventmanager.models.Notification;
import com.example.eventmanager.ui.NotificationsActivity;
import com.example.eventmanager.models.Event;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the notifications panel.
 * Binds a List<Notification> to individual notification card views.
 *
 * Each card shows:
 * - An envelope icon (filled = unread, outlined = read)
 * - Notification title and event name
 * - Formatted timestamp
 *
 * Notifies NotificationsActivity via OnNotificationClickListener
 * when a card is tapped, so the Activity can call markAsRead().
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    /**
     * Callback interface so the Activity knows which notification was tapped.
     */
    public interface OnNotificationClickListener {
        void onNotificationClicked(Notification notification);
    }

    private List<Notification> notifications = new ArrayList<>();
    private final OnNotificationClickListener clickListener;

    /**
     * @param clickListener The Activity that will handle tap events.
     */
    public NotificationAdapter(OnNotificationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * Replaces the current list with a fresh one from Firestore and redraws.
     * Called by NotificationsActivity every time the real-time listener fires.
     *
     * @param newNotifications The updated list from Firestore.
     */
    public void updateData(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, clickListener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    // -------------------------------------------------------------------------
    // VIEW HOLDER
    // -------------------------------------------------------------------------

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconEnvelope;
        private final TextView textTitle;
        private final TextView textEventName;
        private final TextView textTimestamp;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconEnvelope  = itemView.findViewById(R.id.icon_envelope);
            textTitle     = itemView.findViewById(R.id.text_notification_title);
            textEventName = itemView.findViewById(R.id.text_event_name);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
        }

        void bind(Notification notification, OnNotificationClickListener clickListener) {

            textTitle.setText(notification.getTitle());
            textEventName.setText(notification.getEventName());

            // Format the Firestore Timestamp into a readable date string
            if (notification.getTimestamp() != null) {
                Date date = notification.getTimestamp().toDate();
                String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(date);
                textTimestamp.setText(formatted);
            }

            // Filled envelope = unread, outlined = read
            // Replace these with your actual drawable resource names
            if (notification.isRead()) {
                iconEnvelope.setImageResource(R.drawable.ic_envelope_filled);
            } else {
                iconEnvelope.setImageResource(R.drawable.ic_envelope_filled);
            }

            // Bold title for unread notifications
            if (!notification.isRead()) {
                textTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                textTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            // Tapping the card marks it as read via the Activity
            itemView.setOnClickListener(v -> clickListener.onNotificationClicked(notification));
        }
    }
}