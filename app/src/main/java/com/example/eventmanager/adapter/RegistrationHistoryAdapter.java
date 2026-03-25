package com.example.eventmanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventmanager.R;
import com.example.eventmanager.models.Event; // Assuming Event stores history data
import java.util.List;

public class RegistrationHistoryAdapter extends RecyclerView.Adapter<RegistrationHistoryAdapter.ViewHolder> {

    private List<Event> historyList;

    public RegistrationHistoryAdapter(List<Event> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registration_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = historyList.get(position);
        holder.tvEventName.setText(event.getName());
        // Customize this string based on your Event model fields
        holder.tvEventDateLocation.setText("Jan 01, 2026 • Online");
        holder.tvStatus.setText("SELECTED"); // Or get from a registration model
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvEventDateLocation, tvStatus;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDateLocation = itemView.findViewById(R.id.tvEventDateLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
