package ru.fakingfanny.wcag;

import android.content.Context;

public class GridTile extends BaseTile {
    boolean needsSecure() { return false; }
    boolean active() { return GridService.size > 0; }
    String label() { return GridService.size > 0 ? "Сетка " + GridService.size + "dp" : "Сетка 48dp"; }
    void toggle() {
        if (!Toggles.hasOverlay(this)) { openApp(); return; }
        GridService.set(this, GridService.size > 0 ? 0 : 48);
    }
    static void refresh(Context c) { request(c, GridTile.class); }
}
