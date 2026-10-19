package com.example.hobbithero.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CommentsBottomSheet extends BottomSheetDialogFragment {

    private String postId;
    private String postOwnerId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Comment> commentList = new ArrayList<>();
    private CommentAdapter adapter;
    private ListenerRegistration commentListener;
    private TextView tvNoComments;
    private NotificationHelper notificationHelper;

    public static CommentsBottomSheet newInstance(String postId, String postOwnerId) {
        CommentsBottomSheet fragment = new CommentsBottomSheet();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        args.putString("postOwnerId", postOwnerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
            postOwnerId = getArguments().getString("postOwnerId");
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        notificationHelper = new NotificationHelper();
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments_bottom_sheet, container, false);
    }

    private RecyclerView rvComments; 

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvComments = view.findViewById(R.id.rvComments);
        EditText input = view.findViewById(R.id.etCommentInput);
        com.google.android.material.button.MaterialButton send = view.findViewById(R.id.btnSendComment);
        tvNoComments = view.findViewById(R.id.tvNoComments);

        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommentAdapter(commentList);
        rvComments.setAdapter(adapter);

        send.setOnClickListener(v -> {
            final String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(getContext(), "Lütfen bir yorum yazın", Toast.LENGTH_SHORT).show();
                return;
            }
            if (auth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Yorum yapmak için giriş yapmalısınız", Toast.LENGTH_SHORT).show();
                return;
            }
            if (postId == null) {
                Toast.makeText(getContext(), "Hata: Gönderi ID bulunamadı", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            String tempName = user.getDisplayName();
            if (tempName == null || tempName.isEmpty()) tempName = user.getEmail();
            final String name = tempName;
            
            // Profil resmini al (varsa)
            String profileImg = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

            Comment comment = new Comment(user.getUid(), name, text, profileImg);
            db.collection("posts").document(postId).collection("comments").add(comment.toMap())
                    .addOnSuccessListener(ref -> {
                        input.setText("");
                        Toast.makeText(getContext(), "Yorum paylaşıldı!", Toast.LENGTH_SHORT).show();
                        
                        // Bildirim gonder
                        if (postOwnerId != null && !postOwnerId.equals(user.getUid())) {
                            notificationHelper.sendNotification(
                                postOwnerId,
                                user.getUid(),
                                "comment",
                                name + " gönderine yorum yaptı: \"" + text + "\""
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        startListeningComments();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                int displayHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight(displayHeight);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void startListeningComments() {
        commentListener = db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    commentList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Comment c = doc.toObject(Comment.class);
                        c.setCommentId(doc.getId());
                        commentList.add(c);
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (commentList.isEmpty()) {
                        tvNoComments.setVisibility(View.VISIBLE);
                        rvComments.setVisibility(View.GONE);
                    } else {
                        tvNoComments.setVisibility(View.GONE);
                        rvComments.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (commentListener != null) commentListener.remove();
    }

    private void loadComments() {
        // Obsolete, replaced by startListeningComments
    }

    private class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {
        private List<Comment> list;
        CommentAdapter(List<Comment> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Comment c = list.get(position);
            holder.userName.setText(c.getUserName() != null ? c.getUserName() : "Anonim");
            holder.text.setText(c.getText());
            
            if (c.getUserProfileImage() != null && !c.getUserProfileImage().isEmpty()) {
                if (c.getUserProfileImage().startsWith("data:image")) {
                     try {
                         String pureBase64 = c.getUserProfileImage().substring(c.getUserProfileImage().indexOf(",") + 1);
                         byte[] decodedString = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                         android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                         holder.profileImage.setImageBitmap(decodedByte);
                     } catch (Exception e) {}
                }
            } else {
                holder.profileImage.setImageResource(R.drawable.ic_turtle);
            }

            // 3 Nokta Menü Mantığı
            String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
            boolean isMyComment = c.getUserId().equals(currentUid);
            boolean isPostOwner = postOwnerId != null && postOwnerId.equals(currentUid);

            if (isMyComment || isPostOwner) {
                holder.btnMenu.setVisibility(View.VISIBLE);
                holder.btnMenu.setOnClickListener(v -> showCommentMenu(v, c, isMyComment, isPostOwner));
            } else {
                holder.btnMenu.setVisibility(View.GONE);
            }
        }

        private void showCommentMenu(View v, Comment comment, boolean isMyComment, boolean isPostOwner) {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(getContext(), v);
            if (isMyComment) {
                popup.getMenu().add("Düzenle");
                popup.getMenu().add("Sil");
            } else if (isPostOwner) {
                popup.getMenu().add("Sil (Yönetici)");
            }

            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Sil") || item.getTitle().equals("Sil (Yönetici)")) {
                    deleteComment(comment);
                } else if (item.getTitle().equals("Düzenle")) {
                    showEditCommentDialog(comment);
                }
                return true;
            });
            popup.show();
        }

        private void deleteComment(Comment comment) {
            db.collection("posts").document(postId).collection("comments").document(comment.getCommentId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Yorum silindi", Toast.LENGTH_SHORT).show());
        }

        private void showEditCommentDialog(Comment comment) {
            EditText et = new EditText(getContext());
            et.setText(comment.getText());
            et.setPadding(40, 40, 40, 40);
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Yorumu Düzenle")
                    .setView(et)
                    .setPositiveButton("Güncelle", (dialog, which) -> {
                        String newText = et.getText().toString().trim();
                        if (!newText.isEmpty()) {
                            db.collection("posts").document(postId).collection("comments").document(comment.getCommentId())
                                    .update("text", newText)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Yorum güncellendi", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView userName, text;
            com.google.android.material.imageview.ShapeableImageView profileImage;
            ImageButton btnMenu;
            VH(View v) {
                super(v);
                userName = v.findViewById(R.id.tvCommentUserName);
                text = v.findViewById(R.id.tvCommentText);
                profileImage = v.findViewById(R.id.ivCommentUser);
                btnMenu = v.findViewById(R.id.btnCommentMenu);
            }
        }
    }
}
