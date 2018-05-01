package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

class Geofencing implements ResultCallback<Status> {
    private static final String TAG = "Geofencing";
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000;
    private static final float GEOFENCE_RADIUS = 50;
    private final Context context;
    private final GoogleApiClient mClient;
    private PendingIntent geofencePendingIntent;
    private List<Geofence> geofencesList;

    public Geofencing(Context context, GoogleApiClient mClient) {
        this.context = context;
        this.mClient = mClient;
        geofencePendingIntent = null;
        geofencesList = new ArrayList<>();
    }

    public void updateGeofencesList(PlaceBuffer places) {
        geofencesList = new ArrayList<>();
        if (places == null || places.getCount() == 0) return;
        for (Place place : places) {
            String placeID = place.getId();
            double latitude = place.getLatLng().latitude;
            double longitude = place.getLatLng().longitude;
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeID)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            geofencesList.add(geofence);
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofencesList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    public void registerAllGeofences() {
        if (mClient == null || !mClient.isConnected() || geofencesList == null || geofencesList.size() == 0) {
            return;
        }
        try {
            LocationServices.GeofencingApi.addGeofences(mClient, getGeofencingRequest(), getGeofencePendingIntent())
                    .setResultCallback(this);
        } catch (SecurityException e) {
            Log.e(TAG, "registerAllGeofences: " + e.getMessage());
        }
    }

    public void unRegisterAllGeofences() {
        if (mClient == null || !mClient.isConnected()) {
            return;
        }
        try {
            LocationServices.GeofencingApi.removeGeofences(mClient, getGeofencePendingIntent())
                    .setResultCallback(this);
        } catch (SecurityException e) {
            Log.e(TAG, "registerAllGeofences: " + e.getMessage());
        }
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.e(TAG, String.format("Error adding/removing geofence: %s",
                status.getStatus().toString()));
    }
}
