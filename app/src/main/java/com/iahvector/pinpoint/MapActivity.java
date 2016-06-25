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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapActivity
        extends AppCompatActivity
        implements OnMapReadyCallback,
                   GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener,
                   ConfirmationDialogFragment.ConfirmationDialogListener,
                   GoogleApiErrorDialogFragment.GoogleApiErrorDialogListener,
                   LocationListener,
                   GoogleMap.OnMapLongClickListener,
                   GoogleMap.OnCameraChangeListener,
                   GoogleMap.OnMapClickListener {

    private final static int RESOLVE_GOOGLE_API_ERROR_REQUEST_CODE = 0;
    private final static int LOCATION_PERMISSION_REQUEST_CODE = 10;
    private final static int ENABLE_MY_LOCATION_REQUEST_CODE = 11;
    private final static int ANIMATE_TO_MY_LOCATION_REQUEST_CODE = 12;
    private final static String CONFIRMATION_DIALOG_TAG = "confirmation";
    private final static String GOOGLE_API_ERROR_DIALOG_TAG = "google-api-error";
    private final static String PARAM_RESOLVING_GOOGLE_API_ERROR = "resolving-google-api-error";

    private boolean isResolvingGoogleApiError;
    private boolean isLocationUpdatesRequested;
    private boolean isDisplayingConfirmationDialog;
    private ArrayList<Integer> pendingLocationActions;

    private GoogleApiClient googleApiClient;
    private GoogleMap map;
    private Location lastLocation;
    private Marker marker;
    private CameraPosition cameraPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        isLocationUpdatesRequested = false;
        isResolvingGoogleApiError = savedInstanceState != null
                && savedInstanceState.getBoolean(PARAM_RESOLVING_GOOGLE_API_ERROR, false);

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton myLocation = (FloatingActionButton) findViewById(R.id.myLocation_fab);
        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestLocationAction(ANIMATE_TO_MY_LOCATION_REQUEST_CODE, true);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_activity_menu, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager
                        .PERMISSION_GRANTED) {
                    requestLocationUpdates();
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

    private void requestLocationUpdates() {
        if (!isLocationUpdatesRequested && googleApiClient.isConnected()
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission
                .ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission
                        .ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);

            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                                                                     locationRequest,
                                                                     this);
            isLocationUpdatesRequested = true;
        }
    }

    private void stopLocationUpdates() {
        if (googleApiClient.isConnected() && isLocationUpdatesRequested) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            isLocationUpdatesRequested = false;
        }
    }

    private void animateToCurrentLocation() {
        if (map != null) {
            //noinspection MissingPermission
            lastLocation = LocationServices.FusedLocationApi.getLastLocation
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
        map.setPadding(0, getSupportActionBar().getHeight(), 0, 0);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setOnMapLongClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnCameraChangeListener(this);
        requestLocationAction(ENABLE_MY_LOCATION_REQUEST_CODE, true);

        if (cameraPosition == null) {
            requestLocationAction(ANIMATE_TO_MY_LOCATION_REQUEST_CODE, true);
        } else {
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (cameraPosition == null) {
            requestLocationAction(ANIMATE_TO_MY_LOCATION_REQUEST_CODE, true);
        }
        requestLocationUpdates();
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
                            this, RESOLVE_GOOGLE_API_ERROR_REQUEST_CODE);
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

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (marker == null) {
            marker = map.addMarker(new MarkerOptions().position(latLng));
        } else {
            marker.setPosition(latLng);
        }
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        this.cameraPosition = cameraPosition;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }
}
