package com.app.game;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Player {

    // Основные переменные игрока
    private float x, y;         // координаты в мире(пока что y всегда одна)
    private final float speed;  // пикс/сек
    private int directionX = 0; // -1 влево, 1 вправо, 0 стоим
    private int lastDirX = 1;   // куда смотрим, если стоим (1 = вправо, -1 влево)

    // Спрайт-листы
    private final Bitmap idleLeft, idleRight, runLeft, runRight;

    // Количество кадров в каждом листе
    private static final int FRAMES = 8;
    private final int frameW, frameH;

    // Анимация
    private static final float FRAME_DURATION_IDLE = 0.14f;  // сек на кадр в idle
    private static final float FRAME_DURATION_RUN = 0.09f;  // сек на кадр в беге
    private int frameIndex = 0;
    private float frameTimer = 0f;

    private final Rect src = new Rect();
    private final Rect dst = new Rect();


    public Player(Resources res, float startX, float startY, float speedPxPerSec, float scale) {
        this.x = startX;
        this.y = startY;
        this.speed = speedPxPerSec;

        idleLeft = scaleSheet(loadAlpha(res, R.drawable.idle_left), scale);
        idleRight = scaleSheet(loadAlpha(res, R.drawable.idle_right), scale);
        runLeft = scaleSheet(loadAlpha(res, R.drawable.run_left), scale);
        runRight = scaleSheet(loadAlpha(res, R.drawable.run_right), scale);

        // считаем размеры кадра по первому (уже масштабированному) листу
        frameW = idleLeft.getWidth() / FRAMES;
        frameH = idleLeft.getHeight();
    }

    public void setDirection(int dirX) {
        this.directionX = dirX;
        if (dirX != 0) lastDirX = dirX;
    }
    public int getLastDirX() {
        return lastDirX;
    }
    public void setLastDirX(int lastDir){
        this.lastDirX = lastDir;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setY(float v) {
        y = v;
    }

    public int getDrawHeight() {
        return frameH;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void update(float dt) {
        // движение
        x += directionX * speed * dt;

        boolean moving = directionX != 0;
        float dur = moving ? FRAME_DURATION_RUN : FRAME_DURATION_IDLE;

        frameTimer += dt;
        while (frameTimer >= dur) {
            frameTimer -= dur;
            frameIndex = (frameIndex + 1) % FRAMES;
        }
    }

    // Рисуем спрайт нашего челика
    public void draw(Canvas canvas, float camX) {
        Bitmap sheet;
        if (directionX < 0) sheet = runLeft;
        else if (directionX > 0) sheet = runRight;
        else sheet = (lastDirX < 0) ? idleLeft : idleRight;

        int sx = frameIndex * frameW;
        src.set(sx, 0, sx + frameW, frameH);

        int dx = Math.round(x - camX - frameW / 2f);
        int dy = Math.round(y - frameH / 2f);
        dst.set(dx, dy, dx + frameW, dy + frameH);

        canvas.drawBitmap(sheet, src, dst, null);
    }

    // Облегчаем наши пнгшки
    private static Bitmap loadAlpha(Resources res, int resId) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(res, resId, o);
    }

    //Скейлим на необходимое число наши спрайт-листы
    private static Bitmap scaleSheet(Bitmap src, float scale) {
        int w = Math.max(1, Math.round(src.getWidth() * scale));
        int h = Math.max(1, Math.round(src.getHeight() * scale));
        return Bitmap.createScaledBitmap(src, w, h, false);
    }

    //Очищаем объекты с спрайтлистами
    public void dispose() {
        if (idleLeft != null && !idleLeft.isRecycled()) idleLeft.recycle();
        if (idleRight != null && !idleRight.isRecycled()) idleRight.recycle();
        if (runLeft != null && !runLeft.isRecycled()) runLeft.recycle();
        if (runRight != null && !runRight.isRecycled()) runRight.recycle();
    }

}
