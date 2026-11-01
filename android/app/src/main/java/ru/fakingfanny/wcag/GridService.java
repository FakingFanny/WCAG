package ru.fakingfanny.wcag;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

/** Полупрозрачная сетка поверх всех приложений: видно, какие кнопки меньше 48dp / 24dp. */
public class GridService extends Service {
    static volatile int size = 0;
    static final String EXTRA_SIZE = "size";
    private static final String CH = "grid";
    private View view;

    static void set(Context c, int dp) {
        Intent i = new Intent(c, GridService.class).putExtra(EXTRA_SIZE, dp);
        if (dp <= 0) { c.stopService(i); size = 0; return; }
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int dp = intent == null ? 48 : intent.getIntExtra(EXTRA_SIZE, 48);
        if (dp <= 0) { stopSelf(); return START_NOT_STICKY; }
        startFg(dp);
        if (!Toggles.hasOverlay(this)) { stopSelf(); return START_NOT_STICKY; }
        show(dp);
        size = dp;
        GridTile.refresh(this);
        return START_NOT_STICKY;
    }

    private void startFg(int dp) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CH) == null) {
            NotificationChannel ch = new NotificationChannel(CH, "Сетка поверх экрана", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, GridService.class).putExtra(EXTRA_SIZE, 0),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_tile_grid)
                .setContentTitle("Сетка " + dp + " dp включена")
                .setContentText("Нажми «Скрыть», чтобы убрать")
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Скрыть", stop).build());
        if (Build.VERSION.SDK_INT >= 34) startForeground(7, b.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(7, b.build());
    }

    private void show(int dp) {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (view != null) { try { wm.removeView(view); } catch (Exception ignored) {} }
        final float step = dp * getResources().getDisplayMetrics().density;
        view = new View(this) {
            final Paint line = new Paint();
            final Paint bold = new Paint();
            {
                line.setColor(Color.argb(150, 255, 0, 140));
                line.setStrokeWidth(1f);
                bold.setColor(Color.argb(220, 0, 160, 255));
                bold.setStrokeWidth(2f);
            }
            @Override
            protected void onDraw(Canvas c) {
                int w = getWidth(), h = getHeight(), n = 0;
                for (float x = 0; x <= w; x += step, n++) c.drawLine(x, 0, x, h, n % 4 == 0 ? bold : line);
                n = 0;
                for (float y = 0; y <= h; y += step, n++) c.drawLine(0, y, w, y, n % 4 == 0 ? bold : line);
            }
        };
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        // Android 12+: окно поверх с непрозрачностью выше 0.8 блокирует касания
        p.alpha = 0.75f;
        if (Build.VERSION.SDK_INT >= 28) p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        wm.addView(view, p);
    }

    @Override
    public void onDestroy() {
        if (view != null) {
            try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(view); } catch (Exception ignored) {}
            view = null;
        }
        size = 0;
        GridTile.refresh(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
