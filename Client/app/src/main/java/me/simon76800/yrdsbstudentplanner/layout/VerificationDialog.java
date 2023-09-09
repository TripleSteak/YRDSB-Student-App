package me.simon76800.yrdsbstudentplanner.layout;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import me.simon76800.yrdsbstudentplanner.MainActivity;
import me.simon76800.yrdsbstudentplanner.R;

/**
 * UI component that prompts the user for a verification code.
 * Used during the client authentication process.
 */
public class VerificationDialog extends DialogFragment {
    public static String message;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.verification_dialog, null));

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.show();

        final LinearLayout layout = dialog.findViewById(R.id.verification_layout);
        ViewGroup.LayoutParams layoutParams = layout.getLayoutParams();
        layoutParams.width = (int) (MainActivity.PX_WIDTH * 4 / 5);
        layout.setLayoutParams(layoutParams);

        final ImageView greyX = dialog.findViewById(R.id.grey_x);
        ViewGroup.LayoutParams xParams = greyX.getLayoutParams();
        xParams.width = (int) (MainActivity.PX_WIDTH * 1 / 6);
        xParams.height = (int) (MainActivity.PX_WIDTH * 1 / 6);
        greyX.setLayoutParams(xParams);

        final TextView textView = dialog.findViewById(R.id.verification_text);
        textView.setText(message);

        final Button okButton = dialog.findViewById(R.id.verification_button);
        okButton.setText(R.string.ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        return dialog;
    }
}
