package com.example.eventmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class HomeEventAdapter extends RecyclerView.Adapter<HomeEventAdapter.HomeEventVH> {

    private final Context context;
    private List<DocumentSnapshot> events;
    private OnEventClickListener onEventClickListener;

    public interface OnEventClickListener {
        void onEventClick(DocumentSnapshot eventDoc);
    }

    public HomeEventAdapter(Context context, List<DocumentSnapshot> events) {
        this.context = context;
        this.events = events;
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    public void setOnEventClickListener(OnEventClickListener onEventClickListener) {
        this.onEventClickListener = onEventClickListener;
    }

    @NonNull @Override
    public HomeEventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HomeEventVH(LayoutInflater.from(context).inflate(R.layout.item_home_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HomeEventVH h, int pos) {
        DocumentSnapshot doc = events.get(pos);

        String name = doc.getString("name");
        h.tvName.setText(name != null ? name : "Event");

        Timestamp regStart = doc.getTimestamp("registrationStart");
        if (regStart != null) {
            SimpleDateFormat dayFmt = new SimpleDateFormat("dd", Locale.getDefault());
            SimpleDateFormat monthFmt = new SimpleDateFormat("MMM", Locale.getDefault());
            h.tvDay.setText(dayFmt.format(regStart.toDate()));
            h.tvMonth.setText(monthFmt.format(regStart.toDate()));
        } else {
            h.tvDay.setText("--");
            h.tvMonth.setText("---");
        }

        h.tvGoing.setText("+20 Going");
        String location = doc.getString("location");
        h.tvLocation.setText(location != null && !location.trim().isEmpty()
                ? location
                : "University of Alberta");

        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(context).load(posterUrl).placeholder(R.drawable.ic_event_placeholder).centerCrop().into(h.ivImage);
        } else {
            h.ivImage.setImageResource(R.drawable.ic_event_placeholder);
        }

        h.itemView.setOnClickListener(v -> {
            if (onEventClickListener != null) {
                onEventClickListener.onEventClick(doc);
            }
        });
    }

    @Override public int getItemCount() { return events.size(); }

    static class HomeEventVH extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvDay, tvMonth, tvName, tvGoing, tvLocation;
        HomeEventVH(View v) {
            super(v);
            ivImage = v.findViewById(R.id.iv_event_image);
            tvDay = v.findViewById(R.id.tv_event_day);
            tvMonth = v.findViewById(R.id.tv_event_month);
            tvName = v.findViewById(R.id.tv_event_name);
            tvGoing = v.findViewById(R.id.tv_going_count);
            tvLocation = v.findViewById(R.id.tv_location);
        }
    }
}
