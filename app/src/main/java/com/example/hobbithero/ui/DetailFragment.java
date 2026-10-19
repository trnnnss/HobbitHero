package com.example.hobbithero.ui;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hobbithero.R;

public class DetailFragment extends Fragment {

    private WebView webView;
    private ProgressBar progressBar;

    // Context Menu item ID'leri
    private static final int MENU_OPEN_URL   = 1001;
    private static final int MENU_COPY_URL   = 1002;
    private static final int MENU_RELOAD     = 1003;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView    = view.findViewById(R.id.wvDetailTips);
        progressBar = view.findViewById(R.id.progressBarWeb);

        setupWebView();
        registerForContextMenu(webView);

        // Fragment argümanlarından başlık / açıklama / url al
        Bundle args = getArguments();
        String url  = (args != null) ? args.getString("url", getDefaultUrl()) : getDefaultUrl();
        String title = (args != null) ? args.getString("title", "Sabah Yürüyüşü") : "Sabah Yürüyüşü";
        String desc  = (args != null) ? args.getString("description", "Her sabah 30 dakika tempolu yürüyüş yap.") : "Her sabah 30 dakika tempolu yürüyüş yap.";

        // UI güncelle
        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvDesc  = view.findViewById(R.id.tvDetailDescription);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvDesc  != null) tvDesc.setText(desc);

        webView.loadUrl(url);
    }

    // ── WebView Kurulumu ──────────────────────────────────────────────────────

    private void setupWebView() {
        // JavaScript ve DOM Storage etkinleştir
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // Linkleri tarayıcıya yönlendirmeden uygulama içinde aç
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);   // uygulama içinde yükle
                return true;
            }
        });

        // Yükleme ilerlemesini ProgressBar'a yansıt
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar == null) return;
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    // ── Context Menu ─────────────────────────────────────────────────────────

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu,
                                    @NonNull View v,
                                    @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("WebView Seçenekleri");
        menu.add(Menu.NONE, MENU_RELOAD,   0, "🔄 Sayfayı Yenile");
        menu.add(Menu.NONE, MENU_COPY_URL, 1, "📋 URL'yi Kopyala");
        menu.add(Menu.NONE, MENU_OPEN_URL, 2, "🌐 Tarayıcıda Aç");
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == MENU_RELOAD) {
            webView.reload();
            return true;
        } else if (id == MENU_COPY_URL) {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) requireContext()
                            .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip =
                    android.content.ClipData.newPlainText("URL", webView.getUrl());
            if (clipboard != null) clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(requireContext(),
                    "URL kopyalandı!", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == MENU_OPEN_URL) {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(webView.getUrl()));
            startActivity(intent);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // ── Geri Tuşu ile WebView History ────────────────────────────────────────

    /**
     * Activity'den çağrılabilir. WebView geçmişi varsa geri gider, yoksa false döner.
     */
    public boolean onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    private String getDefaultUrl() {
        return "https://musclewiki.com/tr-tr";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
    }
}

