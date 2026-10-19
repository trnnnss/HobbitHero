package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;
import com.example.hobbithero.data.HabitRepository;
import com.example.hobbithero.ui.viewmodel.DashboardViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class HabitSelectionBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_habit_selection_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DashboardViewModel viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        RecyclerView rv = view.findViewById(R.id.rvPredefinedHabits);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new PredefinedHabitAdapter(HabitRepository.getPredefinedHabits(), habit -> {
            viewModel.addHabit(habit);
            Toast.makeText(getContext(), habit.getTitle() + " eklendi!", Toast.LENGTH_SHORT).show();
            dismiss();
        }));

        view.findViewById(R.id.btnCreateCustomHabit).setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(getContext());
            input.setHint("Alışkanlık Adı (Örn: Kitap Okumak)");
            input.setPadding(48, 48, 48, 48);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Özel Alışkanlık Oluştur")
                    .setMessage("Kazanmak istediğin yeni alışkanlığın adını gir:")
                    .setView(input)
                    .setPositiveButton("Ekle", (dialog, which) -> {
                        String habitName = input.getText().toString().trim();
                        if (!habitName.isEmpty()) {
                            Habit customHabit = new Habit(habitName, 0, false, 50, "Özel"); // Varsayılan 50 XP
                            viewModel.addHabit(customHabit);
                            Toast.makeText(getContext(), habitName + " başarıyla eklendi!", Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            Toast.makeText(getContext(), "Alışkanlık adı boş bırakılamaz.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });
    }

    private interface OnHabitSelectedListener {
        void onSelected(Habit habit);
    }

    private class PredefinedHabitAdapter extends RecyclerView.Adapter<PredefinedHabitAdapter.ViewHolder> {
        private List<Habit> list;
        private OnHabitSelectedListener listener;

        public PredefinedHabitAdapter(List<Habit> list, OnHabitSelectedListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_predefined_habit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Habit habit = list.get(position);
            holder.title.setText(habit.getTitle());
            holder.category.setText(habit.getCategory());
            holder.xp.setText("+" + habit.getXpReward() + " XP");

            holder.itemView.setOnClickListener(v -> {
                listener.onSelected(habit);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, category, xp;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvPredefinedTitle);
                category = itemView.findViewById(R.id.tvPredefinedCategory);
                xp = itemView.findViewById(R.id.tvPredefinedXp);
            }
        }
    }
}
