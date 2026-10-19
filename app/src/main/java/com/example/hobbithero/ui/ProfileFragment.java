package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView tvProfileName, tvProfileHandle, tvProfileTitle, tvTotalXp, tvLevel;
    private TextView tvPostCount, tvFollowerCount, tvFollowingCount;
    private MaterialButton btnEditProfile, btnSettings;
    private RadarChartView radarChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // View baglantilari
        tvProfileName    = view.findViewById(R.id.tvProfileName);
        tvProfileHandle  = view.findViewById(R.id.tvProfileHandle);
        tvProfileTitle   = view.findViewById(R.id.tvProfileTitle);
        tvTotalXp        = view.findViewById(R.id.tvTotalXp);
        tvLevel          = view.findViewById(R.id.tvLevel);
        tvPostCount      = view.findViewById(R.id.tvPostCount);
        tvFollowerCount  = view.findViewById(R.id.tvFollowerCount);
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount);
        btnEditProfile   = view.findViewById(R.id.btnProfileDetails);
        btnSettings      = view.findViewById(R.id.btnSettings);
        radarChart       = view.findViewById(R.id.radarChart);

        // Radar Chart verilerini doldur
        if (radarChart != null) {
            Map<String, Integer> skills = new HashMap<>();
            skills.put("Spor", 75);
            skills.put("Zihin", 92);
            skills.put("Sosyal", 64);
            skills.put("Odak", 88);
            skills.put("Enerji", 70);
            radarChart.setData(skills);
        }

        // Kullanici bilgilerini yukle
        loadUserProfile();

        // Post grid (Gerçek veriler)
        RecyclerView rvProfilePosts = view.findViewById(R.id.rvProfilePosts);
        rvProfilePosts.setLayoutManager(new GridLayoutManager(getContext(), 3));
        
        List<SocialPost> userPosts = new ArrayList<>();
        ProfilePostGridAdapter gridAdapter = new ProfilePostGridAdapter(userPosts);
        rvProfilePosts.setAdapter(gridAdapter);
        
        TabLayout tabLayoutProfile = view.findViewById(R.id.tabLayoutProfile);
        
        // İlk yükleme (Paylaşılanlar)
        loadSharedPosts(userPosts, gridAdapter);

        // Tab değiştirme dinleyicisi
        tabLayoutProfile.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadSharedPosts(userPosts, gridAdapter);
                } else {
                    loadSavedPosts(userPosts, gridAdapter);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Profili Duzenle butonu -> EditProfileFragment'e git
        btnEditProfile.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profileFragment_to_editProfileFragment);
        });

        // Ayarlar butonu
        btnSettings.setOnClickListener(v -> {
            String[] options = {"Bildirim Ayarları", "Gizlilik ve Güvenlik", "Hesabı Sil", "Çıkış Yap"};
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("⚙️ Ayarlar")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) showNotificationSettings();
                        else if (which == 1) showPrivacySettings();
                        else if (which == 2) confirmDeleteAccount();
                        else if (which == 3) {
                            auth.signOut();
                            NavHostFragment.findNavController(this)
                                    .navigate(R.id.action_profileFragment_to_loginFragment);
                        }
                    })
                    .setNegativeButton("Kapat", null)
                    .show();
        });
    }

    private void loadSharedPosts(List<SocialPost> userPosts, ProfilePostGridAdapter gridAdapter) {
        if (auth.getCurrentUser() == null) return;
        userPosts.clear();
        db.collection("posts")
            .whereEqualTo("authorUid", auth.getCurrentUser().getUid())
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!isAdded()) return;
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                    SocialPost post = doc.toObject(SocialPost.class);
                    post.setPostId(doc.getId());
                    userPosts.add(post);
                }
                gridAdapter.notifyDataSetChanged();
            });
    }

    private void loadSavedPosts(List<SocialPost> userPosts, ProfilePostGridAdapter gridAdapter) {
        if (auth.getCurrentUser() == null) return;
        userPosts.clear();
        db.collection("users").document(auth.getCurrentUser().getUid())
            .collection("savedPosts")
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!isAdded()) return;
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                    SocialPost post = doc.toObject(SocialPost.class);
                    post.setPostId(doc.getId());
                    userPosts.add(post);
                }
                gridAdapter.notifyDataSetChanged();
            });
    }

    /** Firebase Auth + Firestore'dan profil bilgilerini cek */
    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Auth'dan hemen goster
        String dName = user.getDisplayName();
        if (dName == null || dName.isEmpty()) dName = user.getEmail();
        final String displayName = dName;
        tvProfileName.setText(displayName);

        // Firestore'dan detaylar
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (!doc.exists()) {
                        // Ilk giris — kullanici belgesi olustur
                        createUserDocument(user);
                    } else {
                        long xp         = doc.getLong("totalXp")       != null ? doc.getLong("totalXp")       : 560;
                        long level      = doc.getLong("level")          != null ? doc.getLong("level")          : 5;
                        long posts      = doc.getLong("postCount")      != null ? doc.getLong("postCount")      : 0;
                        long followers  = doc.getLong("followerCount")  != null ? doc.getLong("followerCount")  : 0;
                        long following  = doc.getLong("followingCount") != null ? doc.getLong("followingCount") : 0;

                        tvTotalXp.setText(String.valueOf(xp));
                        tvLevel.setText("Sv." + level + " ⭐");
                        tvPostCount.setText(String.valueOf(posts));
                        tvFollowerCount.setText(String.valueOf(followers));
                        tvFollowingCount.setText(String.valueOf(following));

                        // Username (Handle) goster veya yoksa olustur (Migration)
                        String username = doc.getString("username");
                        if (username == null || username.isEmpty()) {
                            // Migration: Mail adresinden username uret
                            String email = doc.getString("email");
                            if (email != null && email.contains("@")) {
                                username = email.split("@")[0].toLowerCase().replace(".", "_");
                            } else {
                                username = "hero_" + user.getUid().substring(0, 5);
                            }
                            // Firestore'u guncelle
                            db.collection("users").document(user.getUid())
                                    .update("username", username, "searchName", (displayName + " @" + username).toLowerCase());
                        }
                        tvProfileHandle.setText("@" + username);

                        // Unvan
                        String title = level < 10 ? "Verimlilik Çırağı 🏅"
                                     : level < 20 ? "Alışkanlık Ustası 🏅"
                                     : "Zaman Efendisi 🏆";
                        tvProfileTitle.setText(title);

                        // Base64 Profil Fotoğrafı yükle
                        String base64Image = doc.getString("profileImageBase64");
                        if (base64Image != null && !base64Image.isEmpty()) {
                            try {
                                byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                                android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                ImageView ivAvatar = getView().findViewById(R.id.ivProfilePicture);
                                if (ivAvatar != null) {
                                    ivAvatar.setImageBitmap(decodedByte);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("ProfileFragment", "Fotoğraf yüklenemedi", e);
                            }
                        }
                    }
                });
    }

    /** Ilk giris icin Firestore'da kullanici belgesi olustur */
    private void createUserDocument(FirebaseUser user) {
        UserSession session = UserSession.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid",           user.getUid());
        userData.put("displayName",   user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
        userData.put("email",         user.getEmail());
        userData.put("totalXp",       (long) session.getTotalXp());
        userData.put("level",         (long) session.getLevel());
        userData.put("postCount",     0L);
        userData.put("followerCount", 0L);
        userData.put("followingCount",0L);
        userData.put("createdAt",     FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(v -> loadUserProfile());
    }

    /** Hesap silme onay dialogu */
    private void confirmDeleteAccount() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Hesabı Sil")
                .setMessage("Bu işlem geri alınamaz. Tüm verileriniz silinecektir.")
                .setPositiveButton("Sil", (d, w) -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        db.collection("users").document(user.getUid()).delete();
                        user.delete().addOnSuccessListener(a ->
                                NavHostFragment.findNavController(this)
                                        .navigate(R.id.action_profileFragment_to_loginFragment));
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showNotificationSettings() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        
        db.collection("users").document(user.getUid()).collection("settings").document("notifications")
            .get().addOnSuccessListener(doc -> {
                boolean push = doc.getBoolean("push") != null ? doc.getBoolean("push") : true;
                boolean email = doc.getBoolean("email") != null ? doc.getBoolean("email") : false;
                
                boolean[] checkedItems = {push, email};
                String[] items = {"Anlık Bildirimler (Push)", "E-posta Bildirimleri"};
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Bildirim Ayarları")
                        .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                            checkedItems[which] = isChecked;
                        })
                        .setPositiveButton("Kaydet", (dialog, which) -> {
                            Map<String, Object> settings = new HashMap<>();
                            settings.put("push", checkedItems[0]);
                            settings.put("email", checkedItems[1]);
                            db.collection("users").document(user.getUid()).collection("settings").document("notifications").set(settings);
                            Toast.makeText(getContext(), "Bildirim tercihleri kaydedildi.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            });
    }

    private void showPrivacySettings() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        
        db.collection("users").document(user.getUid()).collection("settings").document("privacy")
            .get().addOnSuccessListener(doc -> {
                boolean privateProfile = doc.getBoolean("privateProfile") != null ? doc.getBoolean("privateProfile") : false;
                boolean showActivity = doc.getBoolean("showActivity") != null ? doc.getBoolean("showActivity") : true;
                
                boolean[] checkedItems = {privateProfile, showActivity};
                String[] items = {"Profilimi Gizli Tut", "Aktivite Durumumu Göster"};
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Gizlilik ve Güvenlik")
                        .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                            checkedItems[which] = isChecked;
                        })
                        .setPositiveButton("Kaydet", (dialog, which) -> {
                            Map<String, Object> settings = new HashMap<>();
                            settings.put("privateProfile", checkedItems[0]);
                            settings.put("showActivity", checkedItems[1]);
                            db.collection("users").document(user.getUid()).collection("settings").document("privacy").set(settings);
                            Toast.makeText(getContext(), "Gizlilik ayarları güncellendi.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            });
    }

    // ====== Post Grid Adapter ======
    private static class ProfilePostGridAdapter
            extends RecyclerView.Adapter<ProfilePostGridAdapter.ViewHolder> {
        private final List<SocialPost> posts;
        ProfilePostGridAdapter(List<SocialPost> posts) { this.posts = posts; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profile_post_grid, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SocialPost post = posts.get(position);
            holder.tvLikes.setText("❤ " + post.getLikesCount());

            if (post.getImageBase64() != null && !post.getImageBase64().isEmpty()) {
                try {
                    byte[] decodedString = android.util.Base64.decode(post.getImageBase64(), android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivImage.setImageBitmap(decodedByte);
                    holder.ivImage.setBackgroundColor(0x00000000); // clear background
                } catch (Exception e) {
                    setDefaultBackgroundColor(holder.ivImage, position);
                }
            } else if (post.getImageResId() != 0) {
                holder.ivImage.setImageResource(post.getImageResId());
                holder.ivImage.setBackgroundColor(0x00000000); // clear background
            } else {
                holder.ivImage.setImageDrawable(null);
                setDefaultBackgroundColor(holder.ivImage, position);
            }

            holder.itemView.setOnClickListener(v -> {
                if (post.getPostId() != null) {
                    Bundle args = new Bundle();
                    args.putString("postId", post.getPostId());
                    androidx.navigation.Navigation.findNavController(v)
                            .navigate(R.id.action_profileFragment_to_postDetailFragment, args);
                }
            });
        }

        private void setDefaultBackgroundColor(ImageView iv, int position) {
            int[] colors = {0xFFE3F2FD, 0xFFFCE4EC, 0xFFF3E5F5,
                            0xFFE8F5E9, 0xFFFFF3E0, 0xFFE0F2F1};
            iv.setBackgroundColor(colors[position % colors.length]);
        }

        @Override
        public int getItemCount() { return posts.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvLikes;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage  = itemView.findViewById(R.id.ivGridPostImage);
                tvLikes  = itemView.findViewById(R.id.tvGridPostLikes);
            }
        }
    }
}
