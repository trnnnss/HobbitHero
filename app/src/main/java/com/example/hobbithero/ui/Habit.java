package com.example.hobbithero.ui;

public class Habit {
    private String title;
    private int streak;
    private boolean completedToday; // Simplified name for Firestore
    private int xpReward;
    private String category;

    public Habit() {} // Firestore required

    public Habit(String title, int streak, boolean completedToday, int xpReward, String category) {
        this.title = title;
        this.streak = streak;
        this.completedToday = completedToday;
        this.xpReward = xpReward;
        this.category = category;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public boolean isCompletedToday() { return completedToday; }
    public void setCompletedToday(boolean completedToday) { this.completedToday = completedToday; }

    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
