package com.example.hobbithero.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UserSearchFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView rvUserSearch;
    private UserSearchAdapter adapter;
    private final List<UserSearchFragment.UserResult> userResults = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FollowManager followManager;
    private NotificationHelper notificationHelper;

    static class UserResult {
        String uid, displayName, username, bio;
        boolean isFollowing;
        UserResult(String uid, String displayName, String username, String bio) {
            this.uid = uid; this.displayName = displayName; this.username = username; this.bio = bio;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        followManager = new FollowManager();
        notificationHelper = new NotificationHelper();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        rvUserSearch = view.findViewById(R.id.rvUserSearch);
        rvUserSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserSearchAdapter(userResults);
        rvUserSearch.setAdapter(adapter);

        searchView = view.findViewById(R.id.searchView);
        setupSearchView();
        
        // Klavye otomatik acilsin
        searchView.requestFocus();
        showKeyboard();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchUsers(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 2) {
                    searchUsers(newText.trim());
                } else {
                    userResults.clear();
                    adapter.notifyDataSetChanged();
                }
                return true;
            }
        });
    }

    private void searchUsers(String query) {
        if (TextUtils.isEmpty(query)) return;

        final Locale trLocale = new Locale("tr", "TR");
        String lowerQuery = query.toLowerCase(trLocale).replace("@", ""); // @ işaretini temizle

        // 1. Yol: Hem İsim hem Kullanıcı Adı üzerinden (searchName)
        db.collection("users")
                .orderBy("searchName")
                .startAt(lowerQuery)
                .endAt(lowerQuery + "\uf8ff")
                .limit(40)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded()) return;
                    
                    if (!snapshots.isEmpty()) {
                        processResults(snapshots);
                    } else {
                        // 2. Yol (Yedek): Sadece kullanıcı adına göre ara
                        db.collection("users")
                                .orderBy("username")
                                .startAt(lowerQuery)
                                .endAt(lowerQuery + "\uf8ff")
                                .limit(20)
                                .get()
                                .addOnSuccessListener(snap2 -> {
                                    if (!isAdded()) return;
                                    processResults(snap2);
                                });
                    }
                });
    }

    private void processResults(com.google.firebase.firestore.QuerySnapshot snapshots) {
        userResults.clear();
        String myUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        for (QueryDocumentSnapshot doc : snapshots) {
            String uid = doc.getString("uid");
            String name = doc.getString("displayName");
            String username = doc.getString("username");
            String bio = doc.getString("bio");
            if (name == null) name = "Anonim";
            if (username == null) username = "";
            if (bio == null) bio = "";
            if (myUid.equals(uid)) continue;
            userResults.add(new UserResult(uid, name, username, bio));
        }

        Collator collator = Collator.getInstance(new Locale("tr", "TR"));
        Collections.sort(userResults, (u1, u2) -> collator.compare(u1.displayName, u2.displayName));
        adapter.notifyDataSetChanged();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
    }

    private class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.VH> {
        private final List<UserResult> list;
        UserSearchAdapter(List<UserResult> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            UserResult user = list.get(position);
            holder.tvName.setText(user.displayName + " (@" + user.username + ")");
            holder.tvBio.setText(TextUtils.isEmpty(user.bio) ? "HabitHero üyesi" : user.bio);

            FirebaseUser me = auth.getCurrentUser();
            if (me != null) {
                followManager.isFollowing(me.getUid(), user.uid, isFollowing -> {
                    if (!isAdded()) return;
                    user.isFollowing = isFollowing;
                    updateFollowBtn(holder.btnFollow, user);
                });
            }

            holder.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(UserProfileFragment.ARG_UID, user.uid);
                NavHostFragment.findNavController(UserSearchFragment.this)
                        .navigate(R.id.action_userSearchFragment_to_userProfileFragment, args);
            });

            holder.btnFollow.setOnClickListener(v -> {
                FirebaseUser cur = auth.getCurrentUser();
                if (cur == null) return;
                if (user.isFollowing) {
                    followManager.unfollow(cur.getUid(), user.uid);
                    user.isFollowing = false;
                } else {
                    followManager.follow(cur.getUid(), user.uid);
                    user.isFollowing = true;
                    String myName = cur.getDisplayName() != null ? cur.getDisplayName() : "Biri";
                    notificationHelper.sendNotification(user.uid, cur.getUid(), NotificationHelper.TYPE_FOLLOW, myName + " seni takip etmeye başladı! 👤");
                }
                updateFollowBtn(holder.btnFollow, user);
            });
        }

        private void updateFollowBtn(MaterialButton btn, UserResult user) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> btn.setText(user.isFollowing ? "✓ Takipte" : "Takip Et"));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            ShapeableImageView ivAvatar;
            TextView tvName, tvBio;
            MaterialButton btnFollow;
            VH(@NonNull View v) {
                super(v);
                ivAvatar = v.findViewById(R.id.ivUserSearchAvatar);
                tvName = v.findViewById(R.id.tvUserSearchName);
                tvBio = v.findViewById(R.id.tvUserSearchBio);
                btnFollow = v.findViewById(R.id.btnFollowUser);
            }
        }
    }
}
