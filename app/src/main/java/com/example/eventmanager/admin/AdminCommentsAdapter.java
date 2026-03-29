package com.example.eventmanager.admin;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;

import java.util.ArrayList;
import java.util.List;

public class AdminCommentsAdapter extends RecyclerView.Adapter<AdminCommentsAdapter.CommentVH> {

    public interface OnDeleteClickListener {
        void onDeleteClick(@NonNull AdminCommentListItem item);
    }

    private final List<AdminCommentListItem> items = new ArrayList<>();
    private final OnDeleteClickListener deleteClickListener;

    public AdminCommentsAdapter(OnDeleteClickListener deleteClickListener) {
        this.deleteClickListener = deleteClickListener;
    }

    public void setItems(List<AdminCommentListItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_comment, parent, false);
        return new CommentVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentVH holder, int position) {
        AdminCommentListItem item = items.get(position);
        holder.tvName.setText(item.authorName);
        holder.tvTime.setText(item.timeAgo);
        holder.tvEvent.setText(item.eventLine);
        holder.tvComment.setText(item.commentText);
        holder.tvAvatar.setText(item.initials);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(item.avatarColor);
        holder.tvAvatar.setBackground(bg);

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                deleteClickListener.onDeleteClick(items.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CommentVH extends RecyclerView.ViewHolder {
        final TextView tvAvatar;
        final TextView tvName;
        final TextView tvTime;
        final TextView tvEvent;
        final TextView tvComment;
        final ImageButton btnDelete;

        CommentVH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvEvent = itemView.findViewById(R.id.tv_event);
            tvComment = itemView.findViewById(R.id.tv_comment);
            btnDelete = itemView.findViewById(R.id.btn_delete_comment);
        }
    }
}
