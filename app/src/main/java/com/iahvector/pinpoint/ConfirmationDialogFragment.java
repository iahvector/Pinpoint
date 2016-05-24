package com.iahvector.pinpoint;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AppCompatDialogFragment;

/**
 * Created by islamhassan on 5/25/16.
 */
public class ConfirmationDialogFragment extends AppCompatDialogFragment {
    public final static String PARAM_ACTION = "param-action";
    public final static String PARAM_TITLE = "param-title";
    public final static String PARAM_MESSAGE = "param-message";

    private ConfirmationDialogListener listener;

    private int action;
    private String title;
    private String message;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (ConfirmationDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    "Activity " + getActivity().getClass().getName() + " must " + "implement " +
                            ConfirmationDialogListener.class.getName());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        action = -1;
        title = null;
        message = null;

        Bundle args = getArguments();
        if (args != null) {
            action = args.getInt(PARAM_ACTION, -1);
            title = args.getString(PARAM_TITLE, null);
            message = args.getString(PARAM_MESSAGE, null);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title);
        builder.setMessage(message);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onConfirmed(action);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onDenied(action);
            }
        });

        builder.setCancelable(false);

        return builder.create();
    }

    interface ConfirmationDialogListener {
        void onConfirmed(int action);

        void onDenied(int action);
    }
}
