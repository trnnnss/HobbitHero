package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hobbithero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PostDetailFragment extends Fragment {

    private String postId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                SocialPost post = doc.toObject(SocialPost.class);
                if (post != null) {
                    post.setPostId(doc.getId());
                    
                    // Manually binding to the included layout
                    List<SocialPost> singleList = new ArrayList<>();
                    singleList.add(post);
                    SocialFeedAdapter adapter = new SocialFeedAdapter(singleList, db, auth);
                    
                    // Since it's a single item, we just use the adapter's logic by manually calling bind
                    SocialFeedAdapter.PostViewHolder holder = new SocialFeedAdapter.PostViewHolder(view.findViewById(R.id.postItemInclude));
                    adapter.onBindViewHolder(holder, 0);
                }
            }
        });
    }
}
