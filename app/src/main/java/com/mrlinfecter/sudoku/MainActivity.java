    package com.mrlinfecter.sudoku;

    import android.annotation.SuppressLint;
    import android.app.AlertDialog;
    import android.content.ClipData;
    import android.content.Intent;
    import android.content.res.Configuration;
    import android.graphics.Color;
    import android.graphics.Point;
    import android.graphics.Typeface;
    import android.os.Bundle;
    import android.os.CountDownTimer;
    import android.util.Log;
    import android.view.DragEvent;
    import android.view.Gravity;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.FrameLayout;
    import android.widget.GridLayout;
    import android.widget.LinearLayout;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.content.ContextCompat;

    import java.util.Arrays;
    import java.util.Random;
    import java.util.concurrent.atomic.AtomicBoolean;

    public class MainActivity extends AppCompatActivity {

        private int[][] puzzle;
        private int[][] solution;

        private GridLayout grid;
        private LinearLayout palette;
        private TextView statusText, scoreText, timerText;

        private int score = 0;
        private CountDownTimer timer;
        private int seconds = 0;
        private int bestScore = 0;
        private int bestTime = Integer.MAX_VALUE; // secondes
        private TextView recordText;
        private boolean darkMode = false;
        private TextView selectedCell = null;
        private TextView oldSelectedCell = null;
        private LinearLayout hintsContainer;
        private TextView hintsText;
        private int highlightedNumber = -1;

        private boolean helpActivate = false;

        private int selectedNumber = -1;
        private TextView selectedNumberView = null;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // === Init des vues d'abord ===
            grid = findViewById(R.id.sudokuGrid);
            palette = findViewById(R.id.palette);
            statusText = findViewById(R.id.statusText);
            scoreText = findViewById(R.id.scoreText);
            timerText = findViewById(R.id.timerText);
            recordText = findViewById(R.id.recordText);

            Button testWinButton = findViewById(R.id.testWinButton);
            //testWinButton.setOnClickListener(v -> checkWinAnimation());
            testWinButton.setVisibility(View.INVISIBLE);

            String difficulty = getIntent().getStringExtra("difficulty");
            if (difficulty == null) difficulty = "normal";

            // === Record ===
            loadRecord(difficulty);
            updateRecordText();

            // === Détection du thème ===
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            darkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

            // === Sudoku ===
            SudokuGenerator generator = new SudokuGenerator();
            int emptyCells;
            switch (difficulty) {
                case "easy": emptyCells = 30; break;   // + facile
                case "hard": emptyCells = 50; break;   // + difficile
                default: emptyCells = 40; break;       // normal
            }

            solution = generator.generateSolution();
            puzzle = generator.generatePuzzle(solution, emptyCells);


            // Construction de la grille après mesure
            grid.post(() -> {
                int width = grid.getWidth();
                int height = grid.getHeight();
                int size = Math.min(width, height);

                ViewGroup.LayoutParams params = grid.getLayoutParams();
                params.width = size;
                params.height = size;
                grid.setLayoutParams(params);

                buildGrid();
                buildPalette();
                applyTheme();
                startTimer();
            });

            hintsText = findViewById(R.id.hintsText);
            hintsText.setText("Activer l'aide ? 💡");

            hintsText.setOnClickListener(v -> {

                Popup.show(this, yes -> {
                    if (yes) {
                        helpActivate = true;
                    } else {
                        helpActivate = false;
                    }
                });


            });

        }

        private void showPossibleNumbers(CellTag tag, TextView cell) {
            if (tag.fixed) {
                hintsText.setText("");
                return;
            }

            oldSelectedCell = selectedCell;
            selectedCell = cell;

            boolean[] possible = getPossibleNumbers(tag.r, tag.c);

            StringBuilder sb = new StringBuilder();
            sb.append("💡");
            for (int n = 1; n <= 9; n++) {
                if (possible[n]) sb.append(n).append(" ");
            }

            hintsText.setText(sb.toString().trim());
            hintsText.setVisibility(View.VISIBLE);

            // Ajout du clic sur les chiffres dans le TextView
            hintsText.setOnClickListener(v -> {
                String text = hintsText.getText().toString().replaceAll("\\s+", "").replace("💡", "");
                if (text.isEmpty() || text.length() > 1) return;

                // ici tu peux choisir le premier chiffre possible pour test ou un chiffre fixe pour cliquer
                int number = Character.getNumericValue(text.charAt(0));
                if (selectedCell != null ) {
                    selectedCell.setText(String.valueOf(number));
                    puzzle[tag.r][tag.c] = number;
                    if (number == solution[tag.r][tag.c]) {
                        tag.fixed = true;
                        selectedCell.setTextColor(getColor(R.color.text_primary));
                        score += 10;
                        updateScore();
                        highlightNumbers();
                        checkWin();
                    } else {
                        score -= 10;
                        updateScore();
                        selectedCell.setText("");
                    }
                }
            });
        }




        private boolean[] getPossibleNumbers(int row, int col) {
            boolean[] possible = new boolean[10]; // index 1..9
            for (int i = 1; i <= 9; i++) possible[i] = true;

            // Ligne
            for (int c = 0; c < 9; c++) {
                int val = puzzle[row][c];
                if (val != 0) possible[val] = false;
            }

            // Colonne
            for (int r = 0; r < 9; r++) {
                int val = puzzle[r][col];
                if (val != 0) possible[val] = false;
            }

            // Bloc 3x3
            int startRow = (row / 3) * 3;
            int startCol = (col / 3) * 3;
            for (int r = startRow; r < startRow + 3; r++) {
                for (int c = startCol; c < startCol + 3; c++) {
                    int val = puzzle[r][c];
                    if (val != 0) possible[val] = false;
                }
            }

            return possible;
        }






        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);

            if ((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                darkMode = true;
            } else {
                darkMode = false;
            }
            applyTheme();
        }

        private void startTimer() {
            timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    seconds++;
                    int mins = seconds / 60;
                    int secs = seconds % 60;
                    timerText.setText(String.format("Temps: %02d:%02d", mins, secs));
                }

                @Override
                public void onFinish() {}
            }.start();
        }

        private void buildGrid() {
            grid.removeAllViews();
            int size = Math.min(grid.getWidth(), grid.getHeight());

            int thin = dp(1);
            int thick = dp(4);

            int totalGaps = 4 * thick + 6 * thin;
            int cellSize = (size - totalGaps) / 9;

            int leftover = size - (cellSize * 9 + totalGaps);

            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    TextView tv = new TextView(this);
                    GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                    lp.width = cellSize;
                    lp.height = cellSize;

                    int left   = (c == 0 || c % 3 == 0) ? thick : thin;
                    int top    = (r == 0 || r % 3 == 0) ? thick : thin;
                    int right  = (c == 8) ? thick + leftover : 0;
                    int bottom = (r == 8) ? thick + leftover : 0;

                    lp.setMargins(left, top, right, bottom);
                    tv.setLayoutParams(lp);

                    tv.setTextSize(20f);
                    tv.setTypeface(Typeface.DEFAULT_BOLD);
                    tv.setGravity(Gravity.CENTER);

                    int value = puzzle[r][c];
                    boolean isFixed = value != 0;

                    if (isFixed) {
                        tv.setText(String.valueOf(value));
                        // couleur cellule fixe (peut être overridée par applyTheme)
                        tv.setTextColor(getColorOrFallback(R.color.text_primary, android.R.color.black));
                    } else {
                        tv.setText("");
                        tv.setTextColor(Color.DKGRAY);
                    }

                    // IMPORTANT : ne pas désactiver la vue -> on veut recevoir les clics
                    tv.setEnabled(true);
                    tv.setClickable(true);

                    // tag contient maintenant l'info si la case est fixe
                    tv.setTag(new CellTag(r, c, isFixed));

                    // click : bascule du surlignage
                    final TextView tvRef = tv;
                    tv.setOnClickListener(v -> {
                        CellTag tag = (CellTag) tvRef.getTag();

                        // ✅ Si le mode aide est activé → affiche les chiffres possibles
                        if (helpActivate) {
                            showPossibleNumbers(tag, tvRef);
                            return;
                        }

                        String val = tvRef.getText().toString();

                        // ✅ Si un chiffre est sélectionné dans la palette et que la case est vide et non fixe
                        if (selectedNumber != -1 && val.isEmpty() && !tag.fixed) {
                            int correct = solution[tag.r][tag.c];

                            if (selectedNumber == correct) {
                                // ✅ Bonne réponse
                                tvRef.setText(String.valueOf(selectedNumber));
                                tvRef.setTextColor(getColor(R.color.text_primary));
                                tag.fixed = true;
                                puzzle[tag.r][tag.c] = selectedNumber;
                                score += 10;
                                updateScore();
                                highlightNumbers();
                                checkWin();

                                // Petit effet visuel vert
                                tvRef.setBackgroundColor(getColor(R.color.bg_cell_good));
                                tvRef.postDelayed(() -> tvRef.setBackgroundColor(getColor(R.color.bg_cell_empty)), 200);

                            } else {
                                // ❌ Mauvaise réponse
                                tvRef.setBackgroundColor(getColor(R.color.bg_cell_not_good));
                                tvRef.postDelayed(() -> tvRef.setBackgroundColor(getColor(R.color.bg_cell_empty)), 200);
                                Toast.makeText(this, "❌ Mauvais chiffre", Toast.LENGTH_SHORT).show();
                                score = Math.max(0, score - 5);
                                updateScore();
                            }

                            // ⚠️ On NE désélectionne PAS le chiffre palette ici
                            // Il restera actif tant que l’utilisateur ne clique pas sur un autre chiffre de la palette
                            return;
                        }

                        // ✅ Gestion de la sélection/surlignage classique
                        if (!val.isEmpty()) {
                            int num = Integer.parseInt(val);
                            highlightedNumber = (highlightedNumber == num) ? -1 : num; // toggle
                            if (selectedCell != null) {
                                selectedCell.setBackgroundColor(getColor(R.color.bg_cell_empty));
                                selectedCell = null;
                            }
                        } else if (selectedCell != null) {
                            selectedCell.setBackgroundColor(getColor(R.color.bg_cell_empty_selected));
                        }

                        if (oldSelectedCell != null && oldSelectedCell != selectedCell)
                            oldSelectedCell.setBackgroundColor(getColor(R.color.bg_cell_empty));

                        highlightNumbers();
                    });

                    grid.addView(tv);
                }
            }

            // reset du surlignage (au cas où)
            highlightNumbers();
        }


        private void highlightNumbers() {
            int highlightColor = getColor(R.color.text_record);

            for (int i = 0; i < grid.getChildCount(); i++) {
                TextView tv = (TextView) grid.getChildAt(i);
                CellTag tag = (CellTag) tv.getTag();
                String s = tv.getText().toString();
                if (!s.isEmpty() && tag != null) {
                    int num = Integer.parseInt(s);
                    if (highlightedNumber != -1 && num == highlightedNumber) {
                        tv.setTextColor(highlightColor);
                    } else {
                        // Remet la couleur normale selon si la cellule est fixe ou non
                        if (tag.fixed) {
                            tv.setTextColor(getColorOrFallback(R.color.text_primary, android.R.color.black));
                        } else {
                            tv.setTextColor(Color.DKGRAY);
                        }
                    }
                }
            }
        }



        private int getColorOrFallback(int resId, int fallbackResId) {
            try {
                return getColor(resId);
            } catch (Exception e) {
                return getColor(fallbackResId);
            }
        }



        private void buildPalette() {
            palette.removeAllViews();
            for (int n = 1; n <= 9; n++) {
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                tv.setLayoutParams(lp);

                tv.setText(String.valueOf(n));
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(20f);
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                int pad = dp(12);
                tv.setPadding(pad, pad, pad, pad);
                tv.setBackgroundResource(R.drawable.bg_palette_number);

                final int number = n;
                tv.setLongClickable(true);
                tv.setOnLongClickListener(v -> {
                    v.startDragAndDrop(ClipData.newPlainText("number", String.valueOf(number)),
                            new NumberDragShadowBuilder(v), null, 0);
                    return true;
                });

                // ✅ Ajout du mode "tap-to-place"
                tv.setOnClickListener(v -> {
                    if (selectedNumberView != null) {
                        selectedNumberView.setBackgroundResource(R.drawable.bg_palette_number);
                    }
                    selectedNumber = number;
                    selectedNumberView = tv;
                    tv.setBackgroundResource(R.drawable.bg_palette_number_selected);
                });

                palette.addView(tv);
            }

            // s'assurer que le listener de la grille est (ré)attaché
            grid.setOnDragListener(globalGridDragListener);
        }




        private TextView lastHoverCell = null;

        private final View.OnDragListener globalGridDragListener = (v, event) -> {
            final int bgCell = getColor(R.color.bg_cell_empty);
            final int bgCellGood = getColor(R.color.bg_cell_good);
            final int bgCellNotGood = getColor(R.color.bg_cell_not_good);

            float shadowOffsetY = dp(-25); // -50 même offset que dans NumberDragShadowBuilder
            float shadowOffsetX = dp(0);

            // On compense l'offset pour retrouver la vraie position dans la grille
            float x = event.getX() + shadowOffsetX;
            float y = event.getY() + shadowOffsetY;

            // Clipper pour rester dans les bornes de la grille
            x = Math.max(0, Math.min(x, grid.getWidth() - 1));
            y = Math.max(0, Math.min(y, grid.getHeight() - 1));

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION: {
                    TextView hoverCell = findCellUnder(x, y);
                    if (hoverCell != null && hoverCell != lastHoverCell) {
                        CellTag ht = (CellTag) hoverCell.getTag();
                        boolean editable = (ht != null && !ht.fixed);
                        if (editable) {
                            if (lastHoverCell != null) lastHoverCell.setBackgroundColor(bgCell);
                            hoverCell.setBackgroundColor(Color.LTGRAY);
                            lastHoverCell = hoverCell;
                        }
                    }
                    break;
                }

                case DragEvent.ACTION_DRAG_EXITED:
                    if (lastHoverCell != null) {
                        lastHoverCell.setBackgroundColor(bgCell);
                        lastHoverCell = null;
                    }
                    break;

                case DragEvent.ACTION_DROP: {
                    if (lastHoverCell != null) lastHoverCell.setBackgroundColor(bgCell);

                    if (event.getClipData() == null || event.getClipData().getItemCount() == 0) return false;
                    CharSequence clip = event.getClipData().getItemAt(0).getText();
                    if (clip == null) return false;

                    int number;
                    try {
                        number = Integer.parseInt(clip.toString());
                    } catch (NumberFormatException e) {
                        return false;
                    }

                    TextView targetCell = findCellUnder(x, y);
                    if (targetCell != null) {
                        CellTag tag = (CellTag) targetCell.getTag();
                        boolean editable = (tag != null && !tag.fixed);
                        if (editable) {
                            int correct = solution[tag.r][tag.c];

                            if (number == correct) {
                                targetCell.setText(String.valueOf(number));
                                tag.fixed = true; // ✅ la case devient "fixée" après validation
                                targetCell.setBackgroundColor(bgCellGood);
                                score += 10;
                                statusText.setText("✔ Correct !");
                                updateScore();
                                checkWin();
                                targetCell.postDelayed(() -> targetCell.setBackgroundColor(bgCell), 350);

                                puzzle[tag.r][tag.c] = number;
                                highlightNumbers();
                            } else {
                                targetCell.setBackgroundColor(bgCellNotGood);
                                targetCell.postDelayed(() -> targetCell.setBackgroundColor(bgCell), 350);
                                score = Math.max(0, score - 5);
                                Toast.makeText(this, "❌ Mauvais chiffre", Toast.LENGTH_SHORT).show();
                                updateScore();
                            }
                        }
                    }
                    lastHoverCell = null;
                    break;
                }

                case DragEvent.ACTION_DRAG_ENDED:
                    if (lastHoverCell != null) {
                        lastHoverCell.setBackgroundColor(bgCell);
                        lastHoverCell = null;
                    }
                    break;
            }
            return true;
        };



        // Fonction utilitaire pour trouver la cellule sous des coordonnées x, y
        private TextView findCellUnder(float x, float yFinger) {
            // Limiter x dans la grille
            x = Math.max(0, Math.min(x, grid.getWidth() - 1));

            // Trouver la colonne correspondant à x
            int col = 0;
            for (int c = 0; c < 9; c++) {
                TextView cell = (TextView) grid.getChildAt(c); // cast
                if (x >= cell.getLeft() && x <= cell.getRight()) {
                    col = c;
                    break;
                }
            }

            // Calculer la ligne selon la position réelle du doigt
            float rowHeight = (float) grid.getHeight() / 9;
            int row = (int)(yFinger / rowHeight);

            // Accrocher première ou dernière ligne si hors de la grille
            if (row < 0) row = 0;
            if (row > 8) row = 8;

            return (TextView) grid.getChildAt(row * 9 + col);
        }


        private void updateScore() {
            scoreText.setText("Score: " + score);
        }

        private void checkWin() {
            for (int i = 0; i < grid.getChildCount(); i++) {
                TextView child = (TextView) grid.getChildAt(i);
                CellTag tag = (CellTag) child.getTag();
                int expected = solution[tag.r][tag.c];
                String s = child.getText().toString();
                int value = s.isEmpty() ? 0 : Integer.parseInt(s);
                if (value != expected) return;
            }

            int timeBonus = Math.max(0, 1800 - seconds);
            int finalScore = score + timeBonus;

            if(helpActivate) finalScore = finalScore /2;

            if (finalScore > bestScore || (finalScore == bestScore && seconds < bestTime)) {
                bestScore = finalScore;
                bestTime = seconds;
                String difficulty = getIntent().getStringExtra("difficulty");
                if (difficulty == null) difficulty = "normal";
                saveRecord(difficulty);
                updateRecordText();
                Toast.makeText(this, "🎖 Nouveau record !", Toast.LENGTH_LONG).show();
            }

            score = finalScore;

            statusText.setText("🎉 Sudoku terminé !");
            if (timer != null) timer.cancel();

            checkWinAnimation();

            //grid.postDelayed(this::resetGame, 2500);
        }

        private void checkWinAnimation() {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);

            int bg_end = getColor(R.color.bg_palette);
            int text_color_end = getColor(R.color.text_record);

            // Message central
            TextView congrats = new TextView(this);
            congrats.setText("🎉 Félicitations ! 🎉\nVotre Score: " + score);
            congrats.setBackgroundColor(bg_end);
            congrats.setBackgroundColor(Color.argb(180, Color.red(bg_end), Color.green(bg_end), Color.blue(bg_end)));
            congrats.setTextSize(32f);
            congrats.setTypeface(Typeface.DEFAULT_BOLD);
            congrats.setTextColor(text_color_end);
            congrats.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams msgParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            congrats.setLayoutParams(msgParams);
            root.addView(congrats);

            // Overlay pour les explosions
            FrameLayout overlay = new FrameLayout(this);
            overlay.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            root.addView(overlay);

            int screenW = root.getWidth();
            int screenH = root.getHeight();

            AtomicBoolean fireworkRunning = new AtomicBoolean(true);

            // Runnable pour générer des explosions
            Runnable fireworkRunnable = new Runnable() {
                Random random = new Random();

                @Override
                public void run() {
                    if (!fireworkRunning.get()) return;

                    int numExplosions = 3 + random.nextInt(3);
                    for (int e = 0; e < numExplosions; e++) {
                        float centerX = random.nextInt(screenW);
                        float centerY = random.nextInt(screenH);

                        int numDigits = 20 + random.nextInt(21);
                        for (int i = 0; i < numDigits; i++) {
                            TextView tv = new TextView(MainActivity.this);
                            int n = 1 + random.nextInt(9);
                            tv.setText(String.valueOf(n));

                            float size = 16 + random.nextFloat() * 24;
                            tv.setTextSize(size);
                            tv.setTypeface(Typeface.DEFAULT_BOLD);

                            int alpha = 100 + random.nextInt(156);
                            tv.setTextColor(Color.argb(alpha,
                                    Color.red(getColor(R.color.text_primary)),
                                    Color.green(getColor(R.color.text_primary)),
                                    Color.blue(getColor(R.color.text_primary))
                            ));

                            tv.setX(centerX);
                            tv.setY(centerY);
                            overlay.addView(tv);

                            double angle = random.nextDouble() * 2 * Math.PI;
                            float distance = 300 + random.nextFloat() * 300;
                            float endX = centerX + (float) (Math.cos(angle) * distance);
                            float endY = centerY + (float) (Math.sin(angle) * distance);
                            long duration = 1000 + random.nextInt(1000);

                            tv.animate()
                                    .x(endX)
                                    .y(endY)
                                    .setDuration(duration)
                                    .withEndAction(() -> overlay.removeView(tv))
                                    .start();
                        }
                    }

                    overlay.postDelayed(this, 500);
                }
            };

            overlay.post(fireworkRunnable);

            // Clic pour retourner au menu principal
            overlay.setOnClickListener(v -> {
                fireworkRunning.set(false); // stop le feu d'artifice
                root.removeView(congrats);
                root.removeView(overlay);

                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }






        private void resetGame() {
            SudokuGenerator generator = new SudokuGenerator();
            solution = generator.generateSolution();
            puzzle = generator.generatePuzzle(solution, 35);
            score = 0;
            seconds = 0;
            updateScore();
            statusText.setText("Place un chiffre !");
            buildGrid();
            applyTheme();
            startTimer();
        }

        private int dp(int value) {
            float d = getResources().getDisplayMetrics().density;
            return Math.round(value * d);
        }

        static class CellTag {
            int r, c;
            boolean fixed; // true = cellule pré-remplie (non éditable)

            CellTag(int r, int c, boolean fixed) { this.r = r; this.c = c; this.fixed = fixed; }
        }

        static class NumberDragShadowBuilder extends View.DragShadowBuilder {
            private final TextView shadowView;

            public NumberDragShadowBuilder(View view) {
                super(view);
                TextView original = (TextView) view;
                shadowView = new TextView(view.getContext());
                shadowView.setText(original.getText());
                shadowView.setTextSize(28f);
                shadowView.setTypeface(Typeface.DEFAULT_BOLD);
                shadowView.setTextColor(Color.BLACK);
                shadowView.setBackgroundResource(R.drawable.bg_drag_shadow);
                int pad = Math.round(12 * view.getResources().getDisplayMetrics().density);
                shadowView.setPadding(pad, pad, pad, pad);
            }

            @Override
            public void onProvideShadowMetrics(Point size, Point touch) {
                shadowView.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                int width = shadowView.getMeasuredWidth();
                int height = shadowView.getMeasuredHeight();
                size.set(width, height);

                // Décalage : le chiffre est au-dessus du doigt
                int offsetY = height + dp(20);
                touch.set(width / 2, offsetY);
            }

            @Override
            public void onDrawShadow(android.graphics.Canvas canvas) {
                shadowView.layout(0, 0, canvas.getWidth(), canvas.getHeight());
                shadowView.draw(canvas);
            }

            private int dp(int value) {
                return Math.round(value * shadowView.getResources().getDisplayMetrics().density);
            }
        }

        private void loadRecord(String difficulty) {
            var prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE);
            bestScore = prefs.getInt("bestScore_" + difficulty, 0);
            bestTime = prefs.getInt("bestTime_" + difficulty, Integer.MAX_VALUE);
        }

        private void saveRecord(String difficulty) {
            var prefs = getSharedPreferences("SudokuPrefs", MODE_PRIVATE);
            var editor = prefs.edit();
            editor.putInt("bestScore_" + difficulty, bestScore);
            editor.putInt("bestTime_" + difficulty, bestTime);
            editor.apply();
        }

        private void updateRecordText() {
            if (bestTime == Integer.MAX_VALUE) {
                recordText.setText("Record: --");
            } else {
                int mins = bestTime / 60;
                int secs = bestTime % 60;
                recordText.setText(String.format("Record: %d pts en %02d:%02d", bestScore, mins, secs));
            }
        }
        private void applyTheme() {
            int bgPrimary = getColor(R.color.bg_primary);
            int bgGrid = getColor(R.color.bg_grid);
            int bgCell = getColor(R.color.bg_cell_empty);
            int bgPalette = getColor(R.color.bg_palette);
            int textPrimary = getColor(R.color.text_primary);
            int textPalette = getColor(R.color.text_palette);
            int textRecord = getColor(R.color.text_record);

            // Fond général
            grid.setBackgroundColor(bgGrid);
            statusText.setTextColor(textPrimary);
            scoreText.setTextColor(textPrimary);
            timerText.setTextColor(textPrimary);
            recordText.setTextColor(textRecord);

            // Cases de la grille
            for (int i = 0; i < grid.getChildCount(); i++) {
                View child = grid.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    tv.setBackgroundColor(bgCell);
                    tv.setTextColor(textPrimary);
                }
            }

            // Palette
            for (int i = 0; i < palette.getChildCount(); i++) {
                TextView tv = (TextView) palette.getChildAt(i);
                tv.setBackgroundResource(R.drawable.bg_palette_number);
                tv.setTextColor(textPalette);
            }
        }





    }
