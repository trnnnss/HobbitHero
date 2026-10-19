package com.example.hobbithero.ui;

import java.util.HashMap;
import java.util.Map;

public class UserSession {
    private static UserSession instance;
    private int currentXp = 60; // Mock başlangıç XP'si
    private int totalXp = 560; // Mock total XP
    private int level = 5; // Mock başlangıç seviyesi
    
    // Kategori Bazlı XP (Radar Grafik için)
    private Map<String, Integer> categoryXpMap;
    
    private UserSession() {
        categoryXpMap = new HashMap<>();
        // Başlangıç Mock verileri
        categoryXpMap.put("Disiplin", 200);
        categoryXpMap.put("Sağlık", 150);
        categoryXpMap.put("Zihin", 180);
        categoryXpMap.put("Sosyallik", 90);
        categoryXpMap.put("Spor", 120);
    }
    
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    public int getXp() { return currentXp; }
    public int getTotalXp() { return totalXp; }
    public int getLevel() { return level; }
    public Map<String, Integer> getCategoryXpMap() { return categoryXpMap; }
    
    public void setLevel(int level) { this.level = level; }
    public void setTotalXp(int totalXp) { 
        this.totalXp = totalXp; 
        this.currentXp = totalXp % 100; // Her seviye 100 XP kabul ediliyor
    }
    
    public int getRequiredXpForNextLevel() {
        // Karesel (Polinom) Eğrisi: Taban XP (100) + (Seviye^2 * Katsayı)
        return 100 + (level * level * 20); 
    }
    
    public boolean addXp(int amount, String category) {
        currentXp += amount;
        totalXp += amount;
        
        // Kategori puanını artır
        if (category != null && !category.isEmpty()) {
            int currentCatXp = categoryXpMap.getOrDefault(category, 0);
            categoryXpMap.put(category, currentCatXp + amount);
        }
        
        boolean leveledUp = false;
        // Dinamik XP hesaplaması: Gerekli XP'yi aştığı sürece seviye atlat
        while (currentXp >= getRequiredXpForNextLevel()) {
            currentXp -= getRequiredXpForNextLevel();
            level++;
            leveledUp = true;
        }
        return leveledUp;
    }
    
    // Geriye dönük uyumluluk için
    public boolean addXp(int amount) {
        return addXp(amount, "Genel");
    }
}
