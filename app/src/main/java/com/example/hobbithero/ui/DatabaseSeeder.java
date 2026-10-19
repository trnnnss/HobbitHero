package com.example.hobbithero.ui;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Demo veritabanı tohumlayıcı.
 * 3 hazır kullanıcı, postları ve takip ilişkilerini Firestore'a ekler.
 *
 * Kullanıcılar:
 *   ali@hobbithero.com   / HabitHero2024
 *   elif@hobbithero.com  / HabitHero2024
 *   can@hobbithero.com   / HabitHero2024
 *
 * Çalıştırmak için: DatabaseSeeder.seed();
 * Dashboard'daki "Veritabanını Hazırla" butonuna basıldığında çalışır.
 */
public class DatabaseSeeder {

    private static final String TAG = "DatabaseSeeder";

    // ─── Demo kullanıcı bilgileri ───────────────────────────────────────────
    private static final String[][] USERS = {
        // { email, password, displayName, bio, xp, level }
        {"ali@hobbithero.com",  "HabitHero2024", "Ali Yılmaz",
         "Her gün koş, her gün kazan 🏃 | Spor & Kitap", "1520", "15"},
        {"elif@hobbithero.com", "HabitHero2024", "Elif Şahin",
         "Meditasyon ve zihin yönetimi 🧘 | Zaman Efendisi", "2340", "22"},
        {"can@hobbithero.com",  "HabitHero2024", "Can Demir",
         "Yazılım + Spor + Beslenme 💻 | Verimlilik odaklı", "890", "8"},
    };

    // ─── Her kullanıcının postları ─────────────────────────────────────────
    private static final String[][] ALI_POSTS = {
        {"Sabah 06:00'da 5 km koşu tamamlandı! Düzenli sabah antrenmanlarının etkisini artık çok daha net hissediyorum. Sizler de deneyin! 💪🏃‍♂️", "14"},
        {"Bugün 'Atomik Alışkanlıklar' kitabını bitirdim. Tavsiye edeceğim en iyi kitap. Küçük değişiklikler büyük sonuçlar doğuruyor! 📚", "28"},
        {"30 günlük su içme serim tamamlandı 💧 Cilt durumu, enerji seviyesi, konsantrasyon — hepsinde fark var!", "45"},
    };

    private static final String[][] ELIF_POSTS = {
        {"Günde 20 dakika meditasyon yapmanın üzerinden 3 ay geçti. Stres yönetimi konusunda inanılmaz gelişme kaydettim. 🧘‍♀️✨", "62"},
        {"Bugün hem yoga hem de 1000 kelime yazdım. Zihin + beden dengesi kurulduğunda her şey mümkün! 🌟", "38"},
        {"Bu hafta hiç sosyal medyaya bakmadım. Odaklanma süremi ölçtüm — tam 4 saat derin çalışma! 🚀", "91"},
    };

    private static final String[][] CAN_POSTS = {
        {"100 günlük kodlama serisinde 42. gün! Bugün ilk full-stack projemi deploy ettim. Gurur dolu bir gün 🖥️🔥", "33"},
        {"Protein hedefimi 3 haftadır tutturuyorum. Beslenme + egzersiz kombinesinin gücü tartışılmaz! 💪🥗", "19"},
    };

    private static final FirebaseFirestore db  = FirebaseFirestore.getInstance();
    private static final FirebaseAuth      auth = FirebaseAuth.getInstance();

