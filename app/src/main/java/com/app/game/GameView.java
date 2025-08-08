package com.app.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameLoop gameLoop;

    private Player player;


    private Bitmap background;


    // HUD-краска и кэш строк
    private final android.graphics.Paint hudPaint = new android.graphics.Paint();
    private String fpsText = "FPS: 0";
    private String upsText = "UPS: 0";

    private float circleX = 0f;
    private float speedPxPerFrameAt60 = 3.0f;

    // Камера
    private float camX = 0f;
    private static final float CAM_LERP = 0.12f;   // плавность следования камеры

    // Background
    private Bitmap bgTile;     // seamless_bg.png
    private int bgW, bgH;
    private static final float BG_PARALLAX = 0.6f; // фон движется медленнее камеры

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);

        hudPaint.setColor(android.graphics.Color.GREEN);
        hudPaint.setTextSize(48f);
        hudPaint.setAntiAlias(false);

        gameLoop = new GameLoop(getHolder(), this);

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // scale = во сколько раз увеличить кадр спрайта на экране
        float scale = 5f;
        player = new Player(getResources(), getWidth() / 2f, getHeight() / 2f, 500f, scale);

        gameLoop = new GameLoop(getHolder(), this);
        gameLoop.setRunning(true);
        gameLoop.start();
    }


    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (gameLoop != null) {
            gameLoop.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    gameLoop.join();
                    retry = false;
                } catch (InterruptedException ignored) {}
            }
        }
    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (bgTile == null) {
            bgTile = BitmapFactory.decodeResource(getResources(), R.drawable.seamless_bg);
            bgW = bgTile.getWidth();
            bgH = bgTile.getHeight();
        }
    }

    // Логика
    public void update(float dtSeconds) {
        // Нормализуем скорость под фиксированный апдейт:
        // если скорость «в пикселях на кадр при 60», то умножаем на 60*dt
        circleX += speedPxPerFrameAt60 * (60f * dtSeconds);
        if (circleX > getWidth()) circleX = 0f;


        // при желании тут кэшируй строки HUD раз в N кадров, чтобы не формировать каждый кадр
        if (gameLoop != null) {
            fpsText = "FPS: " + gameLoop.getFps();
            upsText = "UPS: " + gameLoop.getUps();
        }

        if (player != null) {
            player.update(dtSeconds);
        }

        if (player != null) {
            float target = player.getX() - getWidth() * 0.5f; // держим игрока по центру экрана
            camX += (target - camX) * CAM_LERP;               // плавное следование
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        drawLoopedBackground(canvas);

        if (gameLoop != null) {
            canvas.drawText(fpsText + "   " + upsText, 32, 64, hudPaint);
        }

        if (player != null) player.draw(canvas, camX);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (event.getX() < getWidth() / 2f) {
                    // левая половина экрана
                    player.setDirection(-1);
                } else {
                    // правая половина экрана
                    player.setDirection(1);
                }
                break;

            case MotionEvent.ACTION_UP:
                player.setDirection(0); // отпустили палец — стоим
                break;
        }
        return true;
    }

    private void drawLoopedBackground(Canvas canvas) {
        if (bgTile == null) {
            canvas.drawColor(Color.BLACK);
            return;
        }

        int screenWidth = getWidth();
        int screenHeight = getHeight();

        // Масштаб по высоте
        float scale = (float) screenHeight / bgTile.getHeight();
        int scaledWidth = Math.round(bgTile.getWidth() * scale);
        int scaledHeight = screenHeight; // ровно по высоте экрана

        // Параллакс
        float scroll = camX * BG_PARALLAX;
        int startX = (int) (-(scroll % scaledWidth));
        if (startX > 0) startX -= scaledWidth;

        // Повторяем фон по горизонтали
        for (int x = startX; x < screenWidth; x += scaledWidth) {
            Rect srcRect = new Rect(0, 0, bgTile.getWidth(), bgTile.getHeight());
            Rect dstRect = new Rect(x, 0, x + scaledWidth, scaledHeight);
            canvas.drawBitmap(bgTile, srcRect, dstRect, null);
        }
    }


}