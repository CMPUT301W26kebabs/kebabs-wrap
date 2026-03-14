package com.example.eventmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.ViewHolder> {

    private List<AnasEntrant> entrants;

    public EntrantAdapter(List<AnasEntrant> entrants) {
        this.entrants = entrants;
    }

    public void updateList(List<AnasEntrant> newList) {
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
        AnasEntrant entrant = entrants.get(position);
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
