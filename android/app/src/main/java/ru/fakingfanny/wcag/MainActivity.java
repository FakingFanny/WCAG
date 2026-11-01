package ru.fakingfanny.wcag;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String BASE = "file:///android_asset/index.html";
    private static final int PICK = 42;
    private WebView web;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        applyFontScale();
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(Intent.createChooser(i, "Выбери скриншот"), PICK); }
                catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });
        setContentView(web);
        web.loadUrl(BASE + routeOf(getIntent()));
    }

    private void applyFontScale() {
        // Учитываем системный размер шрифта
        web.getSettings().setTextZoom(Math.round(100 * getResources().getConfiguration().fontScale));
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applyFontScale();
        notifyJs();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK && fileCb != null) {
            Uri u = (res == RESULT_OK && data != null) ? data.getData() : null;
            fileCb.onReceiveValue(u == null ? null : new Uri[]{u});
            fileCb = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifyJs();
    }

    private void notifyJs() {
        if (web != null) web.evaluateJavascript("window.onNative&&onNative()", null);
    }

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

    private void refreshTiles() {
        BaseTile.request(this, TalkbackTile.class);
        BaseTile.request(this, FontTile.class);
        BaseTile.request(this, AnimTile.class);
        BaseTile.request(this, GridTile.class);
    }

    private class Bridge {
        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("WCAG", text));
        }

        @JavascriptInterface
        public void share(String text) {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(i, "Отправить"));
        }

        @JavascriptInterface
        public String device() {
            String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
            String model = Build.MODEL == null ? "" : Build.MODEL;
            String name = model.toLowerCase().startsWith(maker.toLowerCase()) || maker.isEmpty() ? model
                    : Character.toUpperCase(maker.charAt(0)) + maker.substring(1) + " " + model;
            StringBuilder sb = new StringBuilder(name).append(", Android ").append(Build.VERSION.RELEASE);
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null && am.isTouchExplorationEnabled()) sb.append(" · TalkBack вкл");
            sb.append(" · шрифт ").append(Math.round(getResources().getConfiguration().fontScale * 100)).append("%");
            return sb.toString();
        }

        // ---------- пульт ----------
        @JavascriptInterface
        public String state() {
            try { return Toggles.state(MainActivity.this); } catch (Exception e) { return "{}"; }
        }

        @JavascriptInterface
        public String grantCmd() { return Toggles.GRANT_CMD; }

        /** what: talkback | gray | anim | font | grid. Возвращает "ok" или текст ошибки. */
        @JavascriptInterface
        public String set(String what, String value) {
            try {
                Context c = MainActivity.this;
                switch (what) {
                    case "talkback": Toggles.setTalkback(c, "1".equals(value)); break;
                    case "gray": Toggles.setGray(c, "1".equals(value)); break;
                    case "anim": Toggles.setAnimOff(c, "1".equals(value)); break;
                    case "font": Toggles.setFont(c, Float.parseFloat(value)); break;
                    case "grid":
                        int dp = Integer.parseInt(value);
                        if (dp > 0 && !Toggles.hasOverlay(c)) return "overlay";
                        if (dp > 0 && Build.VERSION.SDK_INT >= 33
                                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5));
                        }
                        GridService.set(c, dp);
                        break;
                    default: return "unknown";
                }
                runOnUiThread(MainActivity.this::refreshTiles);
                return "ok";
            } catch (SecurityException e) {
                return "Нет разрешения";
            } catch (Exception e) {
                return e.getMessage() == null ? e.toString() : e.getMessage();
            }
        }

        /** Открыть системный экран: a11y | display | overlay | dev */
        @JavascriptInterface
        public void open(String where) {
            Intent i;
            switch (where) {
                case "overlay": i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())); break;
                case "display": i = new Intent(Settings.ACTION_DISPLAY_SETTINGS); break;
                case "dev": i = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS); break;
                default: i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            }
            try { startActivity(i); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        }
    }
}
