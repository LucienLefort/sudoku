package com.mrlinfecter.sudoku;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class HomeActivity extends AppCompatActivity implements SensorEventListener {

    private Handler handler = new Handler();
    private Random random = new Random();
    private FrameLayout root;
    private int screenWidth;
    private int screenHeight;

    private SensorManager mSensorManager;
    private float mAccel; // accélération actuelle
    private float mAccelCurrent; // accélération actuelle (avec gravité)
    private float mAccelLast; // accélération précédente
    private boolean isFragmentAnimationRunning = false;
    private FrameLayout rootContainer;
    private Handler fragmentHandler = new Handler();
    private final int SHAKE_THRESHOLD = 15;

    private final Runnable fragmentRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFragmentAnimationRunning) {
                // CORRECTION : Appelons la méthode complète de chute et de casse
                addExtremeBreakingNumber();

                // On augmente la fréquence pour que l'effet soit visible
                fragmentHandler.postDelayed(this, 250); // Un peu plus lent que la pluie normale (150ms) mais plus dense
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        root = findViewById(R.id.rootFrame);

        root.post(() -> {
            screenWidth = root.getWidth();
            screenHeight = root.getHeight();
            startRain();

            rootContainer = root;
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
            mAccel = 0.00f;
            mAccelCurrent = SensorManager.GRAVITY_EARTH;
            mAccelLast = SensorManager.GRAVITY_EARTH;
        });

        // Boutons existants
        LinearLayout btnEasy = findViewById(R.id.btnEasy);
        LinearLayout btnNormal = findViewById(R.id.btnNormal);
        LinearLayout btnHard = findViewById(R.id.btnHard);

        ViewGroup parentContainer = (ViewGroup) btnHard.getParent();

        // Récupération des records
        var prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE);
        int recordEasy = prefs.getInt("bestScore_easy", 0);
        int recordNormal = prefs.getInt("bestScore_normal", 0);
        int recordHard = prefs.getInt("bestScore_hard", 0);

        // MODIFICATION : Vérifier l'état de déverrouillage du mode Extrême
        boolean isExtremeUnlocked = prefs.getBoolean("ExtremeModeUnlocked", false);

        // Affichage des records existants
        TextView tvEasy = findViewById(R.id.recordEasy);
        TextView tvNormal = findViewById(R.id.recordNormal);
        TextView tvHard = findViewById(R.id.recordHard);

        tvEasy.setText(recordEasy > 0 ? "Record: " + recordEasy : "Record: --");
        tvNormal.setText(recordNormal > 0 ? "Record: " + recordNormal : "Record: --");
        tvHard.setText(recordHard > 0 ? "Record: " + recordHard : "Record: --");


        final float density = getResources().getDisplayMetrics().density;
        final int padding8dp = (int) (8 * density);
        final int marginBottom16dp = (int) (16 * density);

        if (isExtremeUnlocked && parentContainer != null) {

            // 1. Créer le conteneur du bouton Extrême (LinearLayout)
            LinearLayout btnExtreme = new LinearLayout(this);
            btnExtreme.setId(View.generateViewId());

            // LayoutParams correspondant au match_parent et wrap_content
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            // Marge Basse (marginBottom="16dp")
            lp.setMargins(0, 0, 0, marginBottom16dp);
            btnExtreme.setLayoutParams(lp);

            // Style du Conteneur (Copie des attributs de btnEasy)
            btnExtreme.setBackgroundResource(R.drawable.bg_palette_number); // Fond
            btnExtreme.setOrientation(LinearLayout.VERTICAL);
            btnExtreme.setGravity(Gravity.CENTER);
            btnExtreme.setPadding(padding8dp, padding8dp, padding8dp, padding8dp); // Padding "8dp"

            // 2. Créer le TextView du titre ("Extrême")
            TextView titleExtreme = new TextView(this);
            titleExtreme.setText("Extrême");

            // Taille du titre: "20sp"
            titleExtreme.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            titleExtreme.setTypeface(null, Typeface.BOLD);

            // Couleur du titre : Utiliser R.color.text_palette comme dans le XML (Facile/Normal/Difficile)
            titleExtreme.setTextColor(ContextCompat.getColor(this, R.color.text_palette));
            titleExtreme.setGravity(Gravity.CENTER_HORIZONTAL);

            // Assurez-vous d'ajouter l'import: import android.util.TypedValue;
            // ou utilisez simplement .setTextSize(20f) si vous n'avez pas besoin d'un contrôle précis sur l'unité.

            btnExtreme.addView(titleExtreme);

            // 3. Créer le TextView du record (Score)
            TextView tvExtreme = new TextView(this);
            int recordExtreme = prefs.getInt("bestScore_extreme", 0);
            tvExtreme.setText(recordExtreme > 0 ? "Record: " + recordExtreme : "Record: --");

            // Taille du record: "14sp"
            tvExtreme.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);

            // Couleur du record: Utiliser R.color.text_accent comme dans le XML
            tvExtreme.setTextColor(ContextCompat.getColor(this, R.color.text_accent));

            // Marge Top pour le record: "4dp"
            LinearLayout.LayoutParams recordLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            recordLp.topMargin = (int) (4 * density);
            tvExtreme.setLayoutParams(recordLp);

            btnExtreme.addView(tvExtreme);

            // 4. Ajouter le nouveau bouton Extrême dans le conteneur
            int hardIndex = parentContainer.indexOfChild(btnHard);
            parentContainer.addView(btnExtreme, hardIndex + 1);

            // 5. Attacher le Listener
            btnExtreme.setOnClickListener(v -> startGame("extreme"));
        }

        // Listeners existants
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

    private void spawnBreakingFragments(float centerX, float centerY, int originalNumber) {
        int numFragments = 3 + random.nextInt(3); // 2 à 4 fragments
        for (int i = 0; i < numFragments; i++) {
            TextView fragmentTv = new TextView(this);
            fragmentTv.setText(String.valueOf(originalNumber)); // Les fragments gardent le même chiffre
            fragmentTv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            fragmentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10 + random.nextInt(6)); // Petits fragments
            fragmentTv.setAlpha(0.3f + random.nextFloat() * 0.4f);
            fragmentTv.setTypeface(null, Typeface.BOLD);

            fragmentTv.setX(centerX);
            fragmentTv.setY(centerY);

            rootContainer.addView(fragmentTv);

            double angle = random.nextDouble() * 2 * Math.PI; // Angle de dispersion aléatoire
            float distance = 100 + random.nextFloat() * 150;    // Distance de dispersion
            float endX = centerX + (float) (Math.cos(angle) * distance);
            float endY = centerY + (float) (Math.sin(angle) * distance) + (screenHeight - centerY); // Tombent vers le bas
            long duration = 1000 + random.nextInt(800);

            fragmentTv.animate()
                    .x(endX)
                    .y(endY) // Continuent de tomber
                    .alpha(0f) // Disparaissent progressivement
                    .setDuration(duration)
                    .withEndAction(() -> rootContainer.removeView(fragmentTv))
                    .start();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;

            // C'est la ligne de filtrage essentielle (déjà présente dans votre code initial)
            mAccel = mAccel * 0.9f + delta;

            if (mAccel > SHAKE_THRESHOLD) {
                // Secouage détecté !

                // Pour le mode ON/OFF, on utilise un POST_DELAYED pour éviter un double déclenchement
                // immédiat dû aux vibrations du téléphone après un secouage unique.
                fragmentHandler.postDelayed(() -> {
                    // On vérifie à nouveau mAccel pour éviter les faux positifs rapides
                    if (mAccel > SHAKE_THRESHOLD / 2) {
                        toggleEasterEgg();
                    }
                }, 150); // Délai pour stabiliser

                // IMPORTANT : Réinitialiser mAccel pour éviter les déclenchements multiples
                mAccel = 0.0f;
            }
        }
    }

    private void toggleEasterEgg() {
        if (!isFragmentAnimationRunning) {
            // --- ACTIVER l'Easter Egg ---
            isFragmentAnimationRunning = true;
            Toast.makeText(this, "⭐ EASTER EGG ACTIVÉ ! ⭐", Toast.LENGTH_SHORT).show();

            // 1. Stopper la pluie normale
            handler.removeCallbacks(rainRunnable);

            // 2. Lancer la chute cassante (via le Runnable)
            fragmentHandler.post(fragmentRunnable);
        } else {
            // --- DÉSACTIVER l'Easter Egg ---
            isFragmentAnimationRunning = false;
            Toast.makeText(this, "Easter Egg désactivé.", Toast.LENGTH_SHORT).show();

            // 1. Stopper la chute cassante
            fragmentHandler.removeCallbacks(fragmentRunnable);

            // 2. Réactiver la pluie normale
            handler.post(rainRunnable);
        }
    }

    private void startExtremeRainAnimation() {
        handler.post(extremeRainRunnable);
    }

    private final Runnable extremeRainRunnable = new Runnable() {
        @Override
        public void run() {
            addExtremeBreakingNumber();
            handler.postDelayed(this, 150); // Lancer un nouveau chiffre toutes les 300ms

        }
    };

    private void addExtremeBreakingNumber() {
        if (rootContainer == null || screenWidth == 0 || screenHeight == 0) return; // Sécurité

        int mainNumber = 1 + random.nextInt(9);
        TextView tv = new TextView(this);
        tv.setText(String.valueOf(mainNumber));
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); // Couleur "extrême"
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 + random.nextInt(12)); // Taille un peu plus grande
        tv.setTypeface(null, Typeface.BOLD);
        tv.setAlpha(0.1f + random.nextFloat() * 0.3f); // Plus visible qu'en HomeActivity

        // Position initiale aléatoire en haut
        float startX = random.nextFloat() * screenWidth;
        float startY = -50f; // Juste au-dessus de l'écran
        tv.setX(startX);
        tv.setY(startY);

        rootContainer.addView(tv);

        // Point où le chiffre va se "casser" (ex: au milieu de l'écran)
        float breakY = screenHeight * (0.3f + random.nextFloat() * 0.4f); // Entre 30% et 70% de l'écran

        // Position finale (pour la chute des fragments)
        float endY = screenHeight + 100f;
        long initialDuration = 1500 + random.nextInt(1000); // Durée avant la "casse"

        tv.animate()
                .translationY(breakY)
                .setDuration(initialDuration)
                .withEndAction(() -> {
                    // Quand le chiffre atteint le point de "casse", il disparaît
                    rootContainer.removeView(tv);

                    // Et génère des fragments
                    spawnBreakingFragments(startX, breakY, mainNumber);
                })
                .start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignoré
    }

    // NOUVEAU : Méthode pour démarrer l'animation
    private void startFragmentCelebration() {
        // Optionnel : Afficher un Toast pour confirmer l'Easter Egg
        Toast.makeText(this, "Easter Egg activé ! Secouez à nouveau pour arrêter.", Toast.LENGTH_SHORT).show();

        handler.removeCallbacks(rainRunnable);
        fragmentHandler.post(fragmentRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        }

        // NOUVEAU : Reprendre la bonne animation après une pause
        if (isFragmentAnimationRunning) {
            fragmentHandler.post(fragmentRunnable);
        } else {
            handler.post(rainRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        // NOUVEAU : Stopper les deux handlers pendant la pause
        handler.removeCallbacks(rainRunnable);
        fragmentHandler.removeCallbacks(fragmentRunnable);
    }
}
