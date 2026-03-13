package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Displays the in-app notification panel for the current user.
 * On tap:
 *   - Winner notifications (eventId != null) → launch AcceptDeclineActivity
 *   - Informational notifications (eventId == null) → mark as read only
 */
public class NotificationsActivity extends AppCompatActivity
        implements NotificationAdapter.OnNotificationClickListener {

    private NotificationRepository notificationRepository;
    private NotificationAdapter adapter;
    private ListenerRegistration listenerRegistration;

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        deviceId = new DeviceAuthManager().getDeviceId(this);

        recyclerView     = findViewById(R.id.recycler_notifications);
        emptyStateLayout = findViewById(R.id.layout_empty_state);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        adapter = new NotificationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        notificationRepository = new NotificationRepository();

        listenerRegistration = notificationRepository.listenForNotifications(
                deviceId,
                this::onNotificationsUpdated
        );
    }

    private void onNotificationsUpdated(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            adapter.updateData(notifications);
        }
    }

    /**
     * Called when a notification card is tapped.
     *
     * If the notification has an eventId, it is a winner notification —
     * launch AcceptDeclineActivity so the user can respond.
     *
     * If eventId is null, it is an informational notification —
     * just mark it as read.
     */
    @Override
    public void onNotificationClicked(Notification notification) {

        // Always mark as read when tapped
        if (!notification.isRead()) {
            notificationRepository.markAsRead(deviceId, notification.getNotificationId());
        }

        // Route winner notifications to the accept/decline screen
        if (notification.getEventId() != null) {
            Intent intent = new Intent(this, AcceptDeclineActivity.class);
            intent.putExtra("eventId", notification.getEventId());
            intent.putExtra("eventName", notification.getEventName());
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("notificationId", notification.getNotificationId());
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}