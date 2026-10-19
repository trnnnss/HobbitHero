package com.example.hobbithero;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.core.splashscreen.SplashScreen;

import com.example.hobbithero.ui.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Firestore Çevrimdışı Önbellek Ayarları
        try {
            com.google.firebase.firestore.FirebaseFirestoreSettings settings =
                    new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                            .build();
            com.google.firebase.firestore.FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (Exception e) {
            // Zaten başlatılmışsa hata verebilir, yoksay
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> insets);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            // Alt menuyu belirli sayfalarda gizle
            navController.addOnDestinationChangedListener((controller, destination, args) -> {
                int id = destination.getId();
                boolean hide = id == R.id.loginFragment
                        || id == R.id.registerFragment
                        || id == R.id.editProfileFragment;
                bottomNav.setVisibility(hide ? View.GONE : View.VISIBLE);
            });
        }

        // Oturum aciksa Firestore bildirimlerini dinle
        listenForNotifications();
    }

    private void listenForNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        notificationHelper = new NotificationHelper();
        notificationHelper.listenUnreadCount(user.getUid(), count -> {
            if (count > 0) {
                View rootView = findViewById(R.id.main);
                if (rootView != null) {
                    Snackbar.make(rootView,
                            "🔔 " + count + " yeni bildiriminiz var!",
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }
}