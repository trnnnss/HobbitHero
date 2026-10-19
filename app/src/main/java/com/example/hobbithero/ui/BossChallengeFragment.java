package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hobbithero.R;
import com.example.hobbithero.ui.viewmodel.DashboardViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class BossChallengeFragment extends Fragment {

    private ProgressBar pbBossHp;
    private TextView tvBossHpText, tvBossName;
    private MaterialButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration bossListener;
    
    // We can also connect to DashboardViewModel if we want local state, 
    // but Firestore is better for shared boss state.
    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boss_challenge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        pbBossHp = view.findViewById(R.id.pbBossHp);
        tvBossHpText = view.findViewById(R.id.tvBossHpText);
        tvBossName = view.findViewById(R.id.tvBossName);
        btnBack = view.findViewById(R.id.btnBackToDashboard);

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        loadBossData();
    }

    private void loadBossData() {
        // "bosses/currentBoss" dökümanını dinleyelim
        bossListener = db.collection("bosses").document("currentBoss")
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null) return;
                    
                    if (!doc.exists()) {
                        // Boss yoksa oluştur
                        createInitialBoss();
                        return;
                    }

                    Long hp = doc.getLong("currentHp");
                    Long maxHp = doc.getLong("maxHp");
                    String name = doc.getString("name");

                    if (hp == null) hp = 1000L;
                    if (maxHp == null) maxHp = 1000L;
                    if (name == null) name = "Tembellik İblisi";

                    tvBossName.setText(name);
                    pbBossHp.setMax(maxHp.intValue());
                    pbBossHp.setProgress(hp.intValue());
                    tvBossHpText.setText(hp + " / " + maxHp + " HP");

                    if (hp <= 0) {
                        Toast.makeText(getContext(), "🎉 Boss'u Yendin! Yeni Boss Geliyor...", Toast.LENGTH_LONG).show();
                        createInitialBoss(); // Reset for now
                    }
                });
    }

    private void createInitialBoss() {
        Map<String, Object> bossData = new HashMap<>();
        bossData.put("name", "Tembellik İblisi Seviye 2");
        bossData.put("maxHp", 2000L);
        bossData.put("currentHp", 2000L);
        
        db.collection("bosses").document("currentBoss").set(bossData);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bossListener != null) bossListener.remove();
    }
}
