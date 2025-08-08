package com.app.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Fixed 60 UPS + жесткий кап 60 FPS с точным frame pacing.
 * Рендер ровно каждые ~16.666 мс, без грубого sleep(16).
 */
public class GameLoop extends Thread {
    private static final int TARGET_UPS = 60;
    private static final long FIXED_DT_NS = 1_000_000_000L / TARGET_UPS;
    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private volatile boolean running;

    // РЕНДЕР КАП
    private static final long TARGET_FRAME_NS = 16_666_667L; // 60 FPS

    private volatile int fps, ups;

    public GameLoop(SurfaceHolder holder, GameView view) {
        this.surfaceHolder = holder;
        this.gameView = view;
    }

    public void setRunning(boolean running) { this.running = running; }
    public int getFps() { return fps; }
    public int getUps() { return ups; }

    @Override
    public void run() {
        long prev = System.nanoTime();
        long acc = 0L;

        long secTimer = System.currentTimeMillis();
        int frames = 0, updates = 0;

        while (running) {
            long frameStart = System.nanoTime();

            // ---- UPDATE (fixed timestep) ----
            long now = frameStart;
            long dt = now - prev;
            prev = now;
            acc += dt;

            while (acc >= FIXED_DT_NS) {
                gameView.update(FIXED_DT_NS / 1_000_000_000f);
                updates++;
                acc -= FIXED_DT_NS;
            }

            // ---- RENDER ----
            Canvas c = null;
            try {
                c = surfaceHolder.lockCanvas();
                if (c != null) {
                    synchronized (surfaceHolder) {
                        gameView.draw(c);
                    }
                }
            } finally {
                if (c != null) surfaceHolder.unlockCanvasAndPost(c);
            }
            frames++;

            // ---- STATS ----
            long t = System.currentTimeMillis();
            if (t - secTimer >= 1000) {
                fps = frames;
                ups = updates;
                frames = 0;
                updates = 0;
                secTimer += 1000;
            }

            // ---- FRAME PACING (жёсткий 60 FPS) ----
            long frameTime = System.nanoTime() - frameStart;
            long waitNs = TARGET_FRAME_NS - frameTime;
            if (waitNs > 0) {
                // Сначала nanosleep, затем короткий spin до точного дедлайна
                long end = System.nanoTime() + waitNs;
                if (waitNs > 1_000_000) {
                    try {
                        // -0.5ms чтобы не «проспать» дедлайн
                        Thread.sleep(0, (int) Math.min(waitNs - 500_000, 900_000));
                    } catch (InterruptedException ignored) {}
                }
                while (System.nanoTime() < end) {
                    // busy-wait final touch для ровного pacing
                }
            }
        }
    }
}
