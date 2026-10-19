package com.example.hobbithero.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hobbithero.R;
import com.example.hobbithero.databinding.FragmentLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Intent;
import android.app.Activity;

import java.util.concurrent.Executor;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Zaten giris yapilmissa direkt dashboard'a git
        if (auth.getCurrentUser() != null) {
            navigateToDashboard();
            return;
        }

        // Google Sign In Launcher
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Toast.makeText(getContext(),
                                    "Google Giriş Hatası: " + e.getStatusCode(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        // Google Sign In Ayarlari
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // ── Email / Sifre ile Giris ──
        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etLoginEmail.getText().toString().trim();
            String password = binding.etLoginPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Email ve şifre boş olamaz.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Butona tıklandığını belli etmek için metni değiştiriyoruz
            binding.btnLogin.setEnabled(false);
            binding.btnLogin.setText("Giriş Yapılıyor...");
            
            // 10 saniye sonra hala giriş yapılmadıysa zaman aşımı ver
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (!binding.btnLogin.isEnabled()) {
                    binding.btnLogin.setEnabled(true);
                    binding.btnLogin.setText("GİRİŞ YAP");
                    Toast.makeText(requireContext(), "Bağlantı zaman aşımına uğradı. Emülatörün internetini kontrol edin!", Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(timeoutRunnable, 10000);
            
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        handler.removeCallbacks(timeoutRunnable); // İşlem bittiyse zaman aşımını iptal et
                    })
                    .addOnSuccessListener(result -> {
                        Toast.makeText(requireContext(), "Hoş geldin! 👋", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    })
                    .addOnFailureListener(e -> {
                        binding.btnLogin.setEnabled(true);
                        binding.btnLogin.setText("GİRİŞ YAP");
                        Toast.makeText(requireContext(),
                                "Hata: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });

        // Google Butonu
        binding.btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });

        // Kayit Ol
        binding.tvGoToRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_loginFragment_to_registerFragment));

        // Demo Kullanıcı Doldurma Butonları
        binding.btnDemoAli.setOnClickListener(v -> {
            binding.etLoginEmail.setText("ali@hobbithero.com");
            binding.etLoginPassword.setText("HabitHero2024");
        });

        binding.btnDemoElif.setOnClickListener(v -> {
            binding.etLoginEmail.setText("elif@hobbithero.com");
            binding.etLoginPassword.setText("HabitHero2024");
        });

        binding.btnDemoCan.setOnClickListener(v -> {
            binding.etLoginEmail.setText("can@hobbithero.com");
            binding.etLoginPassword.setText("HabitHero2024");
        });

        // Biyometrik giris (opsiyonel) - Şimdilik otomatik açılmasını engelliyoruz, giriş butonunu bozabiliyor.
        // showBiometricPrompt();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    Toast.makeText(getContext(), "Google ile Giriş Başarılı!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Giriş Başarısız.", Toast.LENGTH_SHORT).show());
    }

    private void navigateToDashboard() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_loginFragment_to_dashboardFragment);
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        Toast.makeText(getContext(), "Biyometrik giriş başarılı!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    }
                    @Override public void onAuthenticationError(int code, @NonNull CharSequence err) {}
                    @Override public void onAuthenticationFailed() {}
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("HabitHero Giriş")
                .setSubtitle("Güvenli ve hızlı giriş için doğrulama yapın")
                .setNegativeButtonText("Şifre ile Gir")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
