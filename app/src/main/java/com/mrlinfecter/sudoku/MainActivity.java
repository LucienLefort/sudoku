package com.mrlinfecter.sudoku;

import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
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

public class MainActivity extends AppCompatActivity {

    private int[][] puzzle;
    private int[][] solution;

    private GridLayout grid;
    private LinearLayout palette;
    private TextView statusText, scoreText, timerText;

    private int score = 0;
    private CountDownTimer timer;
    private int seconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button testWinButton = findViewById(R.id.testWinButton);
        testWinButton.setOnClickListener(v -> {
            checkWinAnimation(); // Force l'animation de victoire
        });

        grid = findViewById(R.id.sudokuGrid);
        palette = findViewById(R.id.palette);
        statusText = findViewById(R.id.statusText);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);

        SudokuGenerator generator = new SudokuGenerator();
        solution = generator.generateSolution();
        puzzle = generator.generatePuzzle(solution, 35);

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
            startTimer();
        });
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
                tv.setBackgroundColor(Color.WHITE);
                tv.setTag(new CellTag(r, c));

                int value = puzzle[r][c];
                if (value != 0) {
                    tv.setText(String.valueOf(value));
                    tv.setTextColor(Color.BLACK);
                    tv.setEnabled(false);
                } else {
                    tv.setText("");
                    tv.setTextColor(Color.DKGRAY);
                }

                grid.addView(tv);
            }
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
            int pad = dp(10);
            tv.setPadding(pad, pad, pad, pad);
            tv.setTextColor(Color.WHITE);
            tv.setBackgroundColor(Color.parseColor("#3F51B5"));

            final int number = n;
            tv.setLongClickable(true);
            tv.setOnLongClickListener(v -> {
                v.startDragAndDrop(ClipData.newPlainText("number", String.valueOf(number)),
                        new NumberDragShadowBuilder(v), null, 0);
                return true;
            });

            palette.addView(tv);
        }

        grid.setOnDragListener(globalGridDragListener);
    }

    private TextView lastHoverCell = null;

    private final View.OnDragListener globalGridDragListener = (v, event) -> {
        float shadowOffsetY = dp(-50); // même offset que dans NumberDragShadowBuilder
        float shadowOffsetX = dp(0);  // tu peux ajouter un offset horizontal si nécessaire

        float x = event.getX() + shadowOffsetX;
        float y = event.getY() + shadowOffsetY;

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                TextView hoverCell = findCellUnder(x, y);
                if (hoverCell != null && hoverCell != lastHoverCell && hoverCell.isEnabled()) {
                    if (lastHoverCell != null) lastHoverCell.setBackgroundColor(Color.WHITE);
                    hoverCell.setBackgroundColor(Color.LTGRAY);
                    lastHoverCell = hoverCell;
                }
                break;

            case DragEvent.ACTION_DRAG_EXITED:
                if (lastHoverCell != null) {
                    lastHoverCell.setBackgroundColor(Color.WHITE);
                    lastHoverCell = null;
                }
                break;

            case DragEvent.ACTION_DROP:
                if (lastHoverCell != null) lastHoverCell.setBackgroundColor(Color.WHITE);

                if (event.getClipData() == null || event.getClipData().getItemCount() == 0) return false;
                CharSequence clip = event.getClipData().getItemAt(0).getText();
                if (clip == null) return false;

                int number;
                try {
                    number = Integer.parseInt(clip.toString());
                } catch (NumberFormatException e) { return false; }

                TextView targetCell = findCellUnder(x, y);
                if (targetCell != null && targetCell.isEnabled()) {
                    CellTag tag = (CellTag) targetCell.getTag();
                    int correct = solution[tag.r][tag.c];

                    if (number == correct) {
                        targetCell.setText(String.valueOf(number));
                        targetCell.setEnabled(false);
                        targetCell.setBackgroundColor(Color.parseColor("#E8F5E9"));
                        score += 10;
                        statusText.setText("✔ Correct !");
                        updateScore();
                        checkWin();
                    } else {
                        targetCell.setBackgroundColor(Color.parseColor("#FFCDD2"));
                        targetCell.postDelayed(() -> targetCell.setBackgroundColor(Color.WHITE), 350);
                        score = Math.max(0, score - 5);
                        Toast.makeText(this, "❌ Mauvais chiffre", Toast.LENGTH_SHORT).show();
                        updateScore();
                    }
                }
                lastHoverCell = null;
                break;

            case DragEvent.ACTION_DRAG_ENDED:
                if (lastHoverCell != null) {
                    lastHoverCell.setBackgroundColor(Color.WHITE);
                    lastHoverCell = null;
                }
                break;
        }
        return true;
    };

    // Fonction utilitaire pour trouver la cellule sous des coordonnées x, y
    private TextView findCellUnder(float x, float y) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            TextView cell = (TextView) grid.getChildAt(i);
            if (x >= cell.getLeft() && x <= cell.getRight()
                    && y >= cell.getTop() && y <= cell.getBottom()) {
                return cell;
            }
        }
        return null;
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

        statusText.setText("🎉 Sudoku terminé !");
        Toast.makeText(this, "Bravo ! Score: " + score, Toast.LENGTH_LONG).show();
        if (timer != null) timer.cancel();

        checkWinAnimation();

        //grid.postDelayed(this::resetGame, 2500);
    }

    private void checkWinAnimation() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);

        // Message central
        TextView congrats = new TextView(this);
        congrats.setText("🎉 Félicitations ! 🎉");
        congrats.setTextSize(32f);
        congrats.setTypeface(Typeface.DEFAULT_BOLD);
        congrats.setTextColor(Color.parseColor("#388E3C"));
        congrats.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams msgParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        congrats.setLayoutParams(msgParams);
        root.addView(congrats);

        int screenW = root.getWidth();
        int screenH = root.getHeight();

        for (int i = 0; i < 60; i++) {
            TextView tv = new TextView(this);
            int n = 1 + (int)(Math.random() * 9);
            tv.setText(String.valueOf(n));

            // Taille aléatoire
            float size = 18 + (float)(Math.random() * 14);
            tv.setTextSize(size);
            tv.setTypeface(Typeface.DEFAULT_BOLD);

            // Couleur aléatoire
            tv.setTextColor(Color.rgb(
                    100 + (int)(Math.random()*155),
                    100 + (int)(Math.random()*155),
                    100 + (int)(Math.random()*155)
            ));

            // Position aléatoire en X
            float startX = (float)(Math.random() * (screenW - 50));
            tv.setX(startX);
            tv.setY(-50f - (float)(Math.random() * 200)); // départ au-dessus de l'écran

            root.addView(tv);

            // Durée, rotation et décalage final
            long duration = 2000 + (long)(Math.random() * 1500);
            float rotation = (float)(Math.random()*720 - 360);
            float endX = startX + (float)(Math.random()*200 - 100); // décalage horizontal
            float endY = screenH - 50f;

            tv.animate()
                    .translationX(endX)
                    .translationY(endY)
                    .rotation(rotation)
                    .setDuration(duration)
                    .withEndAction(() -> {
                        // petit rebond
                        tv.animate()
                                .translationY(endY - dp(20))
                                .setDuration(150)
                                .withEndAction(() -> tv.animate()
                                        .translationY(endY)
                                        .setDuration(150)
                                        .withEndAction(() -> root.removeView(tv))
                                        .start()
                                ).start();
                    })
                    .start();
        }

        // Retirer le message central et reset le jeu
        root.postDelayed(() -> {
            root.removeView(congrats);
            resetGame();
        }, 4000);
    }




    private void resetGame() {
        SudokuGenerator generator = new SudokuGenerator();
        solution = generator.generateSolution();
        puzzle = generator.generatePuzzle(solution, 35);
        score = 0;
        updateScore();
        statusText.setText("Place un chiffre !");
        buildGrid();
        startTimer();
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(value * d);
    }

    static class CellTag {
        int r, c;
        CellTag(int r, int c) { this.r = r; this.c = c; }
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
            shadowView.setBackgroundColor(Color.WHITE);
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

}
