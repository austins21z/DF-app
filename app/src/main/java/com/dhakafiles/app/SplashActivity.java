package com.dhakafiles.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView tagline = findViewById(R.id.splash_tagline);
        ProgressBar progress = findViewById(R.id.splash_progress);

        logo.setAlpha(0f);
        logo.setScaleX(0.3f);
        logo.setScaleY(0.3f);
        appName.setAlpha(0f);
        appName.setTranslationY(50f);
        tagline.setAlpha(0f);
        progress.setAlpha(0f);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(logo, "scaleX", 0.3f, 1f),
                    ObjectAnimator.ofFloat(logo, "scaleY", 0.3f, 1f)
            );
            set.setDuration(800);
            set.setInterpolator(new OvershootInterpolator(1.5f));
            set.start();
        }, 300);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(appName, "translationY", 50f, 0f)
            );
            set.setDuration(600);
            set.start();
        }, 800);

        new Handler(Looper.getMainLooper()).postDelayed(() ->
                ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f).setDuration(500).start(), 1200);

        new Handler(Looper.getMainLooper()).postDelayed(() ->
                ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f).setDuration(400).start(), 1500);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3000);
    }
}
