package com.example.eventmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Date;
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

    public EventAdapter(Context ctx, List<DocumentSnapshot> events, OnEventClickListener l) { this.context=ctx; this.events=events; this.listener=l; }
    public void updateList(List<DocumentSnapshot> nl) { this.events=nl; notifyDataSetChanged(); }

    @NonNull @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        return new EventViewHolder(LayoutInflater.from(context).inflate(R.layout.item_admin_event_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder h, int pos) {
        try {
            DocumentSnapshot doc = events.get(pos);
            String name = doc.getString("name");
            h.tvName.setText(name != null ? name : "Unnamed Event");

            Timestamp rs = safeTimestamp(doc, "registrationStart");
            Timestamp re = safeTimestamp(doc, "registrationEnd");
            String dt = "";
            if (rs != null) dt += sdf.format(rs.toDate());
            if (re != null) dt += " - " + sdf.format(re.toDate());
            h.tvDateRange.setText(!dt.isEmpty() ? dt : "No dates set");

            String posterUrl = doc.getString("posterUrl");
            if (posterUrl != null && !posterUrl.isEmpty()) {
                Glide.with(context).load(posterUrl).placeholder(R.drawable.ic_event_placeholder).centerCrop().into(h.ivPoster);
                h.ivPoster.setPadding(0,0,0,0);
            } else {
                h.ivPoster.setImageResource(R.drawable.ic_event_placeholder);
                h.ivPoster.setPadding(10,10,10,10);
            }

            Boolean isDel = doc.getBoolean("isDeleted"); boolean del = (isDel != null && isDel);
            h.btnRemove.setVisibility(del ? View.GONE : View.VISIBLE);
            h.statusBadge.setVisibility(del ? View.VISIBLE : View.GONE);
            h.cardRoot.setAlpha(del ? 0.6f : 1.0f);
            h.cardRoot.setOnClickListener(v -> listener.onEventClick(doc));
            h.btnRemove.setOnClickListener(v -> listener.onEventRemoveClick(doc));
        } catch (Exception e) {
            h.tvName.setText("Invalid event");
            h.tvDateRange.setText("Could not render this event");
            h.ivPoster.setImageResource(R.drawable.ic_event_placeholder);
            h.btnRemove.setVisibility(View.GONE);
            h.statusBadge.setVisibility(View.VISIBLE);
            h.cardRoot.setOnClickListener(null);
        }
    }

    @Override public int getItemCount() { return events.size(); }

    private Timestamp safeTimestamp(DocumentSnapshot doc, String field) {
        try {
            Timestamp ts = doc.getTimestamp(field);
            if (ts != null) return ts;
        } catch (Exception ignored) {
        }
        try {
            Date d = doc.getDate(field);
            if (d != null) return new Timestamp(d);
        } catch (Exception ignored) {
        }
        try {
            Object raw = doc.get(field);
            if (raw instanceof Long) {
                return new Timestamp(new Date((Long) raw));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        View cardRoot; ImageView ivPoster; TextView tvName, tvDateRange, btnRemove; LinearLayout statusBadge;
        EventViewHolder(View v) { super(v);
            cardRoot=v.findViewById(R.id.card_root); ivPoster=v.findViewById(R.id.iv_event_poster);
            tvName=v.findViewById(R.id.tv_event_name); tvDateRange=v.findViewById(R.id.tv_date_range);
            btnRemove=v.findViewById(R.id.btn_remove); statusBadge=v.findViewById(R.id.status_badge);
        }
    }
}
