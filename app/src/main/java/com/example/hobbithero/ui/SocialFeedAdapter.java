package com.example.hobbithero.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import androidx.navigation.Navigation;
import androidx.fragment.app.FragmentActivity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import android.media.MediaPlayer;
import java.util.List;

public class SocialFeedAdapter extends RecyclerView.Adapter<SocialFeedAdapter.PostViewHolder> {
    
    private static MediaPlayer mediaPlayer;
    private static String currentPlayingUrl;

    private final List<SocialPost> postList;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final NotificationHelper notificationHelper;

    public SocialFeedAdapter(List<SocialPost> postList,
                             FirebaseFirestore db,
                             FirebaseAuth auth) {
        this.postList            = postList;
        this.db                  = db;
        this.auth                = auth;
        this.notificationHelper  = new NotificationHelper();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_social_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        SocialPost post = postList.get(position);

        if (holder.tvAuthor != null) {
            holder.tvAuthor.setText(post.getAuthorName());
            holder.tvTime.setText(post.getTimeAgo());
            holder.tvContent.setText(post.getContentText());

            // Profil yönlendirmesi
            View.OnClickListener openProfile = v -> {
                if (post.getAuthorUid() != null) {
                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("uid", post.getAuthorUid());
                    androidx.navigation.Navigation.findNavController(v)
                            .navigate(R.id.userProfileFragment, args);
                }
            };
            holder.tvAuthor.setOnClickListener(openProfile);
            if (holder.ivProfileImage != null) holder.ivProfileImage.setOnClickListener(openProfile);
        }

        if (holder.tvLikes != null) holder.tvLikes.setText(post.getLikesCount() + " beğenme");

        // Konum ve Müzik etiketleri
        if (post.getLocation() != null && !post.getLocation().isEmpty()) {
            holder.tvLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setText("📍 " + post.getLocation());
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        if (post.getMusic() != null && !post.getMusic().isEmpty()) {
            holder.tvMusic.setVisibility(View.VISIBLE);
            holder.tvMusic.setText("🎵 " + post.getMusic());
            
            // Müzik Çıkartması (Görselin üzerinde)
            if (holder.layoutMusicSticker != null) {
                holder.layoutMusicSticker.setVisibility(View.VISIBLE);
            }
            if (holder.tvMusicSticker != null) {
                holder.tvMusicSticker.setText(post.getMusic());
            }
        } else {
            holder.tvMusic.setVisibility(View.GONE);
            if (holder.layoutMusicSticker != null) {
                holder.layoutMusicSticker.setVisibility(View.GONE);
            }
        }


        // Begeni ikonu durumu
        updateLikeIcon(holder.btnLike, post.isLikedByMe());

        if (post.getImageBase64() != null && !post.getImageBase64().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            try {
                byte[] decodedString = android.util.Base64.decode(post.getImageBase64(), android.util.Base64.DEFAULT);
                android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivImage.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.ivImage.setVisibility(View.GONE);
            }
        } else if (post.getImageResId() != 0) {
            holder.ivImage.setVisibility(View.VISIBLE);
            holder.ivImage.setImageResource(post.getImageResId());
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }
        // Beğeni Sayısı ve Açıklama (Zaten yukarıda kısmen yapıldı ama tvLikes burada güncellenmeli)
        holder.tvLikes.setText(post.getLikesCount() + " beğenme");
        holder.tvContent.setText(post.getContentText());
        holder.tvTime.setText(post.getTimeAgo());

        // Begeni toggle
        holder.btnLike.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null || post.getPostId() == null) return;

            boolean nowLiked = !post.isLikedByMe();
            post.setLikedByMe(nowLiked);
            post.setLikesCount(post.getLikesCount() + (nowLiked ? 1 : -1));

            notifyItemChanged(position);

            // Firestore guncelle
            db.collection("posts").document(post.getPostId())
                    .update(
                        "likesCount", FieldValue.increment(nowLiked ? 1 : -1),
                        "liked_" + user.getUid(), nowLiked
                    );

            // Begeni bildirimi — sadece begeni yapildiginda ve post sahibi farkli ise
            if (nowLiked
                    && post.getAuthorUid() != null
                    && !post.getAuthorUid().equals(user.getUid())) {

                String fromName = user.getDisplayName() != null
                        ? user.getDisplayName() : "Biri";
                notificationHelper.sendNotification(
                        post.getAuthorUid(),
                        user.getUid(),
                        NotificationHelper.TYPE_LIKE,
                        fromName + " gönderini beğendi ❤️"
                );
            }
        });

        // Yorum - Alt sayfayı aç
        View.OnClickListener commentClick = v -> {
            if (post.getPostId() != null) {
                CommentsBottomSheet sheet = CommentsBottomSheet.newInstance(post.getPostId(), post.getAuthorUid());
                if (v.getContext() instanceof FragmentActivity) {
                    sheet.show(((FragmentActivity) v.getContext()).getSupportFragmentManager(), "comments");
                }
            }
        };

        holder.btnComment.setOnClickListener(commentClick);
        holder.tvCommentBox.setOnClickListener(commentClick);

        // Kaydetme Toggle
        if (post.getPostId() != null && auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).collection("savedPosts").document(post.getPostId())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            holder.btnSave.setColorFilter(Color.parseColor("#1976D2")); // Mavi
                            holder.btnSave.setTag(true); // Kaydedildi isareti
                        } else {
                            holder.btnSave.clearColorFilter();
                            holder.btnSave.setTag(false);
                        }
                    });
        }

        holder.btnSave.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null || post.getPostId() == null) return;

            boolean isAlreadySaved = holder.btnSave.getTag() != null && (boolean) holder.btnSave.getTag();

            if (isAlreadySaved) {
                // Kaydedilenlerden cikar
                db.collection("users").document(user.getUid())
                  .collection("savedPosts").document(post.getPostId())
                  .delete()
                  .addOnSuccessListener(aVoid -> {
                      holder.btnSave.clearColorFilter();
                      holder.btnSave.setTag(false);
                      android.widget.Toast.makeText(v.getContext(), "Kaydedilenlerden çıkarıldı", android.widget.Toast.LENGTH_SHORT).show();
                  });
            } else {
                // Kaydet
                db.collection("users").document(user.getUid())
                  .collection("savedPosts").document(post.getPostId())
                  .set(post.toMap())
                  .addOnSuccessListener(aVoid -> {
                      holder.btnSave.setColorFilter(Color.parseColor("#1976D2")); // Mavi
                      holder.btnSave.setTag(true);
                      android.widget.Toast.makeText(v.getContext(), "Gönderi kaydedildi!", android.widget.Toast.LENGTH_SHORT).show();
                  });
            }
        });

        // Post Menü (3 Nokta)
        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (post.getAuthorUid() != null && post.getAuthorUid().equals(currentUid)) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> showPostMenu(v, post, position));
        } else {
            holder.btnMenu.setVisibility(View.GONE);
        }
    }

    private void showPostMenu(View v, SocialPost post, int position) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(v.getContext(), v);
        popup.getMenu().add("Düzenle");
        popup.getMenu().add("Sil");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Sil")) {
                db.collection("posts").document(post.getPostId()).delete()
                        .addOnSuccessListener(aVoid -> {
                            android.widget.Toast.makeText(v.getContext(), "Gönderi silindi", android.widget.Toast.LENGTH_SHORT).show();
                            // Liste guncelleme genellikle snapshotListener tarafindan yapilir ama manuel de tetiklenebilir
                        });
            } else if (item.getTitle().equals("Düzenle")) {
                Bundle args = new Bundle();
                args.putString("postId", post.getPostId());
                androidx.navigation.Navigation.findNavController(v).navigate(R.id.action_exploreFragment_to_createPostFragment, args);
            }
            return true;
        });
        popup.show();
    }

    public static void playMusic(String url) {
        if (url == null || url.isEmpty()) {
            stopMusic();
            return;
        }
        if (url.equals(currentPlayingUrl) && mediaPlayer != null && mediaPlayer.isPlaying()) return;

        stopMusic();
        currentPlayingUrl = url;
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setLooping(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopMusic() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
            currentPlayingUrl = null;
        }
    }

    private void updateLikeIcon(ImageButton btn, boolean liked) {
        if (liked) {
            btn.setImageResource(R.drawable.ic_heart_filled);
            btn.clearColorFilter();
        } else {
            btn.setImageResource(R.drawable.ic_heart_outline);
        }
    }

    @Override
    public int getItemCount() { return postList.size(); }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvTime, tvContent, tvLikes, tvLocation, tvMusic, tvCommentBox, tvMusicSticker;
        ImageView ivImage, ivProfileImage;
        ImageButton btnLike, btnComment, btnSave, btnMenu;
        View layoutMusicSticker;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor  = itemView.findViewById(R.id.tvPostAuthor);
            tvTime    = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvPostCaption);
            tvLikes   = itemView.findViewById(R.id.tvPostLikes);
            tvLocation = itemView.findViewById(R.id.tvLocationTag);
            tvMusic   = itemView.findViewById(R.id.tvMusicTag);
            tvCommentBox = itemView.findViewById(R.id.tvCommentBoxPlaceholder);
            ivImage   = itemView.findViewById(R.id.ivPostImage);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            btnLike   = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnSave   = itemView.findViewById(R.id.btnSave);
            btnMenu   = itemView.findViewById(R.id.btnPostMenu);
            layoutMusicSticker = itemView.findViewById(R.id.layoutMusicSticker);
            tvMusicSticker = itemView.findViewById(R.id.tvMusicStickerText);
        }
    }
}
