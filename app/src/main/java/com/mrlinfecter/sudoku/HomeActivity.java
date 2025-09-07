package com.mrlinfecter.sudoku;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        LinearLayout btnEasy = findViewById(R.id.btnEasy);
        LinearLayout btnNormal = findViewById(R.id.btnNormal);
        LinearLayout btnHard = findViewById(R.id.btnHard);

        TextView recordEasy = findViewById(R.id.recordEasy);
        TextView recordNormal = findViewById(R.id.recordNormal);
        TextView recordHard = findViewById(R.id.recordHard);

        btnEasy.setOnClickListener(v -> startGame("easy"));
        btnNormal.setOnClickListener(v -> startGame("normal"));
        btnHard.setOnClickListener(v -> startGame("hard"));
    }

    private void startGame(String difficulty) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
    }
}
