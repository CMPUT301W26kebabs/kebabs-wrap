package com.example.eventmanager.admin;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;

import java.util.ArrayList;
import java.util.List;

public class AdminCommentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_comments);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_filter).setOnClickListener(v ->
                Toast.makeText(this, "Comment filters coming soon.", Toast.LENGTH_SHORT).show());

        RecyclerView rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(new AdminCommentsAdapter(seedComments()));
    }

    private List<CommentItem> seedComments() {
        List<CommentItem> comments = new ArrayList<>();
        comments.add(new CommentItem("Alex Rivera", "2m ago", "Event: Tech Meetup 2026",
                "This sounds amazing!\nCan't wait to see the new AI integration demos. Will there be any hands-on workshops?",
                "AR", 0xFF6C8CCF));
        comments.add(new CommentItem("Sarah Chen", "15m ago", "Event: Design Thinking 101",
                "Check out my portfolio for some examples of the framework I mentioned during the last session.\nhttp://design-sarah.io",
                "SC", 0xFFE0A787));
        comments.add(new CommentItem("Marcus Johns...", "1h ago", "Event: Rooftop Networking",
                "Is the venue wheelchair accessible? Looking forward to it!",
                "MJ", 0xFF8E7CE6));
        comments.add(new CommentItem("David Wilson", "3h ago", "Event: Tech Meetup 2026",
                "I have a spare ticket if anyone is interested in joining. First come first served! DM me for details.",
                "DW", 0xFF8B623D));
        return comments;
    }

    private static class CommentItem {
        final String name;
        final String timeAgo;
        final String eventText;
        final String comment;
        final String initials;
        final int avatarColor;

        CommentItem(String name, String timeAgo, String eventText, String comment, String initials, int avatarColor) {
            this.name = name;
            this.timeAgo = timeAgo;
            this.eventText = eventText;
            this.comment = comment;
            this.initials = initials;
            this.avatarColor = avatarColor;
        }
    }

    private static class AdminCommentsAdapter extends RecyclerView.Adapter<AdminCommentsAdapter.CommentVH> {

        private final List<CommentItem> items;

        AdminCommentsAdapter(List<CommentItem> items) {
            this.items = new ArrayList<>(items);
        }

        @NonNull
        @Override
        public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_comment, parent, false);
            return new CommentVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentVH holder, int position) {
            CommentItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvTime.setText(item.timeAgo);
            holder.tvEvent.setText(item.eventText);
            holder.tvComment.setText(item.comment);
            holder.tvAvatar.setText(item.initials);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(item.avatarColor);
            holder.tvAvatar.setBackground(bg);

            holder.btnDelete.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    items.remove(adapterPos);
                    notifyItemRemoved(adapterPos);
                    Toast.makeText(v.getContext(), "Comment removed", Toast.LENGTH_SHORT).show();
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
}
