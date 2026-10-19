package com.example.hobbithero.ui;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Bildirim yardımcı sınıfı.
 * Firestore yapisi: notifications/{targetUid}/items/{notifId}
 */
public class NotificationHelper {

    public static final String TYPE_LIKE   = "like";
    public static final String TYPE_FOLLOW = "follow";

    private final FirebaseFirestore db;

    public NotificationHelper() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Hedef kullaniciya bildirim gonder.
     * @param targetUid  bildirimi alacak kullanici
     * @param fromUid    bildirimi gönderen kullanici
     * @param type       "like" veya "follow"
     * @param message    okunabilir bildirim metni
     */
    public void sendNotification(String targetUid, String fromUid,
                                 String type, String message) {
        if (targetUid.equals(fromUid)) return; // Kendine bildirim gönderme

        Map<String, Object> notif = new HashMap<>();
        notif.put("fromUid",   fromUid);
        notif.put("type",      type);
        notif.put("message",   message);
        notif.put("read",      false);
        notif.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notifications")
                .document(targetUid)
                .collection("items")
                .add(notif);
    }

    /**
     * Okunmamis bildirim sayisini dinle (badge sayacı icin).
     */
    public void listenUnreadCount(String uid, UnreadCountListener listener) {
        db.collection("notifications")
                .document(uid)
                .collection("items")
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    listener.onCount(snap.size());
                });
    }

    public interface UnreadCountListener {
        void onCount(int count);
    }
}
