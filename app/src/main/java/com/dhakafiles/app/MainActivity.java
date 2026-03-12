package com.dhakafiles.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://dhakafiles.com/";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ProgressBar progressBar;
    private ProgressBar pageProgressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout noInternetLayout;
    private FrameLayout webViewContainer;
    private FrameLayout fullscreenContainer;
    private BottomNavigationView bottomNav;

    private ValueCallback<Uri[]> fileUploadCallback;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;
    private boolean isFullscreen = false;
    private boolean doubleBackToExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(Color.parseColor("#1a1a2e"));
        getWindow().setNavigationBarColor(Color.parseColor("#1a1a2e"));

        initViews();
        setupWebView();
        setupSwipeRefresh();
        setupBottomNavigation();

        if (isNetworkAvailable()) {
            webView.loadUrl(HOME_URL);
        } else {
            showNoInternet();
        }
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        pageProgressBar = findViewById(R.id.pageProgressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        noInternetLayout = findViewById(R.id.noInternetLayout);
        webViewContainer = findViewById(R.id.webViewContainer);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);
        bottomNav = findViewById(R.id.bottomNavigation);

        Button retryBtn = findViewById(R.id.retryButton);
        retryBtn.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                hideNoInternet();
                webView.loadUrl(HOME_URL);
            } else {
                Toast.makeText(this, "ইন্টারনেট সংযোগ নেই!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " DhakaFilesApp/1.0");

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                pageProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                pageProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("tel:") || url.startsWith("mailto:")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                    return true;
                }
                if (url.startsWith("whatsapp:") || url.contains("wa.me")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (ActivityNotFoundException e) { Toast.makeText(MainActivity.this, "WhatsApp নেই", Toast.LENGTH_SHORT).show(); }
                    return true;
                }
                if (url.contains("facebook.com") || url.contains("youtube.com") ||
                        url.contains("twitter.com") || url.contains("instagram.com") ||
                        url.contains("play.google.com") || url.contains("t.me")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (Exception e) { view.loadUrl(url); }
                    return true;
                }
                if (url.contains("dhakafiles.com")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (Exception e) { return false; }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showNoInternet();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                pageProgressBar.setProgress(p);
                if (p == 100) pageProgressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileUploadCallback != null) fileUploadCallback.onReceiveValue(null);
                fileUploadCallback = cb;
                try { startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST); }
                catch (Exception e) { fileUploadCallback = null; return false; }
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) { callback.onCustomViewHidden(); return; }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(view);
                fullscreenContainer.setVisibility(View.VISIBLE);
                webViewContainer.setVisibility(View.GONE);
                bottomNav.setVisibility(View.GONE);
                isFullscreen = true;
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);
                webViewContainer.setVisibility(View.VISIBLE);
                bottomNav.setVisibility(View.VISIBLE);
                customView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                isFullscreen = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public boolean onJsAlert(WebView v, String u, String msg, JsResult r) {
                new AlertDialog.Builder(MainActivity.this).setTitle("DhakaFiles").setMessage(msg)
                        .setPositiveButton("ঠিক আছে", (d, w) -> r.confirm()).setCancelable(false).show();
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) { request.grant(request.getResources()); }

            @Override
            public boolean onCreateWindow(WebView v, boolean d, boolean u, Message r) {
                String url = v.getHitTestResult().getExtra();
                if (url != null) v.loadUrl(url);
                return false;
            }
        });

        webView.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                String name = URLUtil.guessFileName(url, cd, mime);
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                req.setMimeType(mime);
                req.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
                req.setTitle(name);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
                Snackbar.make(webView, "ডাউনলোড: " + name, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(Color.parseColor("#16213e")).setTextColor(Color.WHITE).show();
            } catch (Exception e) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#e94560"), Color.parseColor("#0f3460"));
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#1a1a2e"));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) webView.reload();
            else { swipeRefreshLayout.setRefreshing(false); showNoInternet(); }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, sx, sy, osx, osy) -> swipeRefreshLayout.setEnabled(sy == 0));
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { webView.loadUrl(HOME_URL); return true; }
            if (id == R.id.nav_back) { if (webView.canGoBack()) webView.goBack(); return true; }
            if (id == R.id.nav_forward) { if (webView.canGoForward()) webView.goForward(); return true; }
            if (id == R.id.nav_refresh) { webView.reload(); return true; }
            if (id == R.id.nav_share) { shareCurrentPage(); return true; }
            return false;
        });
    }

    private void shareCurrentPage() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, webView.getTitle() + "\n" + webView.getUrl());
        startActivity(Intent.createChooser(i, "শেয়ার করুন"));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager c = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (c == null) return false;
        NetworkCapabilities n = c.getNetworkCapabilities(c.getActiveNetwork());
        return n != null && (n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || n.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private void showNoInternet() { noInternetLayout.setVisibility(View.VISIBLE); webViewContainer.setVisibility(View.GONE); }
    private void hideNoInternet() { noInternetLayout.setVisibility(View.GONE); webViewContainer.setVisibility(View.VISIBLE); }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == FILE_CHOOSER_REQUEST && fileUploadCallback != null) {
            Uri[] results = null;
            if (res == RESULT_OK && data != null && data.getDataString() != null)
                results = new Uri[]{Uri.parse(data.getDataString())};
            fileUploadCallback.onReceiveValue(results);
            fileUploadCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isFullscreen && customViewCallback != null) { customViewCallback.onCustomViewHidden(); return true; }
            if (webView.canGoBack()) { webView.goBack(); return true; }
            if (doubleBackToExit) { finish(); return true; }
            doubleBackToExit = true;
            Snackbar.make(webView, "বের হতে আবার চাপুন", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(Color.parseColor("#16213e")).setTextColor(Color.WHITE).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExit = false, 2000);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() { super.onPause(); webView.onPause(); CookieManager.getInstance().flush(); }
    @Override
    protected void onResume() { super.onResume(); webView.onResume(); }
    @Override
    protected void onDestroy() { if (webView != null) { webView.loadUrl("about:blank"); webView.destroy(); } super.onDestroy(); }
}
