package com.example.eventmanager.adapter;

import com.example.eventmanager.R;
import com.example.eventmanager.models.Entrant;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter that displays entrant rows in ManageEventActivity.
 * Supports two row types: section headers (bold labels separating groups
 * such as "Selected", "Waiting List") and regular entrant rows showing
 * an avatar, name, and email or device ID.
 *
 * @see Entrant#isSectionHeader()
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.ViewHolder> {

    private List<Entrant> entrants;

    public EntrantAdapter(List<Entrant> entrants) {
        this.entrants = entrants;
    }

    /**
     * Replaces the backing entrant list and refreshes the RecyclerView.
     *
     * @param newList the updated list (may include section-header entries).
     */
    public void updateList(List<Entrant> newList) {
        this.entrants = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.makeshift_item_entrant_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);
        if (entrant.isSectionHeader()) {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvName.setText(entrant.getName());
            holder.tvName.setTextSize(24);
            holder.tvName.setTextColor(0xFF374151);
            holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvEmail.setVisibility(View.GONE);
            holder.itemView.setPadding(holder.itemView.getPaddingLeft(), 18,
                    holder.itemView.getPaddingRight(), 6);
        } else {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.ivAvatar.setImageResource(R.drawable.ic_person_placeholder);
            holder.tvName.setText(entrant.getName() != null ? entrant.getName() : "Unknown");
            holder.tvName.setTextSize(16);
            holder.tvName.setTextColor(0xFF111827);
            holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvEmail.setText(entrant.getEmail() != null && !entrant.getEmail().isEmpty()
                    ? entrant.getEmail()
                    : entrant.getDeviceId());
            holder.tvEmail.setVisibility(View.VISIBLE);
            holder.itemView.setPadding(holder.itemView.getPaddingLeft(), 10,
                    holder.itemView.getPaddingRight(), 10);
        }
    }

    @Override
    public int getItemCount() { return entrants != null ? entrants.size() : 0; }

    private String getSectionLabel(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "#";
        }
        return name.trim().substring(0, 1).toUpperCase(Locale.getDefault());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvEmail;
        ViewHolder(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivEntrantAvatar);
            tvName = v.findViewById(R.id.tvEntrantName);
            tvEmail = v.findViewById(R.id.tvEntrantEmail);
        }
    }
}
