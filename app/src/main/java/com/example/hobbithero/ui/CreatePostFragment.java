package com.example.hobbithero.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hobbithero.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CreatePostFragment extends Fragment {

    private EditText etPostContent, etPostLocation, etPostMusic;
    private ImageView ivPostPreview;
    private LinearLayout layoutAddImage;
    private ImageButton btnRemoveImage;
    private MaterialButton btnSharePost, btnDeletePost;
    private ImageButton btnClosePost;
    private MaterialCardView cardImageContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String selectedImageBase64 = null;
    private ActivityResultLauncher<String> pickImageLauncher;
    private String editingPostId = null;
    private String selectedMusicUrl = null;
    
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();
    private List<SuggestionItem> suggestionList = new ArrayList<>();
    private SuggestionAdapter suggestionAdapter;
    private RecyclerView rvSuggestions;
    private ProgressBar pbSuggestions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etPostContent = view.findViewById(R.id.etPostContent);
        etPostLocation = view.findViewById(R.id.etPostLocation);
        etPostMusic = view.findViewById(R.id.etPostMusic);
        ivPostPreview = view.findViewById(R.id.ivPostPreview);
        layoutAddImage = view.findViewById(R.id.layoutAddImage);
        btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        btnSharePost = view.findViewById(R.id.btnSharePost);
        btnDeletePost = view.findViewById(R.id.btnDeletePost);
        btnClosePost = view.findViewById(R.id.btnClosePost);
        cardImageContainer = view.findViewById(R.id.cardImageContainer);
        rvSuggestions = view.findViewById(R.id.rvSuggestions);
        pbSuggestions = view.findViewById(R.id.pbSuggestions);

        setupImagePicker();

        layoutAddImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageBase64 = null;
            ivPostPreview.setImageBitmap(null);
            ivPostPreview.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
            layoutAddImage.setVisibility(View.VISIBLE);
        });

        btnClosePost.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        btnSharePost.setOnClickListener(v -> publishPost());
        btnDeletePost.setOnClickListener(v -> confirmDeletePost());

        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        suggestionAdapter = new SuggestionAdapter(suggestionList, item -> {
            if (item.type == SuggestionItem.TYPE_LOCATION) {
                etPostLocation.setText(item.title);
            } else {
                etPostMusic.setText(item.title);
                selectedMusicUrl = item.subtitle;
            }
            rvSuggestions.setVisibility(View.GONE);
        });
        rvSuggestions.setAdapter(suggestionAdapter);

        setupSearchWatchers();

        if (getArguments() != null && getArguments().containsKey("postId")) {
            editingPostId = getArguments().getString("postId");
            btnDeletePost.setVisibility(View.VISIBLE);
            loadPostForEditing();
        }
    }

    private void setupSearchWatchers() {
        TextWatcher locationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) searchLocation(s.toString());
                else rvSuggestions.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etPostLocation.addTextChangedListener(locationWatcher);

        TextWatcher musicWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) searchMusic(s.toString());
                else rvSuggestions.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etPostMusic.addTextChangedListener(musicWatcher);
    }

    private void searchLocation(String query) {
        pbSuggestions.setVisibility(View.VISIBLE);
        String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + query;
        Request request = new Request.Builder().url(url).header("User-Agent", "HobbitHeroApp").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body().string();
                JsonArray jsonArray = gson.fromJson(body, JsonArray.class);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        pbSuggestions.setVisibility(View.GONE);
                        suggestionList.clear();
                        for (int i = 0; i < Math.min(jsonArray.size(), 5); i++) {
                            JsonObject obj = jsonArray.get(i).getAsJsonObject();
                            suggestionList.add(new SuggestionItem(SuggestionItem.TYPE_LOCATION, obj.get("display_name").getAsString(), ""));
                        }
                        suggestionAdapter.notifyDataSetChanged();
                        rvSuggestions.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                }
            }
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> pbSuggestions.setVisibility(View.GONE));
            }
        });
    }

    private void searchMusic(String query) {
        pbSuggestions.setVisibility(View.VISIBLE);
        String url = "https://itunes.apple.com/search?term=" + query + "&entity=musicTrack&limit=5";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body().string();
                JsonObject root = gson.fromJson(body, JsonObject.class);
                JsonArray results = root.getAsJsonArray("results");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        pbSuggestions.setVisibility(View.GONE);
                        suggestionList.clear();
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject track = results.get(i).getAsJsonObject();
                            String title = track.get("trackName").getAsString() + " - " + track.get("artistName").getAsString();
                            String previewUrl = track.has("previewUrl") ? track.get("previewUrl").getAsString() : "";
                            suggestionList.add(new SuggestionItem(SuggestionItem.TYPE_MUSIC, title, previewUrl));
                        }
                        suggestionAdapter.notifyDataSetChanged();
                        rvSuggestions.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                }
            }
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> pbSuggestions.setVisibility(View.GONE));
            }
        });
    }

    private void loadPostForEditing() {
        btnSharePost.setText("Güncelle");
        db.collection("posts").document(editingPostId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                SocialPost post = doc.toObject(SocialPost.class);
                if (post != null) {
                    etPostContent.setText(post.getContentText());
                    if (post.getLocation() != null) etPostLocation.setText(post.getLocation());
                    if (post.getMusic() != null) etPostMusic.setText(post.getMusic());
                    selectedMusicUrl = post.getMusicPreviewUrl();
                    
                    if (post.getImageBase64() != null && !post.getImageBase64().isEmpty()) {
                        selectedImageBase64 = post.getImageBase64();
                        try {
                            byte[] decodedString = Base64.decode(post.getImageBase64(), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivPostPreview.setImageBitmap(decodedByte);
                            ivPostPreview.setVisibility(View.VISIBLE);
                            btnRemoveImage.setVisibility(View.VISIBLE);
                            layoutAddImage.setVisibility(View.GONE);
                        } catch (Exception e) {}
                    }
                }
            }
        });
    }

    private void confirmDeletePost() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Gönderiyi Sil")
                .setMessage("Bu gönderiyi silmek istediğinizden emin misiniz?")
                .setPositiveButton("Sil", (dialog, which) -> {
                    db.collection("posts").document(editingPostId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Gönderi silindi.", Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(this).navigateUp();
                            });
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    int maxWidth = 800;
                    int maxHeight = 800;
                    float scale = Math.min(((float)maxWidth / bitmap.getWidth()), ((float)maxHeight / bitmap.getHeight()));
                    Bitmap resizedBitmap = (scale < 1) ? Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()*scale), (int)(bitmap.getHeight()*scale), true) : bitmap;
                    ivPostPreview.setImageBitmap(resizedBitmap);
                    ivPostPreview.setVisibility(View.VISIBLE);
                    btnRemoveImage.setVisibility(View.VISIBLE);
                    layoutAddImage.setVisibility(View.GONE);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                    selectedImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Fotoğraf yüklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void publishPost() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String content = etPostContent.getText().toString().trim();
        String location = etPostLocation.getText().toString().trim();
        String music = etPostMusic.getText().toString().trim();
        
        if (music.isEmpty()) selectedMusicUrl = null;

        btnSharePost.setEnabled(false);
        btnSharePost.setText("İşleniyor...");

        SocialPost post = new SocialPost(
                user.getUid(),
                user.getDisplayName(),
                content,
                selectedImageBase64,
                location.isEmpty() ? null : location,
                music.isEmpty() ? null : music,
                selectedMusicUrl
        );

        if (editingPostId != null) {
            db.collection("posts").document(editingPostId).update(post.toMap())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Gönderi güncellendi!", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).navigateUp();
                    })
                    .addOnFailureListener(e -> {
                        btnSharePost.setEnabled(true);
                        btnSharePost.setText("Güncelle");
                        Toast.makeText(getContext(), "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            db.collection("posts").add(post.toMap())
                    .addOnSuccessListener(ref -> {
                        db.collection("users").document(user.getUid())
                                .update("xp", FieldValue.increment(20));
                        Toast.makeText(getContext(), "Gönderi paylaşıldı! +20 XP", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).navigateUp();
                    })
                    .addOnFailureListener(e -> {
                        btnSharePost.setEnabled(true);
                        btnSharePost.setText("Paylaş");
                        Toast.makeText(getContext(), "Hata oluştu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }
    private static class SuggestionItem {
        static final int TYPE_LOCATION = 0;
        static final int TYPE_MUSIC = 1;
        int type;
        String title;
        String subtitle;
        SuggestionItem(int type, String title, String subtitle) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.VH> {
        private List<SuggestionItem> list;
        private OnItemClickListener listener;
        interface OnItemClickListener { void onItemClick(SuggestionItem item); }
        SuggestionAdapter(List<SuggestionItem> list, OnItemClickListener listener) {
            this.list = list;
            this.listener = listener;
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_2, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            SuggestionItem item = list.get(pos);
            h.t1.setText(item.title);
            h.t2.setText(item.type == SuggestionItem.TYPE_LOCATION ? "📍 Konum" : "🎵 Müzik");
            h.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView t1, t2;
            VH(View v) { super(v); t1 = v.findViewById(android.R.id.text1); t2 = v.findViewById(android.R.id.text2); }
        }
    }
}
