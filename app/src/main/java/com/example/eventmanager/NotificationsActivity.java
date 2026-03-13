package com.example.eventmanager;

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
 *
 * Responsibilities:
 * - Identifies the current user via DeviceAuthManager
 * - Attaches a real-time Firestore listener via NotificationRepository
 * - Passes live data to NotificationAdapter for rendering
 * - Switches between empty state and list depending on data
 * - Marks notifications as read when tapped
 * - Cleans up the Firestore listener when the Activity is destroyed
 */
public class NotificationsActivity extends AppCompatActivity
        implements NotificationAdapter.OnNotificationClickListener {

    private NotificationRepository notificationRepository;
    private NotificationAdapter adapter;
    private ListenerRegistration listenerRegistration;

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;   // The "No Notifications" view

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // --- Identify the current user ---
        deviceId = new DeviceAuthManager().getDeviceId(this);

        // --- Wire up views ---
        recyclerView     = findViewById(R.id.recycler_notifications);
        emptyStateLayout = findViewById(R.id.layout_empty_state);

        // --- Set up RecyclerView ---
        adapter = new NotificationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // --- Initialize repository ---
        notificationRepository = new NotificationRepository();

        // --- Attach real-time Firestore listener ---
        // Store the registration so we can remove it in onDestroy.
        listenerRegistration = notificationRepository.listenForNotifications(
                deviceId,
                this::onNotificationsUpdated   // method reference to the handler below
        );
    }

    /**
     * Called automatically every time the Firestore notification list changes.
     * Updates the adapter and toggles between empty state and list.
     *
     * @param notifications The current full list of notifications from Firestore.
     */
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
     * Called by NotificationAdapter when the user taps a notification card.
     * Marks the notification as read in Firestore, which triggers the listener
     * to refresh the list and update the envelope icon automatically.
     *
     * @param notification The notification that was tapped.
     */
    @Override
    public void onNotificationClicked(Notification notification) {
        if (!notification.isRead()) {
            notificationRepository.markAsRead(deviceId, notification.getNotificationId());
        }
    }

    /**
     * Removes the Firestore listener when the Activity is destroyed.
     * This is critical — without this, the listener keeps running in the
     * background and causes memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}