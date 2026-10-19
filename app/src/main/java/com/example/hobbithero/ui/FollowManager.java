package com.example.hobbithero.ui;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Takip/Takipci sistemi yardimci sinifi.
 * Firestore yapisi:
 *   follows/{currentUid}/following/{targetUid}  -> true
 *   users/{targetUid}.followerCount += 1
 *   users/{currentUid}.followingCount += 1
 */
public class FollowManager {

    public interface FollowCallback {
        void onResult(boolean isFollowing);
    }

    private final FirebaseFirestore db;

    public FollowManager() {
        db = FirebaseFirestore.getInstance();
    }

    /** currentUid, targetUid'i takip eder */
    public void follow(String currentUid, String targetUid) {
        // follows koleksiyonu
        db.collection("follows")
                .document(currentUid)
                .collection("following")
                .document(targetUid)
                .set(new HashMap<String, Object>() {{ put("since", FieldValue.serverTimestamp()); }});

        // Sayaclari guncelle
        db.collection("users").document(targetUid)
                .update("followerCount", FieldValue.increment(1));
        db.collection("users").document(currentUid)
                .update("followingCount", FieldValue.increment(1));
    }

    /** currentUid, targetUid'i takipten cikar */
    public void unfollow(String currentUid, String targetUid) {
        db.collection("follows")
                .document(currentUid)
                .collection("following")
                .document(targetUid)
                .delete();

        db.collection("users").document(targetUid)
                .update("followerCount", FieldValue.increment(-1));
        db.collection("users").document(currentUid)
                .update("followingCount", FieldValue.increment(-1));
    }

    /** currentUid'in targetUid'i takip edip etmedigini kontrol eder */
    public void isFollowing(String currentUid, String targetUid, FollowCallback callback) {
        db.collection("follows")
                .document(currentUid)
                .collection("following")
                .document(targetUid)
                .get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists()))
                .addOnFailureListener(e -> callback.onResult(false));
    }
}
