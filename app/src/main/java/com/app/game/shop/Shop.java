package com.app.game.shop;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Shop {
    // Параметры (можно сделать сеттерами, если понадобится)
    private final static float SHOP_SCALE_SIZE = 1.5f;
    private static final float shopX = -420f;     // позиция магазина в мире (центр по X)

    // Ресурсы
    private Bitmap srcBitmap;     // оригинал
    private Bitmap scaledBitmap;  // под экран
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();

    public Shop() {}

    /** Однократно загрузить исходный арт (на старте/при первом surfaceCreated) */
    public void load(Resources res, int drawableId) {
        if (srcBitmap == null) {
            srcBitmap = BitmapFactory.decodeResource(res, drawableId);
            srcRect.set(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
        }
    }

    /** Пересобрать масштаб под текущий экран/высоту земли (surfaceChanged) */
    public void onSurfaceChanged(int groundDrawHeightPx) {
        if (srcBitmap == null) return;

        int targetH = Math.max(1, groundDrawHeightPx + srcBitmap.getHeight());
        float aspect = (float) srcBitmap.getWidth() / Math.max(1, srcBitmap.getHeight());
        int targetW = Math.max(1, Math.round(targetH * aspect));

        if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
            scaledBitmap.recycle();
            scaledBitmap = null;
        }
        scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, (int) (targetW * SHOP_SCALE_SIZE), (int) (targetH * SHOP_SCALE_SIZE), false);
    }

    /** Отрисовка. groundTopY — линия верха земли (куда “ставим” здание) */
    public void draw(Canvas canvas, float camX, float groundTopY) {
        if (scaledBitmap == null) return;

        int left = Math.round(shopX - camX - scaledBitmap.getWidth() / 2f);
        int top  = Math.round(groundTopY - scaledBitmap.getHeight());
        int right = left + scaledBitmap.getWidth();
        int bottom = top + scaledBitmap.getHeight();
        dstRect.set(left, top, right, bottom);

        canvas.drawBitmap(scaledBitmap, null, dstRect, null);
    }

    /** Освобождение памяти (в surfaceDestroyed) */
    public void dispose() {
        if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
            scaledBitmap.recycle();
            scaledBitmap = null;
        }
        if (srcBitmap != null && !srcBitmap.isRecycled()) {
            srcBitmap.recycle();
            srcBitmap = null;
        }
    }
}
