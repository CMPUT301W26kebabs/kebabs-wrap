package com.example.eventmanager.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PROFILE = 1;

    public interface OnProfileClickListener {
        void onProfileClick(DocumentSnapshot profileDoc);
        void onProfileRemoveClick(DocumentSnapshot profileDoc);
    }

    private final Context context;
    private final OnProfileClickListener listener;
    private List<Object> items = new ArrayList<>();

    private final int[] avatarColors = {
            0xFF4A43EC, 0xFF8B5CF6, 0xFFEC4899, 0xFFF59E0B,
            0xFF10B981, 0xFF3B82F6, 0xFFFF445D, 0xFF14B8A6
    };

    public ProfileAdapter(Context context, List<DocumentSnapshot> profiles, OnProfileClickListener listener) {
        this.context = context;
        this.listener = listener;
        buildSectionedList(profiles);
    }

    public void updateList(List<DocumentSnapshot> newList) {
        buildSectionedList(newList);
        notifyDataSetChanged();
    }

    private void buildSectionedList(List<DocumentSnapshot> profiles) {
        items.clear();
        List<DocumentSnapshot> sorted = new ArrayList<>(profiles);
        Collections.sort(sorted, (a, b) -> {
            String nameA = a.getString("name");
            String nameB = b.getString("name");
            if (nameA == null) nameA = "ZZZ";
            if (nameB == null) nameB = "ZZZ";
            return nameA.compareToIgnoreCase(nameB);
        });

        String lastLetter = "";
        for (DocumentSnapshot doc : sorted) {
            String name = doc.getString("name");
            if (name != null && !name.isEmpty()) {
                String firstLetter = name.substring(0, 1).toUpperCase();
                if (!firstLetter.equals(lastLetter)) {
                    items.add(firstLetter);
                    lastLetter = firstLetter;
                }
            }
            items.add(doc);
        }
    }

    @Override public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_PROFILE;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_section_header, parent, false);
            return new SectionVH(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_profile_card, parent, false);
            return new ProfileVH(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SectionVH) {
            ((SectionVH) holder).tvLetter.setText((String) items.get(position));
        } else if (holder instanceof ProfileVH) {
            DocumentSnapshot doc = (DocumentSnapshot) items.get(position);
            ProfileVH h = (ProfileVH) holder;

            String name = doc.getString("name");
            h.tvName.setText(name != null ? name : "No Name");

            if (name != null && !name.isEmpty()) {
                String[] parts = name.split(" ");
                h.tvInitials.setText(parts.length >= 2
                        ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
                        : ("" + parts[0].charAt(0)).toUpperCase());
            } else { h.tvInitials.setText("?"); }

            int ci = Math.abs((name != null ? name : "").hashCode()) % avatarColors.length;
            h.tvInitials.setBackgroundTintList(ColorStateList.valueOf(avatarColors[ci]));

            Boolean isDisabled = doc.getBoolean("isDisabled");
            boolean disabled = (isDisabled != null && isDisabled);
            h.btnRemove.setVisibility(disabled ? View.GONE : View.VISIBLE);
            h.btnDone.setVisibility(disabled ? View.VISIBLE : View.GONE);
            h.cardRoot.setAlpha(disabled ? 0.7f : 1.0f);

            h.cardRoot.setOnClickListener(v -> listener.onProfileClick(doc));
            h.btnRemove.setOnClickListener(v -> listener.onProfileRemoveClick(doc));
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class SectionVH extends RecyclerView.ViewHolder {
        TextView tvLetter;
        SectionVH(View v) { super(v); tvLetter = v.findViewById(R.id.tv_section_letter); }
    }

    static class ProfileVH extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView tvInitials, tvName, btnRemove, btnDone;
        ProfileVH(View v) {
            super(v);
            cardRoot = v.findViewById(R.id.card_root);
            tvInitials = v.findViewById(R.id.tv_avatar_initials);
            tvName = v.findViewById(R.id.tv_profile_name);
            btnRemove = v.findViewById(R.id.btn_remove);
            btnDone = v.findViewById(R.id.btn_done);
        }
    }
}
