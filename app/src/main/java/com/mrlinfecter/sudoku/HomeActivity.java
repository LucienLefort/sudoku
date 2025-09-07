package com.mrlinfecter.sudoku;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    private Random random = new Random();
    private FrameLayout root;
    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        root = findViewById(R.id.rootFrame);

        root.post(() -> {
            screenWidth = root.getWidth();
            screenHeight = root.getHeight();
            startRain();
        });

        // Boutons
        LinearLayout btnEasy = findViewById(R.id.btnEasy);
        LinearLayout btnNormal = findViewById(R.id.btnNormal);
        LinearLayout btnHard = findViewById(R.id.btnHard);

        // Récupération des records
        var prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE);
        int recordEasy = prefs.getInt("bestScore_easy", 0);
        int recordNormal = prefs.getInt("bestScore_normal", 0);
        int recordHard = prefs.getInt("bestScore_hard", 0);

        // Affichage dans les TextView
        TextView tvEasy = findViewById(R.id.recordEasy);
        TextView tvNormal = findViewById(R.id.recordNormal);
        TextView tvHard = findViewById(R.id.recordHard);

        tvEasy.setText(recordEasy > 0 ? "Record: " + recordEasy : "Record: --");
        tvNormal.setText(recordNormal > 0 ? "Record: " + recordNormal : "Record: --");
        tvHard.setText(recordHard > 0 ? "Record: " + recordHard : "Record: --");


        btnEasy.setOnClickListener(v -> startGame("easy"));
        btnNormal.setOnClickListener(v -> startGame("normal"));
        btnHard.setOnClickListener(v -> startGame("hard"));
    }

    private void startGame(String difficulty) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
    }

    private void startRain() {
        handler.post(rainRunnable);
    }

    private final Runnable rainRunnable = new Runnable() {
        @Override
        public void run() {
            // Ajouter un nouveau chiffre
            addNumber();

            // Re-lancer toutes les 150ms pour créer la pluie continue
            handler.postDelayed(this, 150);
        }
    };

    private void addNumber() {
        int n = 1 + random.nextInt(9);
        TextView tv = new TextView(this);
        tv.setText(String.valueOf(n));
        tv.setTextColor(getColor(R.color.text_primary));
        tv.setTextSize(18 + random.nextInt(12));
        tv.setAlpha(0.1f + random.nextFloat() * 0.3f);

        // Position initiale aléatoire en haut
        float x = random.nextFloat() * screenWidth;
        float y = -50f; // juste au-dessus de l'écran
        tv.setX(x);
        tv.setY(y);

        root.addView(tv);

        // Durée et position finale aléatoire
        float endY = screenHeight + 50f;
        long duration = 4000 + random.nextInt(3000);

        tv.animate()
                .translationY(endY)
                .setDuration(duration)
                .withEndAction(() -> root.removeView(tv))
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(rainRunnable);
    }
}
