package ru.fakingfanny.wcag;

public class TalkbackTile extends BaseTile {
    boolean active() { return Toggles.isTalkback(this); }
    String label() { return "TalkBack"; }
    void toggle() { Toggles.setTalkback(this, !Toggles.isTalkback(this)); }
}
