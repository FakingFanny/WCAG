package ru.fakingfanny.wcag;

public class AnimTile extends BaseTile {
    boolean active() { return Toggles.animOff(this); }
    String label() { return Toggles.animOff(this) ? "Анимации выкл" : "Анимации"; }
    void toggle() { Toggles.setAnimOff(this, !Toggles.animOff(this)); }
}
