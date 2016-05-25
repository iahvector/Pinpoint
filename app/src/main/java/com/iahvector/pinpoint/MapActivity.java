package com.iahvector.pinpoint;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MapActivity
        extends AppCompatActivity
        implements OnMapReadyCallback,
                   GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener,
                   ConfirmationDialogFragment.ConfirmationDialogListener,
                   GoogleApiErrorDialogFragment.GoogleApiErrorDialogListener {

    private final static int RESOLVE_GOOGLE_API_ERROR_REQUEST_CODE = 0;
    private final static int LOCATION_PERMISSION_REQUEST_CODE = 10;
    private final static int ENABLE_MY_LOCATION_REQUEST_CODE = 11;
    private final static int ANIMATE_TO_MY_LOCATION_REQUEST_CODE = 12;
    private final static String CONFIRMATION_DIALOG_TAG = "confirmation";
    private final static String GOOGLE_API_ERROR_DIALOG_TAG = "google-api-error";
    private final static String PARAM_RESOLVING_GOOGLE_API_ERROR = "resolving-google-api-error";

    private GoogleApiClient googleApiClient;
    private GoogleMap map;

    private boolean isResolvingGoogleApiError;
    private boolean isDisplayingConfirmationDialog;
    private ArrayList<Integer> pendingLocationActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        isResolvingGoogleApiError = savedInstanceState != null
                && savedInstanceState.getBoolean(PARAM_RESOLVING_GOOGLE_API_ERROR, false);
        ;
        isDisplayingConfirmationDialog = false;
        pendingLocationActions = new ArrayList<>();

        // Create an instance of GoogleAPIClient.
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(PARAM_RESOLVING_GOOGLE_API_ERROR, isResolvingGoogleApiError);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager
                        .PERMISSION_GRANTED) {
                    for (int action : pendingLocationActions) {
                        switch (action) {
                            case ENABLE_MY_LOCATION_REQUEST_CODE: {
                                //noinspection MissingPermission
                                map.setMyLocationEnabled(true);
                                break;
                            }
                            case ANIMATE_TO_MY_LOCATION_REQUEST_CODE: {
                                animateToCurrentLocation();
                                break;
                            }
                        }
                    }
                    pendingLocationActions.clear();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESOLVE_GOOGLE_API_ERROR_REQUEST_CODE: {
                isResolvingGoogleApiError = false;

                if (resultCode == RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if (!googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
                        googleApiClient.connect();
                    }
                }

                break;
            }
        }
    }

    private void requestLocationAction(int actionRequestCode, boolean showRationale) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            switch (actionRequestCode) {
                case ENABLE_MY_LOCATION_REQUEST_CODE: {
                    map.setMyLocationEnabled(true);
                    break;
                }
                case ANIMATE_TO_MY_LOCATION_REQUEST_CODE: {
                    animateToCurrentLocation();
                    break;
                }
            }
        } else {
            pendingLocationActions.add(actionRequestCode);
            if (showRationale && ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ConfirmationDialogFragment confirmationDialogFragment =
                        (ConfirmationDialogFragment) getSupportFragmentManager()
                                .findFragmentByTag(CONFIRMATION_DIALOG_TAG);

                if (confirmationDialogFragment == null) {
                    confirmationDialogFragment = new ConfirmationDialogFragment();
                }

                if (!isDisplayingConfirmationDialog) {
                    Bundle b = new Bundle();
                    b.putInt(ConfirmationDialogFragment.PARAM_ACTION, actionRequestCode);
                    b.putString(ConfirmationDialogFragment.PARAM_TITLE, getString(R.string.permissionNeeded));
                    b.putString(ConfirmationDialogFragment.PARAM_MESSAGE, getString(R.string.locationPermissionRationale));

                    confirmationDialogFragment.setArguments(b);
                    confirmationDialogFragment.setCancelable(false);
                    confirmationDialogFragment.show(getSupportFragmentManager(),
                                                    CONFIRMATION_DIALOG_TAG);
                    isDisplayingConfirmationDialog = true;
                }
            } else {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);

            }
        }
    }

    private void animateToCurrentLocation() {
        if (map != null) {
            //noinspection MissingPermission
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation
                    (googleApiClient);
            if (lastLocation != null) {
                CameraUpdate u = CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 18);
                map.animateCamera(u);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        requestLocationAction(ENABLE_MY_LOCATION_REQUEST_CODE, true);
        requestLocationAction(ANIMATE_TO_MY_LOCATION_REQUEST_CODE, true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationAction(ANIMATE_TO_MY_LOCATION_REQUEST_CODE, true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!isResolvingGoogleApiError) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(
                            this,
                            RESOLVE_GOOGLE_API_ERROR_REQUEST_CODE);
                    isResolvingGoogleApiError = true;
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                    googleApiClient.connect();
                }
            } else {
                isResolvingGoogleApiError = true;

                GoogleApiErrorDialogFragment googleApiErrorDialogFragment =
                        (GoogleApiErrorDialogFragment) getSupportFragmentManager()
                                .findFragmentByTag(GOOGLE_API_ERROR_DIALOG_TAG);

                if (googleApiErrorDialogFragment == null) {
                    googleApiErrorDialogFragment = new GoogleApiErrorDialogFragment();
                }

                Bundle b = new Bundle();
                b.putInt(GoogleApiErrorDialogFragment.PARAM_ERROR_CODE,
                         connectionResult.getErrorCode());
                b.putInt(GoogleApiErrorDialogFragment.PARAM_REQUEST_CODE,
                         RESOLVE_GOOGLE_API_ERROR_REQUEST_CODE);

                googleApiErrorDialogFragment.setArguments(b);
                googleApiErrorDialogFragment.show(getSupportFragmentManager(),
                                                  GOOGLE_API_ERROR_DIALOG_TAG);
            }
        }
    }

    @Override
    public void onConfirmed(int action) {
        isDisplayingConfirmationDialog = false;

        requestLocationAction(action, false);
    }

    @Override
    public void onDenied(int action) {
        isDisplayingConfirmationDialog = false;

        Snackbar.make(findViewById(R.id.mapActivityRoot), R.string.cannotDisplayUserLocation,
                      Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDismissed() {
        isResolvingGoogleApiError = false;
    }
}
