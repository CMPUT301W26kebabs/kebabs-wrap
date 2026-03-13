package com.example.eventmanager.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0, TYPE_PROFILE = 1;
    public interface OnProfileClickListener {
        void onProfileClick(DocumentSnapshot profileDoc);
        void onProfileRemoveClick(DocumentSnapshot profileDoc);
    }

    private final Context context;
    private final OnProfileClickListener listener;
    private List<Object> items = new ArrayList<>();
    private final int[] avatarColors = {0xFF4A43EC,0xFF8B5CF6,0xFFEC4899,0xFFF59E0B,0xFF10B981,0xFF3B82F6,0xFFFF445D,0xFF14B8A6};

    public ProfileAdapter(Context ctx, List<DocumentSnapshot> profiles, OnProfileClickListener l) { this.context=ctx; this.listener=l; buildSectionedList(profiles); }
    public void updateList(List<DocumentSnapshot> nl) { buildSectionedList(nl); notifyDataSetChanged(); }

    private void buildSectionedList(List<DocumentSnapshot> profiles) {
        items.clear();
        List<DocumentSnapshot> sorted = new ArrayList<>(profiles);
        Collections.sort(sorted, (a,b) -> { String na=a.getString("name"); String nb=b.getString("name"); if(na==null)na="ZZZ"; if(nb==null)nb="ZZZ"; return na.compareToIgnoreCase(nb); });
        String last = "";
        for (DocumentSnapshot doc : sorted) {
            String name = doc.getString("name");
            if (name != null && !name.isEmpty()) { String fl = name.substring(0,1).toUpperCase(); if (!fl.equals(last)) { items.add(fl); last=fl; } }
            items.add(doc);
        }
    }

    @Override public int getItemViewType(int pos) { return items.get(pos) instanceof String ? TYPE_HEADER : TYPE_PROFILE; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        if (vt == TYPE_HEADER) return new SectionVH(LayoutInflater.from(context).inflate(R.layout.item_section_header, parent, false));
        return new ProfileVH(LayoutInflater.from(context).inflate(R.layout.item_profile_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        if (holder instanceof SectionVH) { ((SectionVH)holder).tvLetter.setText((String)items.get(pos)); return; }
        ProfileVH h = (ProfileVH) holder;
        DocumentSnapshot doc = (DocumentSnapshot) items.get(pos);
        String name = doc.getString("name");
        h.tvName.setText(name != null ? name : "No Name");
        if (name != null && !name.isEmpty()) { String[] p=name.split(" "); h.tvInitials.setText(p.length>=2?(""+p[0].charAt(0)+p[p.length-1].charAt(0)).toUpperCase():(""+p[0].charAt(0)).toUpperCase()); } else { h.tvInitials.setText("?"); }
        int ci = Math.abs((name!=null?name:"").hashCode()) % avatarColors.length;
        h.tvInitials.setBackgroundTintList(ColorStateList.valueOf(avatarColors[ci]));
        Boolean isDis = doc.getBoolean("isDisabled"); boolean dis = (isDis!=null&&isDis);
        h.btnRemove.setVisibility(dis?View.GONE:View.VISIBLE); h.btnDone.setVisibility(dis?View.VISIBLE:View.GONE);
        h.cardRoot.setAlpha(dis?0.7f:1.0f);
        h.cardRoot.setOnClickListener(v -> listener.onProfileClick(doc));
        h.btnRemove.setOnClickListener(v -> listener.onProfileRemoveClick(doc));
    }

    @Override public int getItemCount() { return items.size(); }
    static class SectionVH extends RecyclerView.ViewHolder { TextView tvLetter; SectionVH(View v){super(v);tvLetter=v.findViewById(R.id.tv_section_letter);} }
    static class ProfileVH extends RecyclerView.ViewHolder { View cardRoot; TextView tvInitials,tvName,btnRemove,btnDone;
        ProfileVH(View v){super(v);cardRoot=v.findViewById(R.id.card_root);tvInitials=v.findViewById(R.id.tv_avatar_initials);tvName=v.findViewById(R.id.tv_profile_name);btnRemove=v.findViewById(R.id.btn_remove);btnDone=v.findViewById(R.id.btn_done);} }
}
