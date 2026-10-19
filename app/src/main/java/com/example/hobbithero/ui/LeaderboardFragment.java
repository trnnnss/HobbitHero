package com.example.hobbithero.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hobbithero.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private ListView listViewLeaderboard;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Veri modeli
    static class LeaderEntry {
        int rank;
        String name;
        long xp;
        long level;
        boolean isMe;

        LeaderEntry(int rank, String name, long xp, long level, boolean isMe) {
            this.rank = rank; this.name = name;
            this.xp = xp; this.level = level; this.isMe = isMe;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        listViewLeaderboard = view.findViewById(R.id.listViewLeaderboard);
        progressBar         = view.findViewById(R.id.progressLeaderboard);

        // Tab secimi
        TabLayout tabs = view.findViewById(R.id.tabLayoutLeaderboard);
        tabs.addTab(tabs.newTab().setText("🏅 Tüm Zamanlar"));
        tabs.addTab(tabs.newTab().setText("🔥 Bu Hafta"));

        loadLeaderboard();

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { loadLeaderboard(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /** Firestore'dan XP'ye göre büyükten küçüğe sıralı kullanıcı listesi */
    private void loadLeaderboard() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String myUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        db.collection("users")
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    List<LeaderEntry> entries = new ArrayList<>();
                    int rank = 1;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("displayName");
                        Long xp     = doc.getLong("totalXp");
                        Long level  = doc.getLong("level");
                        String uid  = doc.getString("uid");
                        if (name == null) name = "Anonim Kahraman";
                        entries.add(new LeaderEntry(
                                rank++, name,
                                xp    != null ? xp    : 0,
                                level != null ? level : 1,
                                myUid.equals(uid)
                        ));
                    }

                    // Firestore bossa mock sıralama göster
                    if (entries.isEmpty()) {
                        entries.add(new LeaderEntry(1, "Elif Şahin",  2340, 22, false));
                        entries.add(new LeaderEntry(2, "Ali Yılmaz",  1520, 15, false));
                        entries.add(new LeaderEntry(3, "Can Demir",    890,  8, false));
                    }

                    progressBar.setVisibility(View.GONE);
                    setupListView(entries);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    // Hata durumunda mock veri
                    List<LeaderEntry> fallback = new ArrayList<>();
                    fallback.add(new LeaderEntry(1, "Elif Şahin", 2340, 22, false));
                    fallback.add(new LeaderEntry(2, "Ali Yılmaz", 1520, 15, false));
                    fallback.add(new LeaderEntry(3, "Can Demir",   890,  8, false));
                    setupListView(fallback);
                });
    }

    /** ListView için özel ArrayAdapter */
    private void setupListView(List<LeaderEntry> entries) {
        LeaderboardAdapter adapter = new LeaderboardAdapter(requireContext(), entries);
        listViewLeaderboard.setAdapter(adapter);

        // Tıklamada kısa bilgi
        listViewLeaderboard.setOnItemClickListener((parent, v, position, id) -> {
            LeaderEntry entry = entries.get(position);
            android.widget.Toast.makeText(getContext(),
                    "#" + entry.rank + " " + entry.name + " — " + entry.xp + " XP",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    // ====== Özel ListView Adaptörü ======
    private static class LeaderboardAdapter extends ArrayAdapter<LeaderEntry> {

        LeaderboardAdapter(Context ctx, List<LeaderEntry> entries) {
            super(ctx, R.layout.item_leaderboard, entries);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_leaderboard, parent, false);
            }

            LeaderEntry entry = getItem(position);
            if (entry == null) return convertView;

            TextView tvRank    = convertView.findViewById(R.id.tvRank);
            TextView tvName    = convertView.findViewById(R.id.tvLeaderName);
            TextView tvLevel   = convertView.findViewById(R.id.tvLeaderLevel);
            TextView tvXp      = convertView.findViewById(R.id.tvLeaderXp);

            tvRank.setText(String.valueOf(entry.rank));
            tvName.setText(entry.name);
            tvLevel.setText("Seviye " + entry.level);
            tvXp.setText(entry.xp + " XP");

            // İlk 3 için altın/gümüş/bronz renkler
            int bgColor;
            switch (entry.rank) {
                case 1: bgColor = Color.parseColor("#FFF8E1"); break; // Altin
                case 2: bgColor = Color.parseColor("#F5F5F5"); break; // Gümüs
                case 3: bgColor = Color.parseColor("#FBE9E7"); break; // Bronz
                default: bgColor = entry.isMe
                        ? Color.parseColor("#E3F2FD")                 // Kullanicinin satiri
                        : Color.TRANSPARENT;
            }
            convertView.setBackgroundColor(bgColor);

            // Rank emojisi
            String rankLabel = entry.rank == 1 ? "🥇" :
                               entry.rank == 2 ? "🥈" :
                               entry.rank == 3 ? "🥉" :
                               String.valueOf(entry.rank);
            tvRank.setText(rankLabel);

            return convertView;
        }
    }
}
