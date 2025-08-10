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
    private GameSnapshot pendingSnapshot;


    // HUD-краска и кэш строк
    private final android.graphics.Paint hudPaint = new android.graphics.Paint();
    private String fpsText = "";
    private String upsText = "";

    // Камера
    private float camX = 0f;
    private static final float CAM_LERP = 0.12f;   // плавность следования камеры

    // Background
    private Bitmap bgTile;     // seamless_bg.png
    private Bitmap bgScaled;
    private int bgScaledW;
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

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);

        hudPaint.setColor(android.graphics.Color.GREEN);
        hudPaint.setTextSize(48f);
        hudPaint.setAntiAlias(false);

        setFocusable(true);
    }

    public void setPendingSnapshot(GameSnapshot snapshot) {
        this.pendingSnapshot = snapshot;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // scale = во сколько раз увеличить кадр спрайта на экране
        float scale = 5f;
        player = new Player(getResources(), getWidth() / 2f, getHeight() / 2f, 500f, scale);

        float refreshRate = (getDisplay() != null) ? getDisplay().getRefreshRate() : 60f;
        gameLoop = new GameLoop(getHolder(), this, refreshRate);
        gameLoop.setRunning(true);
        gameLoop.start();

        if (pendingSnapshot != null) {
            restoreFrom(pendingSnapshot);
            pendingSnapshot = null;
        }

        System.out.println("Surface created");
    }


    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (gameLoop != null) {
            gameLoop.requestStopAndJoin();
            gameLoop = null;
        }
        // освобождение больших битмапов
        if (bgScaled != null) {
            bgScaled.recycle();
            bgScaled = null;
        }
        if (bgTile != null) {
            bgTile.recycle();
            bgTile = null;
        }
        if (groundTileScaled != null) {
            groundTileScaled.recycle();
            groundTileScaled = null;
        }
        if (groundTile != null) {
            groundTile.recycle();
            groundTile = null;
        }
        if (player != null) {
            player.dispose();
        }
        System.out.println("Surface destroyed");
    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (bgTile == null) {
            bgTile = BitmapFactory.decodeResource(getResources(), R.drawable.seamless_bg);
        }
        // Pre-scale background to screen height once
        if (bgTile != null) {
            int screenH = getHeight();
            int srcW = bgTile.getWidth();
            int srcH = bgTile.getHeight();

            float kbg = screenH / (float) srcH;

            bgScaledW = Math.max(1, Math.round(srcW * kbg));

            if (bgScaled != null) {
                bgScaled.recycle();
                bgScaled = null;
            }

            bgScaled = Bitmap.createScaledBitmap(bgTile, bgScaledW, screenH, false);
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

            if (groundTileScaled != null) {
                groundTileScaled.recycle();
                groundTileScaled = null;
            }

            groundTileScaled = Bitmap.createScaledBitmap(groundTile, gW, gH, false);
        }
// Линия пола = нижняя граница экрана
        groundY = getHeight();
    }

    // Логика
    public void update(float dtSeconds) {
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

    public void stopLoop() {
        if (gameLoop != null) {
            gameLoop.requestStopAndJoin();
            gameLoop = null;
        }
    }

    public GameSnapshot createSnapshot() {
        GameSnapshot s = new GameSnapshot();
        s.playerX = player.getX();
        s.playerY = player.getY();
        s.cameraX = camX;
        s.playerLastDirection = player.getLastDirection();
        return s;
    }

    public void restoreFrom(GameSnapshot s) {
        player.setPosition(s.playerX, s.playerY);
        camX = s.cameraX;
        player.setLastDirection(s.playerLastDirection);
    }


}