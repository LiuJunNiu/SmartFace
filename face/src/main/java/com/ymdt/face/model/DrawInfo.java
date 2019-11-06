package com.ymdt.face.model;

import android.graphics.Rect;

public class DrawInfo {
    private Rect rect;
    private int liveness;
    private int color;

    public DrawInfo(Rect rect, int liveness, int color) {
        this.rect = rect;
        this.liveness = liveness;
        this.color = color;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public int getLiveness() {
        return liveness;
    }

    public void setLiveness(int liveness) {
        this.liveness = liveness;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
