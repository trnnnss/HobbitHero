package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MapNotification> notificationList = new ArrayList<>();
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvNoNotifications = view.findViewById(R.id.tvNoNotifications);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        if (auth.getCurrentUser() == null) return;

        db.collection("notifications")
                .document(auth.getCurrentUser().getUid())
                .collection("items")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        MapNotification n = new MapNotification(
                                doc.getString("fromUid"),
                                doc.getString("type"),
                                doc.getString("message"),
                                doc.getBoolean("read") != null && doc.getBoolean("read"),
                                doc.getTimestamp("timestamp")
                        );
                        n.id = doc.getId();
                        notificationList.add(n);
                        
                        // Otomatik olarak okundu işaretle
                        if (!n.read) {
                            doc.getReference().update("read", true);
                        }
                    }

                    if (notificationList.isEmpty()) {
                        tvNoNotifications.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        tvNoNotifications.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    public static class MapNotification {
        public String id;
        public String fromUid, type, message;
        public boolean read;
        public com.google.firebase.Timestamp timestamp;

        public MapNotification() {}

        public MapNotification(String fromUid, String type, String message, boolean read, com.google.firebase.Timestamp timestamp) {
            this.fromUid = fromUid; 
            this.type = type; 
            this.message = message; 
            this.read = read; 
            this.timestamp = timestamp;
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
        private final List<MapNotification> list;
        NotificationAdapter(List<MapNotification> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            MapNotification n = list.get(position);
            holder.tvMessage.setText(n.message);
            holder.vUnread.setVisibility(n.read ? View.GONE : View.VISIBLE);
            
            // Zaman bilgisini göster
            if (n.timestamp != null) {
                long diff = (com.google.firebase.Timestamp.now().getSeconds() - n.timestamp.getSeconds());
                String timeText;
                if (diff < 60) timeText = "az önce";
                else if (diff < 3600) timeText = (diff / 60) + " dk önce";
                else if (diff < 86400) timeText = (diff / 3600) + " sa önce";
                else timeText = (diff / 86400) + " gün önce";
                holder.tvTime.setText(timeText);
                holder.tvTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvTime.setVisibility(View.GONE);
            }

            // İkonu ayarla
            if (NotificationHelper.TYPE_LIKE.equals(n.type)) {
                holder.ivIcon.setImageResource(R.drawable.ic_heart_filled);
                holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#F43F5E"));
            } else if (NotificationHelper.TYPE_FOLLOW.equals(n.type)) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
                holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#10B981")); // Yeşil
            } else if ("comment".equals(n.type)) {
                holder.ivIcon.setImageResource(android.R.drawable.stat_notify_chat);
                holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#3B82F6")); // Mavi
            } else {
                holder.ivIcon.setImageResource(android.R.drawable.ic_popup_reminder);
                holder.ivIcon.clearColorFilter();
            }

            // Profil açma
            holder.itemView.setOnClickListener(v -> {
                if (n.fromUid != null && !n.fromUid.isEmpty()) {
                    dismiss();
                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("uid", n.fromUid);
                    androidx.navigation.fragment.NavHostFragment.findNavController(NotificationsBottomSheet.this)
                            .navigate(R.id.userProfileFragment, args);
                }
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvMessage, tvTime;
            ImageView ivIcon;
            View vUnread;
            VH(View v) {
                super(v);
                tvMessage = v.findViewById(R.id.tvNotifMessage);
                tvTime    = v.findViewById(R.id.tvNotifTime);
                ivIcon    = v.findViewById(R.id.ivNotifIcon);
                vUnread   = v.findViewById(R.id.vUnreadDot);
            }
        }
    }
}
