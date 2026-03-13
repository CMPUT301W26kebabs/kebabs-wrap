package com.example.eventmanager; // Ensure this matches your package name!

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

    private List<Event> eventList;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your item_event_card.xml layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        // Bind the text data
        holder.eventName.setText(event.getName());

        // Mocking date formatting for now. You can format actual dates based on your Event model.
        holder.eventDate.setText("Capacity: " + event.getCapacity());
        holder.eventLocation.setText("Tap for details"); // Replace with event.getLocation() if you added it
        holder.eventStatus.setText("Open");

        // Use Glide to load the poster image smoothly
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(holder.eventPoster);
        } else {
            // Fallback if no poster is uploaded
            holder.eventPoster.setImageResource(R.drawable.placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Updates the list and refreshes the RecyclerView.
     */
    public void updateData(List<Event> newEventList) {
        this.eventList.clear();
        this.eventList.addAll(newEventList);
        notifyDataSetChanged();
    }

    // ViewHolder class holds the UI elements for a single list item
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventDate, eventStatus, eventLocation;
        ImageView eventPoster;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.text_item_event_name);
            eventDate = itemView.findViewById(R.id.text_item_event_date);
            eventStatus = itemView.findViewById(R.id.text_item_event_status);
            eventLocation = itemView.findViewById(R.id.text_item_event_location);
            eventPoster = itemView.findViewById(R.id.image_event_poster);
        }
    }
}
