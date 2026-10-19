package com.example.hobbithero.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hobbithero.R;
import com.example.hobbithero.databinding.FragmentRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // Kayit ol butonu
        binding.btnRegister.setOnClickListener(v -> {
            String name     = binding.etRegisterName.getText().toString().trim();
            String username = binding.etRegisterUsername.getText().toString().trim().toLowerCase();
            String email    = binding.etRegisterEmail.getText().toString().trim();
            String password = binding.etRegisterPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.contains(" ")) {
                Toast.makeText(getContext(), "Kullanıcı adı boşluk içeremez.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(getContext(), "Şifre en az 6 karakter olmalı.", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnRegister.setEnabled(false);

            // Kullanıcı adı benzersizlik kontrolü
            db.collection("users").whereEqualTo("username", username).get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            binding.btnRegister.setEnabled(true);
                            Toast.makeText(getContext(), "Bu kullanıcı adı zaten alınmış.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Kayda devam et
                            performRegistration(name, username, email, password);
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(getContext(), "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Giris Yap'a don
        binding.tvGoToLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_registerFragment_to_loginFragment));
    }

    private void performRegistration(String name, String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) return;

                    // DisplayName guncelle
                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                    user.updateProfile(profileUpdates);

                    // Firestore'a kullanici belgesi yaz
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid",           user.getUid());
                    userData.put("displayName",   name);
                    userData.put("username",      username);
                    // searchName = "isim soyisim @kullanıcıadı" (Aramada her ikisiyle de çıksın)
                    String sn = (name + " @" + username).toLowerCase(new Locale("tr", "TR"));
                    userData.put("searchName",    sn);
                    userData.put("email",         email);
                    userData.put("bio",           "HabitHero üyesi 💪");
                    userData.put("totalXp",       0L);
                    userData.put("level",         1L);
                    userData.put("postCount",     0L);
                    userData.put("followerCount", 0L);
                    userData.put("followingCount",0L);
                    userData.put("createdAt",     FieldValue.serverTimestamp());

                    db.collection("users").document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(unused -> {
                                if (getContext() == null) return;
                                Toast.makeText(getContext(), "Kayıt başarılı! Giriş yapabilirsiniz.", Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(this)
                                        .navigate(R.id.action_registerFragment_to_loginFragment);
                            });
                })
                .addOnFailureListener(e -> {
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(getContext(), "Kayıt Hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
