package com.example.eventmanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying user search results in the Invite Guests screen.
 * Each item shows the user's name, contact info, and two action buttons:
 *  - Invite to waiting list (green +)
 *  - Assign as co-organizer (purple)
 */
public class GuestInviteAdapter extends RecyclerView.Adapter<GuestInviteAdapter.GuestVH> {

    private List<DocumentSnapshot> users = new ArrayList<>();
    private OnGuestActionListener listener;

    public interface OnGuestActionListener {
        void onInvite(DocumentSnapshot userDoc);
        void onAssignCoOrganizer(DocumentSnapshot userDoc);
    }

    /**
     * Constructs the adapter with a callback interface to handle action clicks.
     *
     * @param listener The interface listening for click interactions on the item buttons.
     */
    public GuestInviteAdapter(OnGuestActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the currently displayed active dataset and refreshes the RecyclerView.
     *
     * @param newList The new list of Firestore DocumentSnapshots to iterate over.
     */
    public void updateList(List<DocumentSnapshot> newList) {
        this.users = newList;
        notifyDataSetChanged();
    }

    /**
     * Inflates the specific guest selection layout XML layout resource.
     *
     * @param parent   The enclosing container holding the views.
     * @param viewType The standard categorical view type integer.
     * @return Extracted logic encapsulated into the GuestVH inner class layout bounds.
     */
    @NonNull
    @Override
    public GuestVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guest_invite, parent, false);
        return new GuestVH(v);
    }

    /**
     * Evaluates data indices array offsets into visual UI components, initializing formatting logic.
     * Provides initials-parsing validation for blank user records.
     *
     * @param h   The target Guest ViewHolder representing the list iteration boundary constraint.
     * @param pos The integer list offset of the dataset pointer.
     */
    @Override
    public void onBindViewHolder(@NonNull GuestVH h, int pos) {
        DocumentSnapshot doc = users.get(pos);

        String name = doc.getString("name");
        String email = doc.getString("email");
        String phone = doc.getString("phoneNumber");

        h.nameText.setText(name != null && !name.isEmpty() ? name : "Unknown");

        // Show email or phone as contact info
        if (email != null && !email.isEmpty()) {
            h.contactText.setText(email);
        } else if (phone != null && !phone.isEmpty()) {
            h.contactText.setText(phone);
        } else {
            h.contactText.setText("No contact info");
        }

        // Avatar initials
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            String initials;
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else {
                initials = ("" + parts[0].charAt(0)).toUpperCase();
            }
            h.avatarText.setText(initials);
        } else {
            h.avatarText.setText("?");
        }

        h.inviteButton.setOnClickListener(v -> {
            if (listener != null) listener.onInvite(doc);
        });

        h.coOrgButton.setOnClickListener(v -> {
            if (listener != null) listener.onAssignCoOrganizer(doc);
        });
    }

    /**
     * Identifies the precise limit bounding the scrolling dimensions length.
     *
     * @return Raw integer depth representation of rendered list components.
     */
    @Override
    public int getItemCount() {
        return users.size();
    }

    static class GuestVH extends RecyclerView.ViewHolder {
        TextView avatarText, nameText, contactText;
        ImageButton inviteButton, coOrgButton;

        GuestVH(View v) {
            super(v);
            avatarText = v.findViewById(R.id.avatarText);
            nameText = v.findViewById(R.id.guestNameText);
            contactText = v.findViewById(R.id.guestContactText);
            inviteButton = v.findViewById(R.id.inviteGuestButton);
            coOrgButton = v.findViewById(R.id.inviteOrganizerButton);
        }
    }
}
