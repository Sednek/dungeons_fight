package com.app.game;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Player {

    private float x, y;
    private final float speed;          // пикс/сек
    private int directionX = 0;               // -1 влево, 1 вправо, 0 стоим
    private int lastDirX = 1;           // куда смотрим, если стоим (1 = вправо)

    // спрайт-листы
    private final Bitmap idleLeft, idleRight, runLeft, runRight;

    // количество кадров в каждом листе
    private static final int FRAMES = 8;
    private final int frameW, frameH;

    // анимация
    private int frameIndex = 0;
    private float frameTimer = 0f;
    private float frameDurationIdle = 0.14f;  // сек на кадр в idle
    private float frameDurationRun  = 0.09f;  // сек на кадр в беге

    private final Rect src = new Rect();
    private final Rect dst = new Rect();

    // масштаб спрайта при выводе
    private final float scale;



    public Player(Resources res, float startX, float startY, float speedPxPerSec, float scale) {
        this.x = startX;
        this.y = startY;
        this.speed = speedPxPerSec;
        this.scale = scale;

        idleLeft  = BitmapFactory.decodeResource(res, R.drawable.idle_left);
        idleRight = BitmapFactory.decodeResource(res, R.drawable.idle_right);
        runLeft   = BitmapFactory.decodeResource(res, R.drawable.run_left);
        runRight  = BitmapFactory.decodeResource(res, R.drawable.run_right);

        // считаем размеры кадра по первому листу
        frameW = idleLeft.getWidth() / FRAMES;
        frameH = idleLeft.getHeight();
    }

    public void setDirection(int dirX) {
        this.directionX = dirX;
        if (dirX != 0) lastDirX = dirX;
    }

    public float getX() { return x; }
    public void setX(float v) { x = v; }
    public void setY(float v) { y = v; }
    public int getDrawWidth()  { return Math.round(frameW * scale); }
    public int getDrawHeight() { return Math.round(frameH * scale); }

    public void update(float dt) {
        // движение
        x += directionX * speed * dt;

        boolean moving = directionX != 0;
        float dur = moving ? frameDurationRun : frameDurationIdle;

        frameTimer += dt;
        while (frameTimer >= dur) {
            frameTimer -= dur;
            frameIndex = (frameIndex + 1) % FRAMES;
        }
    }

    public void draw(Canvas canvas, float camX) {
        android.graphics.Bitmap sheet;
        if      (directionX < 0) sheet = runLeft;
        else if (directionX > 0) sheet = runRight;
        else                     sheet = (lastDirX < 0) ? idleLeft : idleRight;

        int sx = frameIndex * frameW;
        src.set(sx, 0, sx + frameW, frameH);

        int dw = Math.round(frameW * scale);
        int dh = Math.round(frameH * scale);
        int dx = Math.round(x - camX - dw / 2f);
        int dy = Math.round(y - dh / 2f);
        dst.set(dx, dy, dx + dw, dy + dh);

        canvas.drawBitmap(sheet, src, dst, null);
    }
}
