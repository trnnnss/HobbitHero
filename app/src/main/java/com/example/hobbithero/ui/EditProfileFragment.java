package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hobbithero.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class EditProfileFragment extends Fragment {

    private TextInputEditText etDisplayName, etBio;
    private MaterialButton btnSave, btnCancel;
    private ImageView ivEditAvatar;
    private TextView tvChangePhoto;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedImageBase64 = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etDisplayName = view.findViewById(R.id.etEditDisplayName);
        etBio         = view.findViewById(R.id.etEditBio);
        btnSave       = view.findViewById(R.id.btnSaveProfile);
        btnCancel     = view.findViewById(R.id.btnCancelEdit);
        ivEditAvatar  = view.findViewById(R.id.ivEditAvatar);
        tvChangePhoto = view.findViewById(R.id.tvChangePhoto);

        // Fotoğraf seçici
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    
                    // Resmi küçült
                    int maxWidth = 400;
                    int maxHeight = 400;
                    float scale = Math.min(((float)maxWidth / bitmap.getWidth()), ((float)maxHeight / bitmap.getHeight()));
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()*scale), (int)(bitmap.getHeight()*scale), true);
                    
                    ivEditAvatar.setImageBitmap(resizedBitmap);

                    // Base64'e çevir
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] imageBytes = baos.toByteArray();
                    selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvChangePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        ivEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Mevcut bilgileri doldur
        prefillCurrentData();

        btnSave.setOnClickListener(v -> saveProfile(view));
        btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
    }

    /** Firebase Auth ve Firestore'dan mevcut verileri alip alanlara doldur */
    private void prefillCurrentData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Kullanici adini hemen goster
        String name = user.getDisplayName();
        if (name != null) etDisplayName.setText(name);

        // Bio ve Fotoğrafı Firestore'dan cek
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String bio = doc.getString("bio");
                    if (bio != null) etBio.setText(bio);
                    
                    String base64Image = doc.getString("profileImageBase64");
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivEditAvatar.setImageBitmap(decodedByte);
                        } catch (Exception ignored) {}
                    }
                });
    }

    /** Degisiklikleri Firebase Auth + Firestore'a kaydet */
    private void saveProfile(View rootView) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String newName = etDisplayName.getText() != null
                ? etDisplayName.getText().toString().trim() : "";
        String newBio  = etBio.getText() != null
                ? etBio.getText().toString().trim() : "";

        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "Kullanıcı adı boş olamaz.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Kaydediliyor...");

        // Firebase Auth displayName guncelle
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        user.updateProfile(profileUpdates)
                .addOnSuccessListener(unused -> {
                    // Firestore users belgesi guncelle
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("displayName", newName);
                    updates.put("bio", newBio);

                    // Arama alanlarını ve kullanıcı adını da güncelle (Tutarlılık için)
                    String username = newName.toLowerCase(new java.util.Locale("tr", "TR")).replace(" ", "");
                    String searchName = (newName + " @" + username).toLowerCase(new java.util.Locale("tr", "TR"));
                    updates.put("username", username);
                    updates.put("searchName", searchName);

                    if (selectedImageBase64 != null) {
                        updates.put("profileImageBase64", selectedImageBase64);
                    }

                    db.collection("users").document(user.getUid())
                            .update(updates)
                            .addOnSuccessListener(v2 -> {
                                Snackbar.make(rootView, "✅ Profil güncellendi!", Snackbar.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(this).navigateUp();
                            })
                            .addOnFailureListener(e -> {
                                btnSave.setEnabled(true);
                                btnSave.setText("💾  Değişiklikleri Kaydet");
                                Toast.makeText(getContext(), "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("💾  Değişiklikleri Kaydet");
                    Toast.makeText(getContext(), "Auth hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
