package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hobbithero.R;
import com.example.hobbithero.databinding.FragmentDashboardBinding;
import com.example.hobbithero.ui.viewmodel.DashboardViewModel;
import com.google.android.material.snackbar.Snackbar;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private HabitAdapter adapter;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // MVVM Entegrasyonu: İş mantığı ViewModel'e devredildi
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Kullanıcı verisini Firestore'dan çek ve oturuma yükle
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        Long totalXp = doc.getLong("totalXp");
                        Long level = doc.getLong("level");
                        if (totalXp != null) com.example.hobbithero.ui.UserSession.getInstance().setTotalXp(totalXp.intValue());
                        if (level != null) com.example.hobbithero.ui.UserSession.getInstance().setLevel(level.intValue());
                        
                        // ViewModel'e güncel değerleri yansıtmasını söyle
                        viewModel.updateSessionStats(); 
                    }
                });
        }

        binding.rvHabits.setLayoutManager(new LinearLayoutManager(getContext()));

        // Görev Listesini (Habits) Dinle
        viewModel.getHabits().observe(getViewLifecycleOwner(), habits -> {
            if (adapter == null) {
                adapter = new HabitAdapter(habits, (habit, position) -> {
                    // ✅ Habit'e tıklanınca hem XP işlemi hem de Detail sayfasına geçiş
                    viewModel.onHabitStatusChanged(habit, true);

                    Bundle args = new Bundle();
                    args.putString("title",       habit.getTitle());
                    args.putString("description", "Kategori: " + habit.getCategory()
                            + " · Seri: " + habit.getStreak() + " gün"
                            + " · XP: +" + habit.getXpReward());
                    args.putString("url",         "https://musclewiki.com/tr-tr");

                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_dashboardFragment_to_detailFragment, args);
                });
                binding.rvHabits.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        });

        // XP'yi Dinle
        viewModel.getXp().observe(getViewLifecycleOwner(), xp -> {
            binding.pbXp.setProgress(xp);
        });

        // XP Max Dinle (Dinamik Seviyeleme)
        viewModel.getXpMax().observe(getViewLifecycleOwner(), max -> {
            binding.pbXp.setMax(max);
        });

        // Seviyeyi Dinle
        viewModel.getLevel().observe(getViewLifecycleOwner(), level -> {
            binding.tvUserLevel.setText("Seviye " + level);
        });

        // Seviye Atlama Etkinliğini Dinle
        viewModel.getLevelUpEvent().observe(getViewLifecycleOwner(), leveledUp -> {
            if (leveledUp != null && leveledUp) {
                Snackbar.make(binding.getRoot(), "🎉 Tebrikler! Seviye Atladın!", Snackbar.LENGTH_LONG).show();
                
                // Konfeti animasyonunu oynat
                if (binding.lottieConfetti != null) {
                    binding.lottieConfetti.setVisibility(View.VISIBLE);
                    binding.lottieConfetti.playAnimation();
                    
                    binding.lottieConfetti.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
                        @Override public void onAnimationStart(android.animation.Animator animation) {}
                        @Override public void onAnimationEnd(android.animation.Animator animation) {
                            binding.lottieConfetti.setVisibility(View.GONE);
                        }
                        @Override public void onAnimationCancel(android.animation.Animator animation) {
                            binding.lottieConfetti.setVisibility(View.GONE);
                        }
                        @Override public void onAnimationRepeat(android.animation.Animator animation) {}
                    });
                }
                
                viewModel.resetLevelUpEvent();
            }
        });

        // Başarım (Easter Egg) Etkinliğini Dinle
        viewModel.getAchievementEvent().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(android.R.color.holo_orange_dark))
                        .show();
            }
        });

        // FAB Butonu ile Bottom Sheet aç
        binding.fabAddHabit.setOnClickListener(v -> {
            HabitSelectionBottomSheet bottomSheet = new HabitSelectionBottomSheet();
            bottomSheet.show(getParentFragmentManager(), "HabitSelection");
        });

        // Boss Battle Butonu
        if (binding.btnBossBattle != null) {
            binding.btnBossBattle.setOnClickListener(v -> {
                Navigation.findNavController(requireView()).navigate(R.id.bossChallengeFragment);
            });
        }

        // Bildirimler Butonu
        if (binding.btnNotifications != null) {
            binding.btnNotifications.setOnClickListener(v -> {
                NotificationsBottomSheet sheet = new NotificationsBottomSheet();
                sheet.show(getParentFragmentManager(), "Notifications");
                binding.tvNotificationBadge.setVisibility(View.GONE);
            });
        }

        listenForNotifications();

        // Demo verilerini ilk acilista yukle (veritabani bossa)
        DatabaseSeeder.seed(() -> {
            // Veritabani hazir
        });
    }

    private void listenForNotifications() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || binding == null) return;

        db.collection("notifications").document(user.getUid())
                .collection("items")
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || binding == null) return;
                    int unreadCount = snapshots.size();
                    if (unreadCount > 0) {
                        binding.tvNotificationBadge.setVisibility(View.VISIBLE);
                        binding.tvNotificationBadge.setText(String.valueOf(unreadCount));
                    } else {
                        binding.tvNotificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
