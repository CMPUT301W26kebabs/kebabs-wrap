package com.example.eventmanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.example.eventmanager.models.Entrant;

import java.util.List;

/**
 * WinnerAdapter
 *
 * Displays the list of selected winners inside the Lottery Draw screen's
 * results CardView (US 02.05.02, 02.06.01).
 */
public class WinnerAdapter extends RecyclerView.Adapter<WinnerAdapter.WinnerViewHolder> {

    private final List<Entrant> winners;

    public WinnerAdapter(List<Entrant> winners) {
        this.winners = winners;
    }

    @NonNull
    @Override
    public WinnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_winner, parent, false);
        return new WinnerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WinnerViewHolder holder, int position) {
        Entrant e = winners.get(position);
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(e.getName() != null ? e.getName() : "Unknown");
        holder.tvEmail.setText(e.getEmail() != null ? e.getEmail() : "—");

        // Subtle entry animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationX(30f);
        holder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(300)
                .setStartDelay(position * 60L)
                .start();
    }

    @Override
    public int getItemCount() {
        return winners.size();
    }

    static class WinnerViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvEmail;

        WinnerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank  = itemView.findViewById(R.id.tvRank);
            tvName  = itemView.findViewById(R.id.tvWinnerName);
            tvEmail = itemView.findViewById(R.id.tvWinnerEmail);
        }
    }
}