package com.example.hobbithero.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hobbithero.ui.AchievementManager;
import com.example.hobbithero.ui.Habit;
import com.example.hobbithero.ui.UserSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardViewModel extends ViewModel {
    private final MutableLiveData<List<Habit>> habitsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> xpLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> levelLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> levelUpEvent = new MutableLiveData<>();
    private final MutableLiveData<String> achievementEvent = new MutableLiveData<>();

    private List<Habit> currentHabits = new ArrayList<>();
    private UserSession session;
    private boolean isAddingDefaults = false;

    public DashboardViewModel() {
        session = UserSession.getInstance();
        loadInitialData(); // Now loads from Firestore
        updateSessionStats();
    }

    private void loadInitialData() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .collection("habits")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    if (snapshots.isEmpty()) {
                        if (!isAddingDefaults) {
                            isAddingDefaults = true;
                            addDefaultHabits();
                        }
                    } else {
                        currentHabits.clear();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                            Habit h = doc.toObject(Habit.class);
                            currentHabits.add(h);
                        }
                        habitsLiveData.setValue(new ArrayList<>(currentHabits));
                    }
                });
    }

    private void addDefaultHabits() {
        List<Habit> defaults = new ArrayList<>();
        defaults.add(new Habit("8 Saat Uyku", 0, false, 15, "Sağlık"));
        defaults.add(new Habit("Kitap Oku (20 Sayfa)", 0, false, 20, "Zihin"));
        defaults.add(new Habit("Günlük Egzersiz", 0, false, 25, "Güç"));
        defaults.add(new Habit("Su İç (2 Litre)", 0, false, 10, "Sağlık"));

        for (Habit h : defaults) {
            addHabit(h);
        }
    }

    public LiveData<List<Habit>> getHabits() {
        return habitsLiveData;
    }

    public LiveData<Integer> getXp() {
        return xpLiveData;
    }

    public LiveData<Integer> getLevel() {
        return levelLiveData;
    }

    public LiveData<Boolean> getLevelUpEvent() {
        return levelUpEvent;
    }

    public LiveData<String> getAchievementEvent() {
        return achievementEvent;
    }

    public void onHabitStatusChanged(Habit habit, boolean isChecked) {
        habit.setCompletedToday(isChecked);
        if (isChecked) {
            // Bulanık Mantık ile Dinamik XP Hesapla
            int finalXp = calculateDynamicXp(habit);
            
            // Başarımları (Easter Eggs) Kontrol Et
            String achievementMessage = AchievementManager.checkAchievements(habit, finalXp);
            if (achievementMessage != null) {
                achievementEvent.setValue(achievementMessage);
            }
            
            boolean leveledUp = session.addXp(finalXp, habit.getCategory());
            if (leveledUp) {
                levelUpEvent.setValue(true);
            }

            // Kullanıcının XP ve Seviyesini Firestore'a kaydet
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .update(
                        "totalXp", session.getTotalXp(),
                        "level", session.getLevel()
                    );
            }

            // Boss HP Düşürme
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("bosses").document("currentBoss")
                .update("currentHp", com.google.firebase.firestore.FieldValue.increment(-finalXp))
                .addOnFailureListener(e -> {
                    // Doküman yoksa vs. yoksayılabilir
                });

            // Alışkanlık durumunu Firestore'da güncelle
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .collection("habits").document(habit.getTitle())
                .update("completedToday", true, "streak", habit.getStreak());
        }
        updateSessionStats();
        // Listeyi tetikle
        habitsLiveData.setValue(new ArrayList<>(currentHabits));
    }
    
    // Fuzzy Logic tabanlı Dinamik Puanlama
    private int calculateDynamicXp(Habit habit) {
        int baseReward = habit.getXpReward();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (habit.getCategory().equals("Disiplin") && hour < 8) {
            // Sabah erken yapılan disiplin görevi (Kritik Vuruş x2)
            return baseReward * 2;
        } else if (habit.getCategory().equals("Sağlık") && hour >= 20) {
            // Gece yapılan rahatlama/sağlık görevleri (%50 Bonus)
            return (int) (baseReward * 1.5);
        } else if (habit.getStreak() >= 5) {
            // 5 günden uzun seriler her zaman %20 bonus verir
            return (int) (baseReward * 1.2);
        }
        
        return baseReward;
    }

    public void addHabit(Habit habit) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .collection("habits").document(habit.getTitle())
                .set(habit);
        }
    }

    private final MutableLiveData<Integer> xpMaxLiveData = new MutableLiveData<>();

    public LiveData<Integer> getXpMax() {
        return xpMaxLiveData;
    }

    public void updateSessionStats() {
        xpLiveData.setValue(session.getXp());
        levelLiveData.setValue(session.getLevel());
        xpMaxLiveData.setValue(session.getRequiredXpForNextLevel());
    }

    public void resetLevelUpEvent() {
        levelUpEvent.setValue(false);
    }
}
