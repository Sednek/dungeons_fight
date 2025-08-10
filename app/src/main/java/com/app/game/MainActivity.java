package com.app.game;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String SNAPSHOT_FILE = "game_snapshot.bin";

    private GameView gameView;

    private void enableImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableImmersive();

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableImmersive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.stopLoop();
            saveSnapshot(gameView.createSnapshot());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GameSnapshot snapshot = loadSnapshot();
        if (snapshot != null && gameView != null) {
            gameView.setPendingSnapshot(snapshot);
        }
    }

    private void saveSnapshot(GameSnapshot s) {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                openFileOutput(SNAPSHOT_FILE, MODE_PRIVATE))) {
            oos.writeObject(s);
        } catch (java.io.IOException ignored) {}
    }

    private GameSnapshot loadSnapshot() {
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                openFileInput(SNAPSHOT_FILE))) {
            return (GameSnapshot) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}