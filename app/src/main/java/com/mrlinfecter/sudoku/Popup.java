package com.mrlinfecter.sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Popup {

    public interface PopupCallback {
        void onResult(boolean yes);
    }

    public static void show(Context context, PopupCallback callback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View popupView = inflater.inflate(R.layout.custom_popup, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(popupView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView message = popupView.findViewById(R.id.popupMessage);
        Button btnYes = popupView.findViewById(R.id.btnYes);
        Button btnNo = popupView.findViewById(R.id.btnNo);

        message.setText("Êtes-vous sûr de vouloir activer l'aide ? (l'aide divise le score final par deux)");

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onResult(true);
        });

        btnNo.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onResult(false);
        });

        dialog.show();
    }
}
