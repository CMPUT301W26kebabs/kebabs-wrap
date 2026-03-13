package com.example.eventmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        holder.tvName.setText(entrant.getName() != null ? entrant.getName() : "Unknown");
        holder.tvEmail.setText(entrant.getEmail() != null ? entrant.getEmail() : "No email");
    }

    @Override
    public int getItemCount() { return entrants != null ? entrants.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvEntrantName);
            tvEmail = v.findViewById(R.id.tvEntrantEmail);
        }
    }
}
