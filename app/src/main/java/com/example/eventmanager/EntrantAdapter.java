package com.example.eventmanager;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

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
                .inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnasEntrant entrant = entrants.get(position);
        String name = entrant.getName() != null ? entrant.getName() : "Unknown";
        holder.tvName.setText(name);

        String section = getSectionLabel(name);
        boolean showHeader = position == 0
                || !section.equals(getSectionLabel(entrants.get(position - 1).getName()));
        holder.tvSectionHeader.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        holder.tvSectionHeader.setText(section);

        boolean isDoneState = "enrolled".equalsIgnoreCase(entrant.getStatus());
        holder.tvStatusAction.setText(isDoneState ? "Done" : "Send");
        holder.tvStatusAction.setBackgroundResource(
                isDoneState ? R.drawable.bg_button_outline_purple : R.drawable.bg_button_primary);
        holder.tvStatusAction.setTextColor(isDoneState
                ? Color.parseColor("#6C47FF")
                : Color.WHITE);
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
        TextView tvSectionHeader, tvName, tvStatusAction;

        ViewHolder(View v) {
            super(v);
            tvSectionHeader = v.findViewById(R.id.tvSectionHeader);
            tvName = v.findViewById(R.id.tvAttendeeName);
            tvStatusAction = v.findViewById(R.id.tvStatusAction);
        }
    }
}
