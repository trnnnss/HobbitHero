package com.example.hobbithero.ui;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Comment {
    private String commentId;
    private String userId;
    private String userName;
    private String text;
    private String userProfileImage;
    private Timestamp timestamp;

    public Comment() {}

    public Comment(String userId, String userName, String text, String userProfileImage) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.userProfileImage = userProfileImage;
        this.timestamp = Timestamp.now();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("text", text);
        map.put("userProfileImage", userProfileImage);
        map.put("timestamp", timestamp);
        return map;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getUserProfileImage() { return userProfileImage; }
    public void setUserProfileImage(String userProfileImage) { this.userProfileImage = userProfileImage; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
