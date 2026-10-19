package com.example.hobbithero.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hobbithero.R;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.chip.ChipGroup;

import android.net.Uri;
import android.content.Intent;
import java.util.Arrays;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private LatLng currentLocation = new LatLng(41.0082, 28.9784); // Varsayılan İstanbul

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    getCurrentLocationAndSetupMap();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    getCurrentLocationAndSetupMap();
                } else {
                    Toast.makeText(getContext(), "Konum izni verilmediği için varsayılan konum kullanılıyor.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Places API Başlatma (Güvenlik için Manifest'ten alınması daha iyidir ancak test için buraya yazıldı)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyCGq1VCtL_FZB8qIemYCdygAXjDzCx9fzc");
        }
        placesClient = Places.createClient(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupFilters(view);
    }

    private void setupFilters(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupFilters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || mMap == null) return;
            
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipGym) {
                searchPlaces(Arrays.asList("gym", "fitness_center"));
            } else if (checkedId == R.id.chipPark) {
                searchPlaces(Arrays.asList("park"));
            } else if (checkedId == R.id.chipPool) {
                searchPlaces(Arrays.asList("swimming_pool"));
            } else if (checkedId == R.id.chipStadium) {
                searchPlaces(Arrays.asList("stadium", "sports_complex"));
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Rota çizmek veya Görev tamamlamak için Marker InfoWindow tıklama olayı
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.getTitle().contains("Görevi") || marker.getTitle().contains("Huzur") || marker.getTitle().contains("Maraton")) {
                // Görev Check-in Simülasyonu
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("🌟 Görev Tamamla")
                        .setMessage(marker.getTitle() + " bölgesindesin. Görevi tamamlayıp XP kazanmak istiyor musun?")
                        .setPositiveButton("Evet, Tamamla!", (d, w) -> {
                            Toast.makeText(getContext(), "🎉 Harika! 100 XP kazandın!", Toast.LENGTH_LONG).show();
                            marker.remove(); // Görevi haritadan kaldır
                        })
                        .setNegativeButton("Daha Sonra", null)
                        .show();
                return;
            }

            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + marker.getPosition().latitude + "," + marker.getPosition().longitude + "&mode=w");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + marker.getPosition().latitude + "," + marker.getPosition().longitude)));
            }
        });

        checkPermissionsAndGetLocation();
    }

    private void checkPermissionsAndGetLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndSetupMap();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 13f));
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocationAndSetupMap() {
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f));
                
                // İlk açılışta tüm mekan türlerini yükle
                searchPlaces(Arrays.asList("gym", "fitness_center", "park", "swimming_pool", "stadium", "sports_complex"));

                // Özel HobbitHero Görevlerini ekle
                addSpecialQuests();
            }
        });
    }

    private void addSpecialQuests() {
        if (mMap == null) return;

        // Rastgele 3-4 görev konumu oluştur (Current Location etrafında)
        LatLng q1 = new LatLng(currentLocation.latitude + 0.005, currentLocation.longitude + 0.005);
        LatLng q2 = new LatLng(currentLocation.latitude - 0.004, currentLocation.longitude + 0.003);
        LatLng q3 = new LatLng(currentLocation.latitude + 0.002, currentLocation.longitude - 0.006);

        mMap.addMarker(new MarkerOptions()
                .position(q1)
                .title("⚔️ Boss Arenası Görevi")
                .snippet("Bu bölgeye git ve 100 XP kazan!")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        mMap.addMarker(new MarkerOptions()
                .position(q2)
                .title("🧘‍♀️ Huzur Bahçesi")
                .snippet("10 dakika meditasyon yap +50 XP")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        mMap.addMarker(new MarkerOptions()
                .position(q3)
                .title("🏃‍♂️ Maraton Başlangıcı")
                .snippet("Koşmaya buradan başla! +75 XP")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
    }

    private void searchPlaces(List<String> placeTypes) {
        mMap.clear(); // Eski markerları temizle
        Toast.makeText(getContext(), "Mekanlar aranıyor...", Toast.LENGTH_SHORT).show();

        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LOCATION, Place.Field.TYPES);
        CircularBounds circle = CircularBounds.newInstance(currentLocation, 5000); // 5 km yarıçap

        SearchNearbyRequest searchNearbyRequest = SearchNearbyRequest.builder(circle, placeFields)
                .setIncludedTypes(placeTypes)
                .setMaxResultCount(20)
                .build();

        placesClient.searchNearby(searchNearbyRequest).addOnSuccessListener(response -> {
            List<Place> places = response.getPlaces();
            if (places.isEmpty()) {
                Toast.makeText(getContext(), "Bu alanda uygun mekan bulunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }
            for (Place place : places) {
                if (place.getLocation() != null) {
                    float hue = BitmapDescriptorFactory.HUE_RED; // Varsayılan: Gym (Kırmızı)
                    
                    if (place.getTypes() != null) {
                        List<Place.Type> types = place.getTypes();
                        boolean isPark     = types.stream().anyMatch(t -> t.name().contains("PARK"));
                        boolean isPool     = types.stream().anyMatch(t -> t.name().contains("POOL") || t.name().contains("SWIM"));
                        boolean isStadium  = types.stream().anyMatch(t -> t.name().contains("STADIUM") || t.name().contains("SPORTS_COMPLEX") || t.name().contains("ARENA"));

                        if (isPark) {
                            hue = BitmapDescriptorFactory.HUE_GREEN;
                        } else if (isPool) {
                            hue = BitmapDescriptorFactory.HUE_BLUE;
                        } else if (isStadium) {
                            hue = BitmapDescriptorFactory.HUE_ORANGE;
                        }
                    }

                    mMap.addMarker(new MarkerOptions()
                            .position(place.getLocation())
                            .title(place.getName())
                            .snippet("Rota oluşturmak için dokunun")
                            .icon(BitmapDescriptorFactory.defaultMarker(hue)));
                }
            }
        }).addOnFailureListener(exception -> {
            Toast.makeText(getContext(), "Arama hatası: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
