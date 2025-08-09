package com.app.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameLoop extends Thread {
    private static final int TARGET_UPS = 60;
    private static final double DT_SEC = 1.0 / TARGET_UPS;
    private static final double MAX_ACCUM_SEC = 0.25; // clamp to avoid spiral of death

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private volatile boolean running = false;

    // Stats
    private volatile int fps = 0;
    private volatile int ups = 0;

    public GameLoop(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
        setName("GameLoop");
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getFps() {
        return fps;
    }

    public int getUps() {
        return ups;
    }

    @Override
    public void run() {
        long previousNs = System.nanoTime();
        double accumulator = 0.0;

        int frames = 0;
        int updates = 0;
        long secTimerMs = System.currentTimeMillis();

        while (running) {
            long nowNs = System.nanoTime();
            double frameDeltaSec = (nowNs - previousNs) / 1_000_000_000.0;
            previousNs = nowNs;

            // clamp spikes
            accumulator += Math.min(frameDeltaSec, MAX_ACCUM_SEC);

            // fixed updates
            while (accumulator >= DT_SEC) {
                gameView.update((float) DT_SEC);
                updates++;
                accumulator -= DT_SEC;
            }

            // render
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                try {
                    gameView.draw(canvas);
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
                frames++;
            }

            // publish stats each second
            long nowMs = System.currentTimeMillis();
            if (nowMs - secTimerMs >= 1000) {
                fps = frames;
                ups = updates;
                frames = 0;
                updates = 0;
                secTimerMs += 1000;
            }

            // (Optional) soft cap to ~60 FPS if device is too fast
            // We can sleep a tiny bit to reduce CPU usage.
            long frameTimeNs = System.nanoTime() - nowNs;
            long targetFrameNs = (long) (1_000_000_000L / 60.0);
            long remainingNs = targetFrameNs - frameTimeNs;
            if (remainingNs > 200_000) { // >0.2ms
                try {
                    // sleep for a bit less than the remaining to avoid oversleeping
                    Thread.sleep(0, (int) Math.min(remainingNs - 100_000, 900_000));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
