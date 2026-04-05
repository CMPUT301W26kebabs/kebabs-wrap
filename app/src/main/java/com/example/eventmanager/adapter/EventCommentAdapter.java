package com.example.eventmanager.adapter;
import com.example.eventmanager.models.Entrant;

import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.example.eventmanager.models.EventComment;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventCommentAdapter extends RecyclerView.Adapter<EventCommentAdapter.CommentViewHolder> {

    private List<EventComment> comments = new ArrayList<>();

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        EventComment c = comments.get(position);

        ViewGroup.LayoutParams rawLp = holder.itemView.getLayoutParams();
        if (rawLp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
            int top = position == 0 ? 0 : (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 14, holder.itemView.getResources().getDisplayMetrics());
            lp.topMargin = top;
            holder.itemView.setLayoutParams(lp);
        }

        String name = c.getAuthorName();
        if (name == null || name.isEmpty()) {
            name = "Entrant";
        }
        holder.authorName.setText(name);
        String initial = name.substring(0, 1).toUpperCase(Locale.getDefault());
        holder.initial.setText(initial);
        holder.body.setText(c.getText());

        Timestamp ts = c.getTimestamp();
        if (ts != null) {
            long then = ts.toDate().getTime();
            holder.time.setText(DateUtils.getRelativeTimeSpanString(
                    then,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
        } else {
            holder.time.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void updateData(List<EventComment> newComments) {
        this.comments = newComments != null ? newComments : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        final TextView initial;
        final TextView authorName;
        final TextView body;
        final TextView time;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            initial = itemView.findViewById(R.id.commentAuthorInitial);
            authorName = itemView.findViewById(R.id.commentAuthorName);
            body = itemView.findViewById(R.id.commentText);
            time = itemView.findViewById(R.id.commentTime);
        }
    }
}
