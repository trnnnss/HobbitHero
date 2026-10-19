package com.example.hobbithero.ui;

import android.os.Bundle;
import android.util.Log;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.text.Collator;

public class ExploreFragment extends Fragment {

    // Post akışı
    private RecyclerView rvSocialFeed;
    private SocialFeedAdapter feedAdapter;
    private final List<SocialPost> postList = new ArrayList<>();
    private ListenerRegistration feedListener;

    private TextView tvFeedTitle;
    private FloatingActionButton fab;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FollowManager followManager;
    private NotificationHelper notificationHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db            = FirebaseFirestore.getInstance();
        auth          = FirebaseAuth.getInstance();
        followManager = new FollowManager();
        notificationHelper = new NotificationHelper();

        // Post akışı RecyclerView
        rvSocialFeed = view.findViewById(R.id.rvSocialFeed);
        rvSocialFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        feedAdapter = new SocialFeedAdapter(postList, db, auth);
        rvSocialFeed.setAdapter(feedAdapter);

        setupScrollListenerForMusic();

        // Post yükleme
        loadPostsRealtime();

        // Büyüteç - Arama Sayfasına Git
        view.findViewById(R.id.btnOpenSearch).setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_exploreFragment_to_userSearchFragment);
        });

        // FAB
        fab = view.findViewById(R.id.fabAddPost);
        fab.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_exploreFragment_to_createPostFragment);
        });

        // Post yükleme
        loadPostsRealtime();
    }



    // ─── POST AKIŞI ────────────────────────────────────────────────────────

    private void loadPostsRealtime() {
        feedListener = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    postList.clear();
                    FirebaseUser user = auth.getCurrentUser();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        SocialPost post = doc.toObject(SocialPost.class);
                        post.setPostId(doc.getId());
                        if (user != null) {
                            Boolean liked = doc.getBoolean("liked_" + user.getUid());
                            post.setLikedByMe(liked != null && liked);
                        }
                        postList.add(post);
                    }
                    // Firestore boşsa mock veriler
                    if (postList.isEmpty()) {
                        Log.d("Explore", "Henüz gönderi yok, veritabanını hazırlayın.");
                    }
                    feedAdapter.notifyDataSetChanged();
                });
    }



    private void setupScrollListenerForMusic() {
        rvSocialFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                // Ortadaki postu bul (en görünür olan)
                int mid = (firstVisible + lastVisible) / 2;
                if (mid >= 0 && mid < postList.size()) {
                    SocialPost post = postList.get(mid);
                    SocialFeedAdapter.playMusic(post.getMusicPreviewUrl());
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        SocialFeedAdapter.stopMusic();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (feedListener != null) feedListener.remove();
        SocialFeedAdapter.stopMusic();
    }


}
