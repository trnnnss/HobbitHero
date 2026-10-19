package com.example.hobbithero.ui;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class SocialPost {
    private String postId;
    private String authorUid;
    private String authorName;
    private String contentText;
    private int likesCount;
    private boolean likedByMe;
    private Timestamp timestamp;
    private String imageBase64;
    private String location;
    private String music;
    private String musicPreviewUrl;

    // Firestore için boş constructor
    public SocialPost() {}

    // Yeni post oluşturma constructor'ı
    public SocialPost(String authorUid, String authorName, String contentText, String imageBase64, String location, String music, String musicPreviewUrl) {
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.contentText = contentText;
        this.imageBase64 = imageBase64;
        this.location = location;
        this.music = music;
        this.musicPreviewUrl = musicPreviewUrl;
        this.likesCount = 0;
        this.likedByMe = false;
        this.timestamp = Timestamp.now();
    }

    // Mock veriler için geriye dönük uyumluluk constructor'ı
    public SocialPost(String authorName, String timeAgo, String contentText, int imageResId, int likesCount) {
        this.authorUid = "system_mock";
        this.authorName = authorName;
        this.contentText = contentText;
        this.likesCount = likesCount;
        this.timestamp = Timestamp.now();
    }

    // Firestore'a yazma için Map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("authorUid", authorUid);
        map.put("authorName", authorName);
        map.put("contentText", contentText);
        if (imageBase64 != null) {
            map.put("imageBase64", imageBase64);
        }
        if (location != null) {
            map.put("location", location);
        }
        if (music != null) {
            map.put("music", music);
        }
        if (musicPreviewUrl != null) {
            map.put("musicPreviewUrl", musicPreviewUrl);
        }
        map.put("likesCount", likesCount);
        map.put("timestamp", timestamp);
        return map;
    }

    // Firestore'dan okunca zaman damgasını okunabilir metne çevir
    public String getTimeAgo() {
        if (timestamp == null) return "";
        long diff = (Timestamp.now().getSeconds() - timestamp.getSeconds());
        if (diff < 60) return "az önce";
        else if (diff < 3600) return (diff / 60) + " dk önce";
        else if (diff < 86400) return (diff / 3600) + " saat önce";
        else return (diff / 86400) + " gün önce";
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }
    public String getAuthorName() { return authorName; }
    public String getContentText() { return contentText; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
    public Timestamp getTimestamp() { return timestamp; }
    public int getImageResId() { return 0; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getMusic() { return music; }
    public void setMusic(String music) { this.music = music; }
    public String getMusicPreviewUrl() { return musicPreviewUrl; }
    public void setMusicPreviewUrl(String musicPreviewUrl) { this.musicPreviewUrl = musicPreviewUrl; }
}
