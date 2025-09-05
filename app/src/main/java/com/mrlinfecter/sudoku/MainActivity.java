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

    private final View.OnDragListener globalGridDragListener = (v, event) -> {
        switch (event.getAction()) {
            case DragEvent.ACTION_DROP:
                if (event.getClipData() == null || event.getClipData().getItemCount() == 0) return false;
                CharSequence clip = event.getClipData().getItemAt(0).getText();
                if (clip == null) return false;

                int number;
                try {
                    number = Integer.parseInt(clip.toString());
                } catch (NumberFormatException e) { return false; }

                float x = event.getX();
                float y = event.getY();

                for (int i = 0; i < grid.getChildCount(); i++) {
                    TextView cell = (TextView) grid.getChildAt(i);
                    if (x >= cell.getLeft() && x <= cell.getRight()
                            && y >= cell.getTop() && y <= cell.getBottom()
                            && cell.isEnabled()) {

                        CellTag tag = (CellTag) cell.getTag();
                        int correct = solution[tag.r][tag.c];

                        if (number == correct) {
                            cell.setText(String.valueOf(number));
                            cell.setEnabled(false);
                            score += 10;
                            cell.setBackgroundColor(Color.parseColor("#E8F5E9"));
                            statusText.setText("✔ Correct !");
                            updateScore();
                            checkWin();
                        } else {
                            cell.setBackgroundColor(Color.parseColor("#FFCDD2"));
                            cell.postDelayed(() -> cell.setBackgroundColor(Color.WHITE), 350);
                            score = Math.max(0, score - 5);
                            Toast.makeText(this, "❌ Mauvais chiffre", Toast.LENGTH_SHORT).show();
                            updateScore();
                        }
                        break;
                    }
                }
                return true;
        }
        return true;
    };

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

        startFallingNumbersEffect();

        grid.postDelayed(this::resetGame, 2500);
    }

    private void startFallingNumbersEffect() {
        int gridWidth = grid.getWidth();
        int gridHeight = grid.getHeight();

        for (int i = 0; i < 30; i++) {
            TextView tv = new TextView(this);
            int n = 1 + (int)(Math.random() * 9);
            tv.setText(String.valueOf(n));
            tv.setTextColor(Color.parseColor("#FF5722"));
            tv.setTextSize(18f);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setX((float) (Math.random() * gridWidth));
            tv.setY(-50f);

            grid.addView(tv);

            tv.animate()
                    .translationY(gridHeight + 50f)
                    .setDuration(2000 + (long)(Math.random() * 1000))
                    .withEndAction(() -> grid.removeView(tv))
                    .start();
        }
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
            shadowView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int width = shadowView.getMeasuredWidth();
            int height = shadowView.getMeasuredHeight();
            size.set(width, height);
            touch.set(width / 2, dp(100));
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
