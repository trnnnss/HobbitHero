package com.example.hobbithero.data;

import com.example.hobbithero.ui.Habit;
import java.util.ArrayList;
import java.util.List;

public class HabitRepository {
    public static List<Habit> getPredefinedHabits() {
        List<Habit> list = new ArrayList<>();
        list.add(new Habit("Günde 2 Litre Su İç", 0, false, 10, "Sağlık"));
        list.add(new Habit("30 Dakika Kitap Oku", 0, false, 20, "Eğitim"));
        list.add(new Habit("45 Dakika Egzersiz Yap", 0, false, 50, "Spor"));
        list.add(new Habit("10 Dakika Meditasyon", 0, false, 15, "Zihin"));
        list.add(new Habit("Şekersiz Bir Gün", 0, false, 30, "Sağlık"));
        list.add(new Habit("Erken Kalk (07:00)", 0, false, 40, "Düzen"));
        return list;
    }
}
