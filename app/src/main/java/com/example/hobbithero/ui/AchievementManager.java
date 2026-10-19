package com.example.hobbithero.ui;

import java.util.Calendar;

public class AchievementManager {

    // Gizli başarımları kontrol eder (Easter Eggs)
    // Başarım açılırsa mesaj döndürür, aksi halde null döndürür
    public static String checkAchievements(Habit habit, int earnedXp) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        // 1. Başarım: Şafak Bekçisi
        if (habit.getCategory().equals("Disiplin") && hour < 8 && earnedXp > habit.getXpReward()) {
            return "🌟 GİZLİ BAŞARIM: Şafak Bekçisi Kilit Açıldı! (Kritik XP)";
        }
        
        // 2. Başarım: Sağlık Şövalyesi
        if (habit.getCategory().equals("Sağlık") && habit.getStreak() >= 5) {
            return "🛡️ BAŞARIM: Sağlık Şövalyesi (5 Günlük Seri)";
        }
        
        return null;
    }
}
