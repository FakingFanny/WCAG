package ru.fakingfanny.wcag;

public class FontTile extends BaseTile {
    boolean active() { return Toggles.font(this) > 1.01f; }
    String label() { return "Шрифт " + Math.round(Toggles.font(this) * 100) + "%"; }
    void toggle() { Toggles.setFont(this, Toggles.nextFont(Toggles.font(this))); }
}
