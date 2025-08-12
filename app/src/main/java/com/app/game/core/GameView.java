package com.app.game.core;

import com.app.game.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.app.game.player.Player;
import com.app.game.shop.Shop;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameLoop gameLoop;
    private GameSnapshot pendingSnapshot;

    private int screenWidth;
    private int screenHeight;

    //WorldX
    private static final float leftX = -1000f;
    private static final float rightX = 3000f;

    //Player

    //во сколько раз увеличить кадр спрайта на экране
    private static final float PLAYER_SCALE_SIZE = 5f;
    private Player player;


    // HUD-краска и кэш строк
    private final Paint hudPaint = new Paint();
    private String fpsText = "";
    private String upsText = "";

    // Камера
    private static final float CAM_HALF_LIFE_SEC = 0.12f;
    private float camX = 0f;


    // Background
    private static final float BG_PARALLAX = 0.3f; // фон движется медленнее камеры
    private Bitmap bgTile;     // seamless_bg.png
    private Bitmap bgScaled;
    private Bitmap bgStrip;             // кэш-полоса фона
    private int bgStripW, bgStripH;     // размеры кэш-полосы

    //Ground tile

    // т.к тайл персонажа 80px, а мы за основу берем серидину персонажа(40px)
    // то надо смещать персонажа по пропорции, чтобы ноги касались пола
    private static final float PLAYER_OFFSET_FOR_GROUND = 0.17f;
    private static final float GROUND_PARALLAX = 1.0f;
    private static final int GROUND_SCALE = 2;
    private static final int GROUND_OFFSET_Y = 20; // на сколько пикселей опущен тайл
    private Bitmap groundTile;
    private Bitmap groundTileScaled;
    private int groundTileWidth, groundTileHeight;
    private int groundDrawHeightPx;
    private float groundY;
    private final Rect groundSrc = new Rect();
    private Bitmap groundStrip;         // кэш-полоса земли
    private int groundStripW, groundStripH;

    //Shop
    private Shop shop;


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);

        hudPaint.setColor(Color.GREEN);
        hudPaint.setTextSize(14f * getResources().getDisplayMetrics().scaledDensity);
        hudPaint.setAntiAlias(true);

        setFocusable(true);
    }

    public void setPendingSnapshot(GameSnapshot snapshot) {
        this.pendingSnapshot = snapshot;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        player = new Player(getResources(), getWidth() / 2f, getHeight() / 2f, 500f, PLAYER_SCALE_SIZE);
        if (shop == null) shop = new Shop();
        shop.load(getResources(), R.drawable.shop);

        float refreshRate = (getDisplay() != null) ? getDisplay().getRefreshRate() : 60f;

        gameLoop = new GameLoop(getHolder(), this, refreshRate);
        gameLoop.setRunning(true);
        gameLoop.start();

        if (pendingSnapshot != null) {
            restoreFrom(pendingSnapshot);
            pendingSnapshot = null;
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        camX = player.getX() - screenWidth * 0.5f;
        camX = clampCameraLeft(camX);

        //Если бгтайл нету, то берем из ресурсов
        if (bgTile == null) {
            bgTile = BitmapFactory.decodeResource(getResources(), R.drawable.seamless_bg);
        }
        // Предварительно масштабируем bg до высоты экрана один раз
        if (bgTile != null) {
            int screenH = getHeight();
            int srcW = bgTile.getWidth();
            int srcH = bgTile.getHeight();

            float koeffBg = screenH / (float) srcH;

            int bgScaledW = Math.max(1, Math.round(srcW * koeffBg));

            if (bgScaled != null) {
                bgScaled.recycle();
                bgScaled = null;
            }

            bgScaled = Bitmap.createScaledBitmap(bgTile, bgScaledW, screenH, false);
            rebuildBgStripIfNeeded();
        }

        if (groundTile == null) {
            groundTile = BitmapFactory.decodeResource(getResources(), R.drawable.ground_tile_dark);
            groundTileWidth = groundTile.getWidth();
            groundTileHeight = groundTile.getHeight();
            groundSrc.set(0, 0, groundTileWidth, groundTileHeight);
        }

        // Увеличиваем высоту в GROUND_SCALE раз относительно оригинала
        groundDrawHeightPx = groundTileHeight * GROUND_SCALE;


        // Предварительно масштабируем тайл земли до высоты экрана один раз
        if (groundTile != null) {
            float kg = groundDrawHeightPx / (float) groundTileHeight;
            int gW = Math.max(1, Math.round(groundTileWidth * kg));
            int gH = Math.max(1, Math.round(groundTileHeight * kg));

            if (groundTileScaled != null) {
                groundTileScaled.recycle();
                groundTileScaled = null;
            }
            groundTileScaled = Bitmap.createScaledBitmap(groundTile, gW, gH, false);
            rebuildGroundStripIfNeeded();
        }
        // Линия пола = нижняя граница экрана
        groundY = getHeight();

        shop.onSurfaceChanged(groundDrawHeightPx);

        if (player != null) {
            float playerHalfHeight = player.getDrawHeight() * 0.5f;
            //PLAYER_OFFSET_FOR_GROUND подобран эпирически под текущий спрайт. Если другой спрайт - высчитывать пропорцию
            player.setY(groundY + GROUND_OFFSET_Y - groundDrawHeightPx - playerHalfHeight * PLAYER_OFFSET_FOR_GROUND);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // Убиваем геймлупу, перед удалением, проверить сохраняемость новых данных если есть
        stopLoop();
        // освобождение битмапов
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
        if (shop != null) {
            shop.dispose();
            shop = null;
        }

        if (player != null) {
            player.dispose();
        }
    }

    // Логика
    public void update(float dtSeconds) {
        if (gameLoop != null) {
            fpsText = "FPS: " + gameLoop.getFps();
            upsText = "UPS: " + gameLoop.getUps();
        }

        if (player != null) {
            player.update(dtSeconds);

            //half-life камера
            final float targetLeft = player.getX() - screenWidth * 0.5f; // целимся держать игрока по центру
            final float k = (float) Math.pow(0.5, dtSeconds / CAM_HALF_LIFE_SEC);
            camX = k * camX + (1f - k) * targetLeft;

            camX = clampCameraLeft(camX);

            int playerW = player.getDrawWidth();
            float half = playerW * 0.1f;

            float minCenterX = leftX + half;
            float maxCenterX = rightX - half;

            float px = player.getX();
            if (px < minCenterX) {
                player.setPosition(minCenterX, player.getY());
            } else if (px > maxCenterX) {
                player.setPosition(maxCenterX, player.getY());
            }
        }
    }


    // Рисуем тут бгшку, тайл земли, потом счетчик фпса(в дальнейшем худ отладки) и игрока.
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        drawLoopedBackground(canvas);
        drawGround(canvas);

        float groundTopY = groundY + GROUND_OFFSET_Y - groundDrawHeightPx * 0.63f;
        if (shop != null) {
            shop.draw(canvas, camX, (int) groundTopY);
        }

        if (gameLoop != null) {
            canvas.drawText(fpsText + " " + upsText, 32, 64, hudPaint);
        }

        if (player != null) player.draw(canvas, camX);
    }

    //Логика событий касания
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

    //Логика бесконечного бг
    private void drawLoopedBackground(Canvas canvas) {
        if (bgStrip == null || bgStrip.isRecycled()) {
            // запасной вариант — однотонный фон или прежний код
            canvas.drawColor(0xFF000000);
            return;
        }

        if (bgScaled == null || bgScaled.isRecycled()) return;
        int tileW = bgScaled.getWidth();
        int scroll = (int) (camX * BG_PARALLAX);
        // смещение в диапазоне [-tileW, 0]
        int offsetX = -(scroll % tileW);
        if (offsetX > 0) offsetX -= tileW;

        // полоса шире экрана на 1 тайл — хватает одного вызова
        canvas.drawBitmap(bgStrip, offsetX, 0, null);
    }

    //Логика тайлов земли
    private void drawGround(Canvas canvas) {
        if (groundStrip == null || groundStrip.isRecycled()) return;

        if (groundTileScaled == null || groundTileScaled.isRecycled()) return;
        int tileW = groundTileScaled.getWidth();
        int scroll = (int) (camX * GROUND_PARALLAX);
        int offsetX = -(scroll % tileW);
        if (offsetX > 0) offsetX -= tileW;

        int destTop = getHeight() - groundDrawHeightPx + GROUND_OFFSET_Y;
        canvas.drawBitmap(groundStrip, offsetX, destTop, null);
    }

    private void rebuildBgStripIfNeeded() {
        if (bgScaled == null || bgScaled.isRecycled()) {
            bgStrip = null;
            return;
        }
        int screenW = getWidth();
        int screenH = getHeight();
        if (screenW <= 0 || screenH <= 0) return;

        int tileW = bgScaled.getWidth();
        int tileH = bgScaled.getHeight();

        int desiredW = screenW + tileW;   // запас на 1 тайл, чтобы всегда хватало на сдвиг

        if (bgStrip != null && bgStripW == desiredW && bgStripH == tileH && !bgStrip.isRecycled())
            return;

        if (bgStrip != null && !bgStrip.isRecycled()) bgStrip.recycle();
        bgStrip = Bitmap.createBitmap(desiredW, tileH, Bitmap.Config.ARGB_8888);
        bgStripW = desiredW;
        bgStripH = tileH;

        Canvas c = new Canvas(bgStrip);
        // заполняем полосу фоновой плиткой
        for (int x = 0; x < bgStripW; x += tileW) {
            c.drawBitmap(bgScaled, x, 0, null);
        }
    }

    private void rebuildGroundStripIfNeeded() {
        if (groundTileScaled == null || groundTileScaled.isRecycled()) {
            groundStrip = null;
            return;
        }
        int screenW = getWidth();
        int screenH = getHeight();
        if (screenW <= 0 || screenH <= 0) return;

        int tileW = groundTileScaled.getWidth();
        int tileH = groundTileScaled.getHeight();

        int desiredW = screenW + tileW;   // запас на 1 тайл

        if (groundStrip != null && groundStripW == desiredW && groundStripH == tileH && !groundStrip.isRecycled())
            return;

        if (groundStrip != null && !groundStrip.isRecycled()) groundStrip.recycle();
        groundStrip = Bitmap.createBitmap(desiredW, tileH, Bitmap.Config.ARGB_8888);
        groundStripW = desiredW;
        groundStripH = tileH;

        Canvas c = new Canvas(groundStrip);
        for (int x = 0; x < groundStripW; x += tileW) {
            c.drawBitmap(groundTileScaled, x, 0, null);
        }
    }

    private float clampCameraLeft(float value) {
        // Если мир уже уже/равен ширине экрана — фиксируй «центр» мира
        if ((rightX - leftX) <= screenWidth) {
            return (leftX + rightX) * 0.5f - (screenWidth * 0.5f);
        }

        // Диапазон допустимых положений ЛЕВОЙ грани камеры
        final float minLeft = leftX;                 // левее — левая граница войдёт в экран
        final float maxLeft = rightX - screenWidth;  // правее — правая граница войдёт в экран

        // Небольшой гистерезис от дрожания на пиксель
        final float epsilon = 0.5f;

        if (value < minLeft) return minLeft - epsilon;
        if (value > maxLeft) return maxLeft + epsilon;
        return value;
    }


    //Остановить gameLoop
    public void stopLoop() {
        if (gameLoop != null) {
            gameLoop.requestStopAndJoin();
            gameLoop = null;
        }
    }

    //Логика создания снапшота сохранения
    //TODO: возможно вынести в отдельный класс при разрастании проекта
    public GameSnapshot createSnapshot() {
        GameSnapshot s = new GameSnapshot();
        s.playerX = player.getX();
        s.playerY = player.getY();
        s.cameraX = camX;
        s.playerLastDirection = player.getLastDirX();
        return s;
    }

    //Логика взятия данных из снапшота сохранения
    //Todo: также как и createSnapshot() - при разрастании проекта возможно вынести в отедльный класс
    public void restoreFrom(GameSnapshot s) {
        player.setPosition(s.playerX, s.playerY);
        camX = s.cameraX;
        player.setLastDirX(s.playerLastDirection);
    }


}