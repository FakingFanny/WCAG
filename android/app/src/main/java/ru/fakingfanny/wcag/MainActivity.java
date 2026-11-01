package ru.fakingfanny.wcag;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String BASE = "file:///android_asset/index.html";
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // Учитываем системный размер шрифта
        s.setTextZoom(Math.round(100 * getResources().getConfiguration().fontScale));
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                // Внешние ссылки открываем в браузере
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        setContentView(web);
        if (state != null) web.restoreState(state);
        else web.loadUrl(BASE + routeOf(getIntent()));
    }

    // Ярлыки приходят как wcag://open/<раздел>
    private String routeOf(Intent i) {
        Uri d = i == null ? null : i.getData();
        return d == null || d.getLastPathSegment() == null ? "" : "#" + d.getLastPathSegment();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String r = routeOf(intent);
        if (!r.isEmpty()) web.evaluateJavascript("window.route&&route('" + r.replace("'", "") + "')", null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("window.goBack?goBack():false", v -> {
            if (!"true".equals(v)) finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    private class Bridge {
        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("WCAG", text));
        }

        @JavascriptInterface
        public void share(String text) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(i, "Отправить баг"));
        }

        @JavascriptInterface
        public String device() {
            String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
            String model = Build.MODEL == null ? "" : Build.MODEL;
            String name = model.toLowerCase().startsWith(maker.toLowerCase()) ? model
                    : (maker.isEmpty() ? model : Character.toUpperCase(maker.charAt(0)) + maker.substring(1) + " " + model);
            StringBuilder sb = new StringBuilder(name).append(", Android ").append(Build.VERSION.RELEASE);
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null && am.isTouchExplorationEnabled()) sb.append(" · TalkBack вкл");
            float fs = getResources().getConfiguration().fontScale;
            sb.append(" · шрифт ").append(Math.round(fs * 100)).append("%");
            return sb.toString();
        }
    }
}
