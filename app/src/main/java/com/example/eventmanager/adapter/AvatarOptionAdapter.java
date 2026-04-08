package com.example.eventmanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.eventmanager.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * Adapter for the avatar picker grid. Each item renders a circular DiceBear avatar
 * loaded by Glide from its URL, with a small style label underneath.
 */
public class AvatarOptionAdapter
        extends RecyclerView.Adapter<AvatarOptionAdapter.AvatarViewHolder> {

    public static class AvatarOption {
        public final String styleName;
        public final String url;
        public AvatarOption(String styleName, String url) {
            this.styleName = styleName;
            this.url = url;
        }
    }

    public interface OnAvatarSelectedListener {
        void onSelected(AvatarOption option);
    }

    private final List<AvatarOption> options;
    private final OnAvatarSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public AvatarOptionAdapter(List<AvatarOption> options, OnAvatarSelectedListener listener) {
        this.options = options;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_option, parent, false);
        return new AvatarViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        AvatarOption option = options.get(position);
        holder.tvStyle.setText(formatStyleName(option.styleName));

        Glide.with(holder.itemView.getContext())
                .load(option.url)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.ivAvatar);

        boolean isSelected = (position == selectedPosition);
        holder.ivAvatar.setStrokeColorResource(
                isSelected ? R.color.brand_accent : R.color.outline_lavender);
        holder.ivAvatar.setStrokeWidth(isSelected ? 6f : 2f);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onSelected(option);
        });
    }

    @Override
    public int getItemCount() { return options.size(); }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView ivAvatar;
        final TextView tvStyle;
        AvatarViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatarOption);
            tvStyle = itemView.findViewById(R.id.tvAvatarStyle);
        }
    }

    private static String formatStyleName(String raw) {
        return raw.replace("-", " ").replace("neutral", "").trim();
    }
}
