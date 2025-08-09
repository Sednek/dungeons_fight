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


    // HUD-краска и кэш строк
    private final android.graphics.Paint hudPaint = new android.graphics.Paint();
    private String fpsText = "";
    private String upsText = "";

    private float circleX = 0f;
    private float speedPxPerFrameAt60 = 3.0f;

    // Камера
    private float camX = 0f;
    private static final float CAM_LERP = 0.12f;   // плавность следования камеры

    // Background
    private Bitmap bgTile;     // seamless_bg.png
    private Bitmap bgScaled;
    private int bgW, bgH, bgScaledW, bgScaledH;
    private static final float BG_PARALLAX = 0.3f; // фон движется медленнее камеры

    //Ground tile
    private Bitmap groundTile;
    private Bitmap groundTileScaled;
    private int groundTileWidth, groundTileHeight;

    private int groundDrawHeightPx;
    private float groundY;
    private static final float GROUND_PARALLAX = 1.0f;

    private static final int GROUND_SCALE = 2;

    private static final int GROUND_OFFSET_Y = 20; // на сколько пикселей опущен тайл

    // т.к тайл персонажа 80px, а мы за основу берем серидину персонажа(40px)
    // то надо смещать персонажа, чтобы ноги касались пола
    private static final float PLAYER_OFFSET_FOR_GROUND = 0.17f;

    private final Rect groundSrc = new Rect();
    private final Rect groundDst = new Rect();

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);

        hudPaint.setColor(android.graphics.Color.GREEN);
        hudPaint.setTextSize(48f);
        hudPaint.setAntiAlias(false);

        // gameLoop will be created in surfaceCreated()

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
                } catch (InterruptedException ignored) {
                }
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
        // Pre-scale background to screen height once
        if (bgTile != null) {
            int screenH = getHeight();
            int srcW = bgTile.getWidth();
            int srcH = bgTile.getHeight();
            bgW = srcW;
            bgH = srcH;
            float kbg = screenH / (float) srcH;
            bgScaledW = Math.max(1, Math.round(srcW * kbg));
            bgScaledH = screenH;
            bgScaled = Bitmap.createScaledBitmap(bgTile, bgScaledW, bgScaledH, false);
        }

        if (groundTile == null) {
            groundTile = BitmapFactory.decodeResource(getResources(), R.drawable.ground_tile_dark);
            groundTileWidth = groundTile.getWidth();
            groundTileHeight = groundTile.getHeight();
            groundSrc.set(0, 0, groundTileWidth, groundTileHeight);
        }

        // Увеличиваем высоту в GROUND_SCALE раз относительно оригинала
        groundDrawHeightPx = groundTileHeight * GROUND_SCALE;


        // Pre-scale ground tile to desired on-screen height once
        if (groundTile != null) {
            float kg = groundDrawHeightPx / (float) groundTileHeight;
            int gW = Math.max(1, Math.round(groundTileWidth * kg));
            int gH = Math.max(1, Math.round(groundTileHeight * kg));
            groundTileScaled = Bitmap.createScaledBitmap(groundTile, gW, gH, false);
        }
// Линия пола = нижняя граница экрана
        groundY = getHeight();
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

            float target = player.getX() - getWidth() * 0.5f;
            camX += (target - camX) * CAM_LERP;

            float playerHalfHeight = player.getDrawHeight() * 0.5f;
            player.setY(groundY + GROUND_OFFSET_Y - groundDrawHeightPx - playerHalfHeight * PLAYER_OFFSET_FOR_GROUND);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        drawLoopedBackground(canvas);
        drawGround(canvas);

        if (gameLoop != null) {
            canvas.drawText(fpsText + " " + upsText, 32, 64, hudPaint);
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
        if (bgScaled == null) {
            canvas.drawColor(Color.BLACK);
            return;
        }
        int screenWidth = getWidth();

        int scroll = (int) (camX * BG_PARALLAX);
        int startX = -(scroll % bgScaledW);
        if (startX > 0) startX -= bgScaledW;

        for (int x = startX; x < screenWidth + bgScaledW; x += bgScaledW) {
            canvas.drawBitmap(bgScaled, x, 0, null);
        }
    }

    private void drawGround(Canvas canvas) {
        if (groundTileScaled == null) return;

        int screenWidth = getWidth();
        int destTop = getHeight() - groundDrawHeightPx + GROUND_OFFSET_Y;

        int tileW = groundTileScaled.getWidth();

        int scroll = (int) (camX * GROUND_PARALLAX);
        int startX = -(scroll % tileW);
        if (startX > 0) startX -= tileW;

        for (int x = startX; x < screenWidth + tileW; x += tileW) {
            canvas.drawBitmap(groundTileScaled, x, destTop, null);
        }
    }


}