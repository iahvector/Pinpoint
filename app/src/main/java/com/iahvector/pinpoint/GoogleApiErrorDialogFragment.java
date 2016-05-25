package com.iahvector.pinpoint;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;

import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by islamhassan on 5/25/16.
 */
public class GoogleApiErrorDialogFragment extends AppCompatDialogFragment {
    public final static String PARAM_ERROR_CODE = "error-code";
    public final static String PARAM_REQUEST_CODE = "request-code";
    private GoogleApiErrorDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (GoogleApiErrorDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException(
                    "Activity " + getActivity().getClass().getName() + " must " + "implement " +
                            GoogleApiErrorDialogListener.class.getName());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the error code and retrieve the appropriate dialog
        int errorCode = getArguments().getInt(PARAM_ERROR_CODE);
        int requestCode = getArguments().getInt(PARAM_REQUEST_CODE);
        return GoogleApiAvailability.getInstance().getErrorDialog(
                this.getActivity(), errorCode, requestCode);

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        listener.onDismissed();
    }

    interface GoogleApiErrorDialogListener {
        void onDismissed();
    }
}
