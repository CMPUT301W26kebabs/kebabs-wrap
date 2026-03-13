package com.example.eventmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * RecyclerView adapter for displaying a list of Entrant profiles.
 * Used by EventListView for US2 (waiting list) and US3 (chosen list).
 * Each row shows the entrant's name and email.
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private List<Entrant> entrants;

    public EntrantAdapter(List<Entrant> entrants) {
        this.entrants = entrants;
    }

    /**
     * Updates the list data and refreshes the RecyclerView.
     * Called whenever Firestore data changes.
     */
    public void updateList(List<Entrant> newEntrants) {
        this.entrants = newEntrants;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.makeshift_item_entrant_activity, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);
        holder.tvName.setText(entrant.getName() != null ? entrant.getName() : "Unknown");
        holder.tvEmail.setText(entrant.getEmail() != null ? entrant.getEmail() : "No email");
    }

    @Override
    public int getItemCount() {
        return entrants != null ? entrants.size() : 0;
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;

        EntrantViewHolder(View itemView) {
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvEntrantName);
            tvEmail = itemView.findViewById(R.id.tvEntrantEmail);
        }
    }
}
