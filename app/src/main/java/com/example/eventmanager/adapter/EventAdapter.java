package com.example.eventmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmanager.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(DocumentSnapshot eventDoc);
        void onEventRemoveClick(DocumentSnapshot eventDoc);
    }

    private final Context context;
    private List<DocumentSnapshot> events;
    private final OnEventClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public EventAdapter(Context context, List<DocumentSnapshot> events, OnEventClickListener listener) {
        this.context = context;
        this.events = events;
        this.listener = listener;
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        DocumentSnapshot doc = events.get(position);

        String name = doc.getString("name");
        holder.tvName.setText(name != null ? name : "Unnamed Event");

        String eventId = doc.getId();
        holder.tvEventId.setText(eventId.length() > 12 ? eventId.substring(0, 12) + "..." : eventId);

        Timestamp regStart = doc.getTimestamp("registrationStart");
        Timestamp regEnd = doc.getTimestamp("registrationEnd");
        String dateText = "";
        if (regStart != null) dateText += sdf.format(regStart.toDate());
        if (regEnd != null) dateText += " - " + sdf.format(regEnd.toDate());
        holder.tvDateRange.setText(!dateText.isEmpty() ? dateText : "No dates set");

        Long capacity = doc.getLong("capacity");
        holder.tvCapacity.setText(capacity != null ? capacity + " spots" : "Unlimited");

        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            holder.ivPoster.setVisibility(View.VISIBLE);
            Glide.with(context).load(posterUrl).placeholder(R.drawable.ic_event_placeholder).centerCrop().into(holder.ivPoster);
        } else {
            holder.ivPoster.setVisibility(View.GONE);
        }

        Boolean isDeleted = doc.getBoolean("isDeleted");
        boolean deleted = (isDeleted != null && isDeleted);
        if (deleted) {
            holder.statusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusText.setText("REMOVED");
            holder.statusBadge.setBackgroundResource(R.drawable.bg_chip_danger);
            holder.cardRoot.setAlpha(0.6f);
        } else {
            holder.statusBadge.setVisibility(View.GONE);
            holder.cardRoot.setAlpha(1.0f);
        }

        holder.cardRoot.setOnClickListener(v -> listener.onEventClick(doc));
        holder.btnRemove.setOnClickListener(v -> listener.onEventRemoveClick(doc));
        holder.btnRemove.setVisibility(deleted ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return events.size(); }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        ImageView ivPoster;
        TextView tvName, tvEventId, tvDateRange, tvCapacity, tvStatusText;
        LinearLayout statusBadge;
        ImageButton btnRemove;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            ivPoster = itemView.findViewById(R.id.iv_event_poster);
            tvName = itemView.findViewById(R.id.tv_event_name);
            tvEventId = itemView.findViewById(R.id.tv_event_id);
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            tvCapacity = itemView.findViewById(R.id.tv_capacity);
            tvStatusText = itemView.findViewById(R.id.tv_status_text);
            statusBadge = itemView.findViewById(R.id.status_badge);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
