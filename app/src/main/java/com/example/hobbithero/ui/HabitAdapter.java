package com.example.hobbithero.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbithero.R;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    public interface OnHabitStatusChangeListener {
        void onStatusChanged(Habit habit, int position);
    }

    private List<Habit> habitList;
    private OnHabitStatusChangeListener listener;

    public HabitAdapter(List<Habit> habitList, OnHabitStatusChangeListener listener) {
        this.habitList = habitList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        holder.tvTitle.setText(habit.getTitle());
        holder.tvStreak.setText("Seri: " + habit.getStreak() + " Gün 🔥");
        
        if (habit.isCompletedToday()) {
            holder.ivStatus.setImageResource(android.R.drawable.checkbox_on_background);
            holder.ivStatus.setColorFilter(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.ivStatus.setImageResource(android.R.drawable.checkbox_off_background);
            holder.ivStatus.setColorFilter(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));
        }

        holder.ivStatus.setOnClickListener(v -> {
            if (!habit.isCompletedToday()) {
                habit.setCompletedToday(true);
                notifyItemChanged(position);
                if (listener != null) {
                    listener.onStatusChanged(habit, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStreak;
        ImageView ivStatus;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHabitTitle);
            tvStreak = itemView.findViewById(R.id.tvHabitStreak);
            ivStatus = itemView.findViewById(R.id.ivHabitStatus);
        }
    }
}
