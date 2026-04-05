package com.example.eventmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.example.eventmanager.models.Event;
import com.example.eventmanager.models.RegistrationHistoryItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the entrant's registration history list.
 * Each row shows the event name, date, location, and a colour-coded
 * status pill (waiting, selected, invited, enrolled, or cancelled).
 *
 * @see RegistrationHistoryItem
 */
public class RegistrationHistoryAdapter extends RecyclerView.Adapter<RegistrationHistoryAdapter.ViewHolder> {

    private final List<RegistrationHistoryItem> historyList;
    private final DateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);

    public RegistrationHistoryAdapter(List<RegistrationHistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registration_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegistrationHistoryItem row = historyList.get(position);
        Event event = row.getEvent();
        Context ctx = holder.itemView.getContext();

        holder.tvEventName.setText(row.getEventDisplayName());

        Date when = event.getRegistrationStart() != null
                ? event.getRegistrationStart()
                : event.getRegistrationEnd();
        String dateStr = formatEventDate(ctx, when);
        String loc = row.getLocationDisplay();
        if (loc != null) {
            holder.tvEventDateLocation.setText(dateStr + " • " + loc);
        } else {
            holder.tvEventDateLocation.setText(dateStr);
        }

        RegistrationHistoryItem.RegistrationStatus st = row.getStatus();
        switch (st) {
            case CANCELLED:
                holder.tvStatus.setBackground(null);
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_cancelled_text));
                holder.tvStatus.setText(R.string.registration_status_cancelled);
                break;
            case WAITING_LIST:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_amber);
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_waiting_text));
                holder.tvStatus.setText(R.string.registration_status_not_selected);
                break;
            case SELECTED:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_green);
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_selected_text));
                holder.tvStatus.setText(R.string.registration_status_selected);
                break;
            case INVITED:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_amber);
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_waiting_text));
                holder.tvStatus.setText(R.string.registration_status_invited);
                break;
            case ENROLLED:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_green);
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_enrolled_text));
                holder.tvStatus.setText(R.string.registration_status_enrolled);
                break;
            default:
                break;
        }
    }

    private String formatEventDate(Context ctx, Date when) {
        if (when == null) {
            return ctx.getString(R.string.registration_history_date_unknown);
        }
        return dateFormat.format(when);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName;
        TextView tvEventDateLocation;
        TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDateLocation = itemView.findViewById(R.id.tvEventDateLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