    /** Tüm seed işlemini başlat */
    public static void seed(Runnable onComplete) {
        // Mevcut kullanıcıların eksik kullanıcı adlarını düzelt (Migration)
        fixAllUsernames();

        // Önce Firestore'da zaten seed'lenmiş mi kontrol et
        db.collection("users").limit(3).get()
                .addOnSuccessListener(snap -> {
                    if (snap.size() >= 3) {
                        Log.d(TAG, "Veritabanı zaten dolu, seed atlandı.");
                        if (onComplete != null) onComplete.run();
                        return;
                    }
                    // 3'ten az kullanıcı varsa seed et
                    createUser(0, new String[3], onComplete);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Kontrol hatası: " + e.getMessage()));
    }

    /** 
     * Veritabanındaki bütün kullanıcıları gezer, 
     * kullanıcı adı (username) veya arama alanı (searchName) eksik olanları doldurur.
     */
    public static void fixAllUsernames() {
        db.collection("users").get().addOnSuccessListener(snapshots -> {
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                String uid = doc.getId();
                String username = doc.getString("username");
                String searchName = doc.getString("searchName");
                String displayName = doc.getString("displayName");

                if (displayName == null) displayName = "Anonim";

                boolean needsFix = false;
                Map<String, Object> updates = new HashMap<>();

                if (username == null || username.isEmpty()) {
                    username = displayName.toLowerCase(new Locale("tr", "TR")).replace(" ", "");
                    updates.put("username", username);
                    needsFix = true;
                }

                if (searchName == null || searchName.isEmpty()) {
                    String sn = (displayName + " @" + username).toLowerCase(new Locale("tr", "TR"));
                    updates.put("searchName", sn);
                    needsFix = true;
                }

                if (needsFix) {
                    db.collection("users").document(uid).update(updates)
                            .addOnSuccessListener(v -> Log.d(TAG, "Kullanıcı güncellendi: " + uid))
                            .addOnFailureListener(e -> Log.e(TAG, "Güncelleme hatası (" + uid + "): " + e.getMessage()));
                }
            }
        });
    }

    /** USERS dizisinde sırayla kullanıcı oluştur */
    private static void createUser(int index, String[] uids, Runnable onComplete) {
        if (index >= USERS.length) {
            // Tüm kullanıcılar oluşturuldu — takip ilişkileri ve postları ekle
            seedFollows(uids);
            seedPosts(uids, onComplete);
            return;
        }

        String email    = USERS[index][0];
        String password = USERS[index][1];
        String name     = USERS[index][2];
        String bio      = USERS[index][3];
        long   xp       = Long.parseLong(USERS[index][4]);
        long   level    = Long.parseLong(USERS[index][5]);

        // Önce giriş dene (kullanıcı zaten varsa)
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    uids[index] = uid;
                    writeUserDoc(uid, name, email, bio, xp, level);
                    // Profil adı güncelle
                    result.getUser().updateProfile(
                            new UserProfileChangeRequest.Builder().setDisplayName(name).build());
                    auth.signOut();
                    createUser(index + 1, uids, onComplete);
                })
                .addOnFailureListener(e -> {
                    // Giriş olmadı — yeni hesap oluştur
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(result2 -> {
                                String uid = result2.getUser().getUid();
                                uids[index] = uid;
                                writeUserDoc(uid, name, email, bio, xp, level);
                                result2.getUser().updateProfile(
                                        new UserProfileChangeRequest.Builder()
                                                .setDisplayName(name).build());
                                auth.signOut();
                                createUser(index + 1, uids, onComplete);
                            })
                            .addOnFailureListener(e2 ->
                                Log.e(TAG, "Kullanıcı oluşturulamadı: " + e2.getMessage()));
                });
    }

    /** Kullanıcı Firestore belgesini oluştur/güncelle */
    private static void writeUserDoc(String uid, String name, String email,
                                     String bio, long xp, long level) {
        String username = name.toLowerCase(new Locale("tr", "TR")).replace(" ", "");
        Map<String, Object> user = new HashMap<>();
        user.put("uid",           uid);
        user.put("displayName",   name);
        user.put("username",      username);
        user.put("searchName",    (name + " @" + username).toLowerCase(new Locale("tr", "TR")));
        user.put("email",         email);
        user.put("bio",           bio);
        user.put("totalXp",       xp);
        user.put("level",         level);
        user.put("postCount",     0L);
        user.put("followerCount", 0L);
        user.put("followingCount",0L);
        user.put("createdAt",     FieldValue.serverTimestamp());
        db.collection("users").document(uid).set(user);
    }

    /** Postları Firestore'a yaz */
    private static void seedPosts(String[] uids, Runnable onComplete) {
        String[][] allPosts = {
            // { authorUid-index, content, likesCount, minutesAgo }
        };

        // Ali postları
        writePost(uids[0], "Ali Yılmaz",  ALI_POSTS[0][0], Long.parseLong(ALI_POSTS[0][1]), 30);
        writePost(uids[0], "Ali Yılmaz",  ALI_POSTS[1][0], Long.parseLong(ALI_POSTS[1][1]), 120);
        writePost(uids[0], "Ali Yılmaz",  ALI_POSTS[2][0], Long.parseLong(ALI_POSTS[2][1]), 1440);

        // Elif postları
        writePost(uids[1], "Elif Şahin",  ELIF_POSTS[0][0], Long.parseLong(ELIF_POSTS[0][1]), 10);
        writePost(uids[1], "Elif Şahin",  ELIF_POSTS[1][0], Long.parseLong(ELIF_POSTS[1][1]), 200);
        writePost(uids[1], "Elif Şahin",  ELIF_POSTS[2][0], Long.parseLong(ELIF_POSTS[2][1]), 2880);

        // Can postları
        writePost(uids[2], "Can Demir",   CAN_POSTS[0][0], Long.parseLong(CAN_POSTS[0][1]), 60);
        writePost(uids[2], "Can Demir",   CAN_POSTS[1][0], Long.parseLong(CAN_POSTS[1][1]), 300);

        // postCount güncelle
        updatePostCount(uids[0], 3);
        updatePostCount(uids[1], 3);
        updatePostCount(uids[2], 2);

        Log.d(TAG, "Postlar yazıldı.");
        if (onComplete != null) onComplete.run();
    }

    private static void writePost(String authorUid, String authorName,
                                  String content, long likes, int minutesAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -minutesAgo);
        Timestamp ts = new Timestamp(new Date(cal.getTimeInMillis()));

        Map<String, Object> post = new HashMap<>();
        post.put("authorUid",   authorUid);
        post.put("authorName",  authorName);
        post.put("contentText", content);
        post.put("likesCount",  likes);
        post.put("timestamp",   ts);
        db.collection("posts").add(post);
    }

    private static void updatePostCount(String uid, long count) {
        db.collection("users").document(uid).update("postCount", count);
    }

    /** Takip ilişkileri: Elif→Ali, Can→Elif, Ali→Can */
    private static void seedFollows(String[] uids) {
        if (uids[0] == null || uids[1] == null || uids[2] == null) return;

        // Elif → Ali takip
        addFollow(uids[1], uids[0]);
        // Can → Elif takip
        addFollow(uids[2], uids[1]);
        // Ali → Can takip
        addFollow(uids[0], uids[2]);
        // Ali → Elif de takip
        addFollow(uids[0], uids[1]);

        // Sayaclari guncelle
        db.collection("users").document(uids[0]).update("followingCount", 2L, "followerCount", 1L);
        db.collection("users").document(uids[1]).update("followingCount", 1L, "followerCount", 2L);
        db.collection("users").document(uids[2]).update("followingCount", 1L, "followerCount", 1L);

        Log.d(TAG, "Takip ilişkileri oluşturuldu.");
    }

    private static void addFollow(String fromUid, String toUid) {
        Map<String, Object> data = new HashMap<>();
        data.put("since", FieldValue.serverTimestamp());
        db.collection("follows").document(fromUid)
                .collection("following").document(toUid).set(data);
    }

    /** Mevcut kullaniciya ornek bildirimler ekle */
    public static void seedNotifications(String targetUid) {
        if (targetUid == null) return;
        
        NotificationHelper helper = new NotificationHelper();
        helper.sendNotification(targetUid, "ali_uid", NotificationHelper.TYPE_LIKE, "Ali Yılmaz gönderini beğendi ❤️");
        helper.sendNotification(targetUid, "elif_uid", NotificationHelper.TYPE_FOLLOW, "Elif Şahin seni takip etmeye başladı.");
        helper.sendNotification(targetUid, "can_uid", "comment", "Can Demir: 'Harika bir başarı! 💪' dedi.");
        helper.sendNotification(targetUid, "system_uid", "system", "Tebrikler! Günlük su içme görevini tamamladın. 💧");
    }
}
