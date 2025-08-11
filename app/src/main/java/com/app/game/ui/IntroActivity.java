package com.app.game.ui;

import com.app.game.R;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Корневой контейнер с чёрным фоном (сюда вешаем скип по тапу)
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        // Видео на всю область; дальше мы его ДООТМАСШТАБИРУЕМ вниз через setScaleX/Y
        VideoView videoView = new VideoView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        );
        videoView.setLayoutParams(lp);
        root.addView(videoView);
        setContentView(root);

        // Скип по тапу В ЛЮБОЙ ТОЧКЕ
        root.setOnClickListener(v -> goNext());

        // Загружаем из res/raw/intro.mp4
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro_video_small);
        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);

            int videoW = mp.getVideoWidth();
            int videoH = mp.getVideoHeight();

            DisplayMetrics dm = getResources().getDisplayMetrics();
            float screenW = dm.widthPixels;
            float screenH = dm.heightPixels;

            // Масштаб "fit" (целиком в экран), но ТОЛЬКО уменьшение (не больше 1.0)
            float scaleW = screenW / videoW;
            float scaleH = screenH / videoH;
            float fitScale = Math.min(scaleW, scaleH);
            if (fitScale < 1f) {
                videoView.setScaleX(fitScale);
                videoView.setScaleY(fitScale);
            } else {
                // Не апскейлим — оставляем 1:1, будет по центру с полями
                videoView.setScaleX(1f);
                videoView.setScaleY(1f);
            }

            videoView.start();
        });

        // Автопереход по окончании
        videoView.setOnCompletionListener(mp -> goNext());
    }

    private void goNext() {
        if (finished) return;
        finished = true;
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // В конце IntroActivity (после заставки/логики)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // закрываем интро, чтобы не вернуться назад
    }
}
