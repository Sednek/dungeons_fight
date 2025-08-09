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

        idleLeft  = scaleSheet(loadAlpha(res, R.drawable.idle_left),  scale);
        idleRight = scaleSheet(loadAlpha(res, R.drawable.idle_right), scale);
        runLeft   = scaleSheet(loadAlpha(res, R.drawable.run_left),   scale);
        runRight  = scaleSheet(loadAlpha(res, R.drawable.run_right),  scale);

        // считаем размеры кадра по первому (уже масштабированному) листу
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
    public int getDrawWidth()  { return frameW; }
    public int getDrawHeight() { return frameH; }

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

        int dx = Math.round(x - camX - frameW / 2f);
        int dy = Math.round(y - frameH / 2f);
        dst.set(dx, dy, dx + frameW, dy + frameH);

        canvas.drawBitmap(sheet, src, dst, null);
    }

    private static Bitmap loadAlpha(Resources res, int resId) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(res, resId, o);
    }

    private static Bitmap scaleSheet(Bitmap src, float scale) {
        int w = Math.max(1, Math.round(src.getWidth() * scale));
        int h = Math.max(1, Math.round(src.getHeight() * scale));
        return Bitmap.createScaledBitmap(src, w, h, false);
    }
}
