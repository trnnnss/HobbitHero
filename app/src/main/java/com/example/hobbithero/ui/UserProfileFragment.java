package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Başka bir kullanıcının profil sayfası.
 * Argument olarak "uid" (String) alır.
 */
public class UserProfileFragment extends Fragment {

    public static final String ARG_UID = "uid";

    private String targetUid;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FollowManager followManager;
    private NotificationHelper notificationHelper;
    private boolean isFollowing = false;

    private TextView tvName, tvHandle, tvBio, tvXpBadge;
    private TextView tvPostCount, tvFollowerCount, tvFollowingCount;
    private MaterialButton btnFollowUnfollow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db            = FirebaseFirestore.getInstance();
        auth          = FirebaseAuth.getInstance();
        followManager = new FollowManager();
        notificationHelper = new NotificationHelper();

        // Argument'tan UID al
        if (getArguments() != null) {
            targetUid = getArguments().getString(ARG_UID, "");
        }

        // Geri butonu
        Toolbar toolbar = view.findViewById(R.id.toolbarUserProfile);
        toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        // View bağlantıları
        tvName           = view.findViewById(R.id.tvUserDisplayName);
        tvHandle         = view.findViewById(R.id.tvUserHandle);
        tvBio            = view.findViewById(R.id.tvUserBio);
        tvXpBadge        = view.findViewById(R.id.tvUserXpBadge);
        tvPostCount      = view.findViewById(R.id.tvUserPostCount);
        tvFollowerCount  = view.findViewById(R.id.tvUserFollowerCount);
        tvFollowingCount = view.findViewById(R.id.tvUserFollowingCount);
        btnFollowUnfollow = view.findViewById(R.id.btnFollowUnfollow);

        // Post Grid
        RecyclerView rvPosts = view.findViewById(R.id.rvUserPosts);
        rvPosts.setLayoutManager(new GridLayoutManager(getContext(), 3));

        loadUserProfile();
        loadUserPosts(rvPosts);
        checkFollowStatus();

        // Takip Et / Bırak
        btnFollowUnfollow.setOnClickListener(v -> toggleFollow(view));
    }

    /** Firestore'dan profil verilerini yükle */
    private void loadUserProfile() {
        if (targetUid == null || targetUid.isEmpty()) return;

        db.collection("users").document(targetUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || !doc.exists()) return;

                    String name  = doc.getString("displayName");
                    String handle = doc.getString("username");
                    String bio   = doc.getString("bio");
                    Long xp      = doc.getLong("totalXp");
                    Long level   = doc.getLong("level");
                    Long posts   = doc.getLong("postCount");
                    Long followers  = doc.getLong("followerCount");
                    Long following  = doc.getLong("followingCount");

                    tvName.setText(name != null ? name : "Kullanıcı");
                    tvHandle.setText(handle != null ? "@" + handle : "");
                    tvBio.setText(bio != null ? bio : "HabitHero üyesi 💪");
                    tvXpBadge.setText("⭐ Sv." + (level != null ? level : 1)
                            + " · " + (xp != null ? xp : 0) + " XP");
                    tvPostCount.setText(String.valueOf(posts != null ? posts : 0));
                    tvFollowerCount.setText(String.valueOf(followers != null ? followers : 0));
                    tvFollowingCount.setText(String.valueOf(following != null ? following : 0));

                    // Toolbar başlığını güncelle
                    Toolbar tb = requireView().findViewById(R.id.toolbarUserProfile);
                    if (name != null) tb.setTitle(name);
                });
    }

    /** Kullanıcının postlarını yükle (mock grid) */
    private void loadUserPosts(RecyclerView rvPosts) {
        db.collection("posts")
                .whereEqualTo("authorUid", targetUid)
                .limit(12)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    // Post sayısına göre renk grid'i göster
                    List<Integer> colors = new ArrayList<>();
                    int[] palette = {0xFFE3F2FD, 0xFFFCE4EC, 0xFFF3E5F5,
                                     0xFFE8F5E9, 0xFFFFF3E0, 0xFFE0F2F1};
                    int count = Math.max(snap.size(), 6);
                    for (int i = 0; i < count; i++) colors.add(palette[i % palette.length]);

                    rvPosts.setAdapter(new ProfilePostGridAdapter(colors));
                });
    }

    /** Mevcut takip durumunu kontrol et */
    private void checkFollowStatus() {
        FirebaseUser me = auth.getCurrentUser();
        if (me == null || targetUid == null) return;

        // Kendi profilini görüyorsa butonu gizle
        if (me.getUid().equals(targetUid)) {
            btnFollowUnfollow.setVisibility(View.GONE);
            return;
        }

        followManager.isFollowing(me.getUid(), targetUid, following -> {
            if (!isAdded()) return;
            isFollowing = following;
            updateFollowButton();
        });
    }

    /** Takip et / takipten çık */
    private void toggleFollow(View rootView) {
        FirebaseUser me = auth.getCurrentUser();
        if (me == null) return;

        if (isFollowing) {
            followManager.unfollow(me.getUid(), targetUid);
            isFollowing = false;
            // followerCount güncelle (UI)
            String cur = tvFollowerCount.getText().toString();
            try { tvFollowerCount.setText(String.valueOf(Integer.parseInt(cur) - 1)); } catch (Exception ignored) {}
            Snackbar.make(rootView, "Takipten çıkıldı.", Snackbar.LENGTH_SHORT).show();
        } else {
            followManager.follow(me.getUid(), targetUid);
            isFollowing = true;
            String cur = tvFollowerCount.getText().toString();
            try { tvFollowerCount.setText(String.valueOf(Integer.parseInt(cur) + 1)); } catch (Exception ignored) {}
            
            // Bildirim gonder
            String myName = me.getDisplayName() != null ? me.getDisplayName() : "Biri";
            notificationHelper.sendNotification(targetUid, me.getUid(), NotificationHelper.TYPE_FOLLOW, myName + " seni takip etmeye başladı! 👤");
            
            Snackbar.make(rootView, "Takip edildi! 🎉", Snackbar.LENGTH_SHORT).show();
        }
        updateFollowButton();
    }

    private void updateFollowButton() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (isFollowing) {
                btnFollowUnfollow.setText("✓ Takip Ediliyor");
                btnFollowUnfollow.setStrokeWidth(2);
                btnFollowUnfollow.setBackgroundTintList(
                        requireContext().getColorStateList(android.R.color.transparent));
            } else {
                btnFollowUnfollow.setText("Takip Et");
                btnFollowUnfollow.setStrokeWidth(0);
                btnFollowUnfollow.setBackgroundTintList(null); // Primary renk
            }
        });
    }

    // ProfileFragment'teki adapter'ı yeniden kullanmak yerine kendi adapter'ı
    static class ProfilePostGridAdapter extends RecyclerView.Adapter<ProfilePostGridAdapter.VH> {
        private final List<Integer> colors;
        ProfilePostGridAdapter(List<Integer> colors) { this.colors = colors; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profile_post_grid, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.iv.setBackgroundColor(colors.get(position));
            holder.tv.setText("❤ " + ((int)(Math.random() * 50) + 1));
        }

        @Override public int getItemCount() { return colors.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView iv; TextView tv;
            VH(@NonNull View v) { super(v);
                iv = v.findViewById(R.id.ivGridPostImage);
                tv = v.findViewById(R.id.tvGridPostLikes); }
        }
    }
}
