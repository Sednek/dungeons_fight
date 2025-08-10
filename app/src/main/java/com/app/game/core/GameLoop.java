package com.app.game.core;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameLoop extends Thread {
    private static final int TARGET_UPS = 60;
    private static final double DT_SEC = 1.0 / TARGET_UPS;
    private static final double MAX_ACCUM_SEC = 0.25;
    private static final int MAX_UPDATES_PER_FRAME = 5;

    private final long targetFrameNs;
    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private volatile boolean running = false;

    // Stats
    private volatile int fps = 0;
    private volatile int ups = 0;

    public GameLoop(SurfaceHolder surfaceHolder, GameView gameView, float refreshRate) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
        this.targetFrameNs = (long) (1_000_000_000L / (refreshRate > 0 ? refreshRate : 60f));
        setName("GameLoop");
    }

    public void requestStopAndJoin() {
        running = false;
        try {
            join();
        } catch (InterruptedException ignored) {
        }
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
            int updatesThisFrame = 0;
            while (accumulator >= DT_SEC && updatesThisFrame < MAX_UPDATES_PER_FRAME) {
                gameView.update((float) DT_SEC);
                updates++;
                accumulator -= DT_SEC;
                updatesThisFrame++;
            }
            if (accumulator > DT_SEC) accumulator = DT_SEC;

            // render

            Canvas canvas = null;
            try {
                if (!surfaceHolder.getSurface().isValid()) {
                    continue;
                }
                canvas = surfaceHolder.lockCanvas();
                if (canvas == null) {
                    continue;
                }
                synchronized (surfaceHolder) {
                    gameView.draw(canvas);
                }
                frames++;
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
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
            long remainingNs = targetFrameNs - frameTimeNs;
            if (remainingNs > 200_000) { // >0.2ms
                try {
                    // sleep for a bit less than the remaining to avoid oversleeping
                    //noinspection BusyWait
                    Thread.sleep(0, (int) Math.min(remainingNs - 100_000, 900_000));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
