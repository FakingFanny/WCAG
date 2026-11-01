package ru.fakingfanny.wcag;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/** Общая логика плиток в шторке. */
public abstract class BaseTile extends TileService {
    abstract boolean active();
    abstract String label();
    abstract void toggle() throws Exception;
    boolean needsSecure() { return true; }

    @Override
    public void onStartListening() { update(); }

    @Override
    public void onClick() {
        if (needsSecure() && !Toggles.hasSecure(this)) { openApp(); return; }
        try { toggle(); } catch (Exception e) {
            Toast.makeText(this, "Не получилось: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        update();
    }

    void update() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean ok = !needsSecure() || Toggles.hasSecure(this);
        t.setState(!ok ? Tile.STATE_UNAVAILABLE : active() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel(label());
        t.updateTile();
    }

    @SuppressWarnings("deprecation")
    void openApp() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("wcag://open/pult"))
                .setClass(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(i);
        }
    }

    static void request(Context c, Class<?> cls) {
        try { TileService.requestListeningState(c, new ComponentName(c, cls)); } catch (Exception ignored) {}
    }
}
