package com.example.eventmanager.adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.example.eventmanager.models.Entrant; // Ensure this matches your package

import java.util.List;

/**
 * Adapter to bind a list of Entrant objects to a RecyclerView.
 */
public class EnrolledEntrantAdapter extends RecyclerView.Adapter<EnrolledEntrantAdapter.EntrantViewHolder> {

    private List<Entrant> entrantList;

    public EnrolledEntrantAdapter(List<Entrant> entrantList) {
        this.entrantList = entrantList;
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant currentEntrant = entrantList.get(position);

        // Handle null safety just in case incomplete data was saved
        String name = currentEntrant.getName() != null ? currentEntrant.getName() : "Unknown Name";
        String email = currentEntrant.getEmail() != null ? currentEntrant.getEmail() : "No Email Provided";

        holder.nameTextView.setText(name);
        holder.emailTextView.setText(email);
    }

    @Override
    public int getItemCount() {
        return entrantList != null ? entrantList.size() : 0;
    }

    /**
     * Updates the data in the adapter and refreshes the UI.
     */
    public void updateData(List<Entrant> newEntrants) {
        this.entrantList = newEntrants;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder pattern to hold references to the UI elements in each row.
     */
    public static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView emailTextView;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textEntrantName);
            emailTextView = itemView.findViewById(R.id.textEntrantEmail);
        }
    }
}