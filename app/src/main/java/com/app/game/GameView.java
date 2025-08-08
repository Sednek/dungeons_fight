package com.app.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameLoop gameLoop;

    private Player player;


    private Bitmap background;
    private Rect bgSrc, bgDst;


    // HUD-краска и кэш строк
    private final android.graphics.Paint hudPaint = new android.graphics.Paint();
    private String fpsText = "FPS: 0";
    private String upsText = "UPS: 0";

    private float circleX = 0f;
    private float speedPxPerFrameAt60 = 3.0f;

    private float fps = 0;

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
    public void surfaceCreated(SurfaceHolder holder) {
        // scale = во сколько раз увеличить кадр спрайта на экране
        float scale = 10f;
        player = new Player(getResources(), getWidth() / 2f, getHeight() / 2f, 500f, scale);

        gameLoop = new GameLoop(getHolder(), this);
        gameLoop.setRunning(true);
        gameLoop.start();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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

        if (background != null) {
            background.recycle();
            background = null;
            bgSrc = null;
            bgDst = null;
        }
    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (background == null) {
            background = BitmapFactory.decodeResource(getResources(), R.drawable.background_forest);
            bgSrc = new Rect(0, 0, background.getWidth(), background.getHeight());
        }

        float scale = Math.max(width / (float) background.getWidth(), height / (float) background.getHeight());
        int w = Math.round(background.getWidth() * scale);
        int h = Math.round(background.getHeight() * scale);
        int left = (width - w) / 2;
        int top  = (height - h) / 2;
        bgDst = new Rect(left, top, left + w, top + h);
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
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        if (background != null && bgSrc != null && bgDst != null) {
            canvas.drawBitmap(background, bgSrc, bgDst, null);
        } else {
            canvas.drawColor(Color.GRAY);
        }

        if (gameLoop != null) {
            canvas.drawText(fpsText + "   " + upsText, 32, 64, hudPaint);
        }

        if (player != null) {
            player.draw(canvas);
        }
    }

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

}