package ru.fakingfanny.wcag;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import java.util.ArrayList;
import java.util.List;

/** Переключатели системных настроек для тестирования доступности. */
final class Toggles {
    private Toggles() {}

    static final String GRANT_CMD = "adb shell pm grant ru.fakingfanny.wcag android.permission.WRITE_SECURE_SETTINGS";

    static boolean hasSecure(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasOverlay(Context c) {
        return Settings.canDrawOverlays(c);
    }

    // ---------- TalkBack ----------
    static ComponentName talkback(Context c) {
        AccessibilityManager am = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return null;
        for (AccessibilityServiceInfo i : am.getInstalledAccessibilityServiceList()) {
            if (i.getResolveInfo() == null) continue;
            ServiceInfo si = i.getResolveInfo().serviceInfo;
            String full = (si.packageName + "/" + si.name).toLowerCase();
            if (full.contains("talkback")) return new ComponentName(si.packageName, si.name);
        }
        return null;
    }

    private static List<String> enabledServices(ContentResolver cr) {
        List<String> out = new ArrayList<>();
        String cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (cur == null) return out;
        for (String s : cur.split(":")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    static boolean isTalkback(Context c) {
        ComponentName tb = talkback(c);
        if (tb == null) return false;
        for (String s : enabledServices(c.getContentResolver())) {
            ComponentName cn = ComponentName.unflattenFromString(s);
            if (tb.equals(cn)) return true;
        }
        return false;
    }

    static void setTalkback(Context c, boolean on) {
        ComponentName tb = talkback(c);
        if (tb == null) throw new IllegalStateException("TalkBack не установлен");
        ContentResolver cr = c.getContentResolver();
        List<String> list = new ArrayList<>();
        for (String s : enabledServices(cr)) {
            if (!tb.equals(ComponentName.unflattenFromString(s))) list.add(s);
        }
        if (on) list.add(tb.flattenToString());
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, String.join(":", list));
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, list.isEmpty() ? 0 : 1);
    }

    // ---------- Шрифт ----------
    static float font(Context c) {
        return Settings.System.getFloat(c.getContentResolver(), Settings.System.FONT_SCALE, 1f);
    }

    static void setFont(Context c, float v) {
        Settings.System.putFloat(c.getContentResolver(), Settings.System.FONT_SCALE, v);
    }

    static float nextFont(float cur) {
        if (cur < 1.25f) return 1.3f;
        if (cur < 1.9f) return 2.0f;
        return 1.0f;
    }

    // ---------- Анимации ----------
    private static final String[] ANIM = {"window_animation_scale", "transition_animation_scale", "animator_duration_scale"};

    static boolean animOff(Context c) {
        ContentResolver cr = c.getContentResolver();
        for (String k : ANIM) if (Settings.Global.getFloat(cr, k, 1f) != 0f) return false;
        return true;
    }

    static void setAnimOff(Context c, boolean off) {
        ContentResolver cr = c.getContentResolver();
        for (String k : ANIM) Settings.Global.putFloat(cr, k, off ? 0f : 1f);
    }

    // ---------- Оттенки серого ----------
    static boolean gray(Context c) {
        ContentResolver cr = c.getContentResolver();
        return Settings.Secure.getInt(cr, "accessibility_display_daltonizer_enabled", 0) == 1
                && Settings.Secure.getInt(cr, "accessibility_display_daltonizer", -1) == 0;
    }

    static void setGray(Context c, boolean on) {
        ContentResolver cr = c.getContentResolver();
        if (on) Settings.Secure.putInt(cr, "accessibility_display_daltonizer", 0);
        Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", on ? 1 : 0);
    }

    static String state(Context c) {
        boolean tbFound = talkback(c) != null;
        return "{\"secure\":" + hasSecure(c)
                + ",\"overlay\":" + hasOverlay(c)
                + ",\"tbFound\":" + tbFound
                + ",\"talkback\":" + (tbFound && isTalkback(c))
                + ",\"font\":" + font(c)
                + ",\"animOff\":" + animOff(c)
                + ",\"gray\":" + gray(c)
                + ",\"grid\":" + GridService.size
                + ",\"sdk\":" + Build.VERSION.SDK_INT + "}";
    }
}
