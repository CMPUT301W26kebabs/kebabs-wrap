package com.example.eventmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    private List<Event> eventList;
    private OnItemClickListener clickListener;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.eventName.setText(event.getName() != null ? event.getName() : "Unnamed Event");

        String date = "";
        if (event.getRegistrationStart() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault());
            date = sdf.format(event.getRegistrationStart());
        }
        Long cap = (long) event.getCapacity();
        holder.eventDate.setText(date + " • " + cap + " entrants");

        holder.eventStatus.setText("Open");

        String posterUrl = event.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(posterUrl).centerCrop().into(holder.eventPoster);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(event);
        });
    }

    @Override
    public int getItemCount() { return eventList != null ? eventList.size() : 0; }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventDate, eventStatus, eventLocation;
        ImageView eventPoster;

        EventViewHolder(View v) {
            super(v);
            eventName = v.findViewById(R.id.text_item_event_name);
            eventDate = v.findViewById(R.id.text_item_event_date);
            eventStatus = v.findViewById(R.id.text_item_event_status);
            eventLocation = v.findViewById(R.id.text_item_event_location);
            eventPoster = v.findViewById(R.id.image_event_poster);
        }
    }
}
