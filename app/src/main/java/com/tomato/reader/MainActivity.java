package com.tomato.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String HOME_URL = "http://127.0.0.1:18423/";
    private static final int MENU_STOP = 1;
    private static final int MENU_MODE = 2;
    private static final String PREFS = "tnd";
    private static final String DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    private boolean desktopMode = false;

    private WebView web;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean pageLoaded = false;
    private volatile boolean stopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent svc = new Intent(this, TomatoService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }

        desktopMode = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("desktop", false);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        applyDisplayMode();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (desktopMode) {
                    view.evaluateJavascript(
                            "(function(){var m=document.querySelector('meta[name=viewport]');"
                                    + "if(m){m.setAttribute('content','width=1280');}"
                                    + "document.documentElement.style.width='1280px';})()", null);
                }
                super.onPageFinished(view, url);
            }
        });
        setContentView(web);

        requestStoragePermission();
        waitForServer(0);
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= 32) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
            }
        }
    }

    private void waitForServer(final int attempt) {
        if (pageLoaded || stopped) return;
        if (attempt > 80) {
            Toast.makeText(this, "服务还没起来，稍等一下或重开一次", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            boolean up = false;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(HOME_URL).openConnection();
                c.setConnectTimeout(800);
                c.setReadTimeout(800);
                up = c.getResponseCode() == 200;
                c.disconnect();
            } catch (Exception ignored) {
            }
            final boolean ok = up;
            handler.post(() -> {
                if (stopped) return;
                if (ok && !pageLoaded) {
                    pageLoaded = true;
                    web.loadUrl(HOME_URL);
                } else if (!ok) {
                    handler.postDelayed(() -> waitForServer(attempt + 1), 600);
                }
            });
        }).start();
    }

    private void applyDisplayMode() {
        WebSettings s = web.getSettings();
        if (desktopMode) {
            s.setUserAgentString(DESKTOP_UA);
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
        } else {
            s.setUserAgentString(null);
            s.setUseWideViewPort(false);
            s.setLoadWithOverviewMode(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_MODE, 0, desktopMode ? "切换为手机版" : "切换为桌面版");
        menu.add(0, MENU_STOP, 1, "停止服务");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_MODE) {
            desktopMode = !desktopMode;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("desktop", desktopMode).apply();
            applyDisplayMode();
            invalidateOptionsMenu();
            web.reload();
            return true;
        }
        if (item.getItemId() == MENU_STOP) {
            stopped = true;
            stopService(new Intent(this, TomatoService.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
