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
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.eventmanager.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    public interface OnImageActionListener {
        void onImageRemoveClick(DocumentSnapshot eventDoc);
    }

    private final Context context;
    private List<DocumentSnapshot> events;
    private final OnImageActionListener listener;

    public ImageAdapter(Context context, List<DocumentSnapshot> events, OnImageActionListener listener) {
        this.context = context;
        this.events = events;
        this.listener = listener;
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        DocumentSnapshot doc = events.get(position);

        String name = doc.getString("name");
        holder.tvEventName.setText(name != null ? name : "Event " + (position + 1));

        String posterUrl = doc.getString("posterUrl");
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(context)
                    .load(posterUrl)
                    .transform(new CenterCrop())
                    .placeholder(R.drawable.ic_event_placeholder)
                    .into(holder.ivPoster);
        } else {
            holder.ivPoster.setImageResource(R.drawable.ic_event_placeholder);
            holder.ivPoster.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivPoster.setPadding(40, 40, 40, 40);
        }

        holder.btnRemove.setOnClickListener(v -> listener.onImageRemoveClick(doc));
    }

    @Override
    public int getItemCount() { return events.size(); }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster, btnRemove;
        TextView tvEventName;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvEventName = itemView.findViewById(R.id.tv_event_name);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
