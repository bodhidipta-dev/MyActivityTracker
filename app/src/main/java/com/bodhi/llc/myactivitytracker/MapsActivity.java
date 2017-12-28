package com.bodhi.llc.myactivitytracker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        GoogleMap.OnCameraMoveListener,
        View.OnClickListener {

    private GoogleMap mMap;
    public GoogleApiClient mApiClient;
    private TextView updated_state, location_update;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private List<LatLng> lastKnownLocation = new ArrayList<>();
    private boolean shouldTrack = false;
    private LocationRequest mLocationRequest;
    BoringLocationCallback boringLocationCallback;
    private ActivtyColorSpecifier specifiedMode = ActivtyColorSpecifier.OTHERS;

    private enum ActivtyColorSpecifier {
        WALKING(Color.parseColor("#FFE64F4F")),
        RUNNING(Color.parseColor("#FF3C43C2")),
        OTHERS(Color.parseColor("#FF96CF2D"));
        private final int colorSpecified;

        ActivtyColorSpecifier(int colorSpecified) {
            this.colorSpecified = colorSpecified;
        }

        public int getColorSpecified() {
            return colorSpecified;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        updated_state = (TextView) findViewById(R.id.updated_state);
        location_update = (TextView) findViewById(R.id.location_update);
        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.clear_reposition).setOnClickListener(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        boringLocationCallback = new BoringLocationCallback();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        /******************************************************
         * Create the LocationRequest object
         */
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        mApiClient.connect();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                this, R.raw.map_style));

        mMap.setOnCameraMoveListener(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        requestActivityUpdatesButtonHandler();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
//        Intent intent = new Intent(this, ActivityRecognizedService.class);
//        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(MapsActivity.this);
//        Task task = activityRecognitionClient.requestActivityUpdates(1000L, pendingIntent);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()), 22));
                    // mMap.addMarker(new MarkerOptions().position(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude())));
                    createMarkerForGlobal(mMap, task.getResult().getLatitude(), task.getResult().getLongitude());

                }
                lastKnownLocation.clear();
                lastKnownLocation.add(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()));
            }
        });


    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(Constants.KEY_DETECTED_ACTIVITIES)) {
            Log.i("ActivityRecogition", "**** onSharedPreferenceChanged ");
            updateDetectedActivitiesList();
        }
    }

    /**
     * Processes the list of freshly detected activities. Asks the adapter to update its list of
     * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
     * activities.
     */
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = Utils.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(MapsActivity.this)
                        .getString(Constants.KEY_DETECTED_ACTIVITIES, ""));

        for (DetectedActivity activity : detectedActivities) {
            Log.i("ActivityRecogition", "******* ON ACTIVITY " + Utils.getActivityString(
                    getApplicationContext(),
                    activity.getType()) + " " + activity.getConfidence() + "%"
            );
            if (Utils.getActivityString(
                    getApplicationContext(),
                    activity.getType()).equalsIgnoreCase("Walking") && activity.getConfidence() < 75) {
                specifiedMode = ActivtyColorSpecifier.WALKING;
                updated_state.setText("Walking");

            } else if (Utils.getActivityString(
                    getApplicationContext(),
                    activity.getType()).equalsIgnoreCase("Running")) {
                specifiedMode = ActivtyColorSpecifier.RUNNING;
                updated_state.setText("Running");
            } else {
                specifiedMode = ActivtyColorSpecifier.OTHERS;
                updated_state.setText("Others");
            }

        }
    }

    @Override
    protected void onDestroy() {
        removeActivityUpdatesButtonHandler();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        mApiClient.disconnect();
        super.onDestroy();
    }

    /**
     * Registers for activity recognition updates using
     * {@link ActivityRecognitionClient#requestActivityUpdates(long, PendingIntent)}.
     * Registers success and failure callbacks.
     */
    public void requestActivityUpdatesButtonHandler() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent());

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(MapsActivity.this,
                        "Updating",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("ActivityRecogition", "exceptiopn " + e.getMessage());
                Toast.makeText(MapsActivity.this,
                        "failed",
                        Toast.LENGTH_SHORT)
                        .show();

            }
        });
    }


    /**
     * Removes activity recognition updates using
     * {@link ActivityRecognitionClient#removeActivityUpdates(PendingIntent)}. Registers success and
     * failure callbacks.
     */
    public void removeActivityUpdatesButtonHandler() {
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(MapsActivity.this,
                        "Removed",
                        Toast.LENGTH_SHORT)
                        .show();
                // Reset the display.
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("ActivityRecogition", "Failed to enable activity recognition.");
                Toast.makeText(MapsActivity.this, "Failed to enable activity recognition.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, ActivityRecognizedService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start: {
                findViewById(R.id.start).setVisibility(View.GONE);
                findViewById(R.id.stop).setVisibility(View.VISIBLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Toast.makeText(MapsActivity.this, "Please allow Location permission from settings -> Permission ", Toast.LENGTH_SHORT).show();
                    return;
                }
                mMap.clear();
                lastKnownLocation.clear();
                LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()), 22));
                            //mMap.addMarker(new MarkerOptions().position(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude())));
                            createMarkerForGlobal(mMap, task.getResult().getLatitude(), task.getResult().getLongitude());

                        }
                        lastKnownLocation.add(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()));
                    }
                });
                LocationServices.getFusedLocationProviderClient(MapsActivity.this).requestLocationUpdates(mLocationRequest, boringLocationCallback, getMainLooper());

            }
            break;
            case R.id.stop: {
                updated_state.setText("");
                findViewById(R.id.start).setVisibility(View.VISIBLE);
                findViewById(R.id.stop).setVisibility(View.GONE);
                LocationServices.getFusedLocationProviderClient(MapsActivity.this).removeLocationUpdates(boringLocationCallback);
            }
            break;
            case R.id.clear_reposition: {
                findViewById(R.id.start).setVisibility(View.VISIBLE);
                findViewById(R.id.stop).setVisibility(View.GONE);
                LocationServices.getFusedLocationProviderClient(MapsActivity.this).removeLocationUpdates(boringLocationCallback);
                mMap.clear();
                LocationServices.getFusedLocationProviderClient(MapsActivity.this).getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()), 22));
                            createMarkerForGlobal(mMap, task.getResult().getLatitude(), task.getResult().getLongitude());


                        }
                        lastKnownLocation.add(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()));
                    }
                });

            }
            break;
        }
    }

    class BoringLocationCallback extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.w("ActivityRecogition", "****** GT LOcation ." + locationResult.getLastLocation().getLatitude() + "/" + locationResult.getLastLocation().getLongitude());
            if (mMap != null
                    && lastKnownLocation.size() > 0
                    && !lastKnownLocation.get(lastKnownLocation.size() - 1).equals(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLatitude()))) {

                mMap.addPolyline(
                        new PolylineOptions()
                                .add(lastKnownLocation.get(lastKnownLocation.size() - 1))
                                .add(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()))
                                .color(specifiedMode.getColorSpecified())
                                .clickable(true)
                                .jointType(JointType.ROUND)
                                .width(10)
                );
                // createMarkerForGlobal(mMap,locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                lastKnownLocation.add(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));

                mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
                    @Override
                    public void onPolylineClick(Polyline polyline) {
                        if (polyline.getColor() == ActivtyColorSpecifier.WALKING.getColorSpecified()) {
                            Toast.makeText(MapsActivity.this, "Activity detected on ::WALKING", Toast.LENGTH_SHORT).show();
                        } else if (polyline.getColor() == ActivtyColorSpecifier.RUNNING.getColorSpecified()) {
                            Toast.makeText(MapsActivity.this, "Activity detected on ::RUNNING", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MapsActivity.this, "Activity detected on ::OTHERS", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude())));
            }
        }
    }

    @Override
    public void onCameraMove() {
//        if (mMap != null
//                && lastKnownLocation.size() > 0
//                && !lastKnownLocation.get(lastKnownLocation.size() - 1).equals(mMap.getCameraPosition().target)) {
//
//            mMap.addPolyline(
//                    new PolylineOptions()
//                            .add(lastKnownLocation.get(lastKnownLocation.size() - 1))
//                            .add(new LatLng(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude))
//                            .color(specifiedMode.getColorSpecified())
//                            .clickable(true)
//                            .jointType(JointType.ROUND)
//                            .width(10)
//            );
//            // createMarkerForGlobal(mMap,mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);
//            lastKnownLocation.add(mMap.getCameraPosition().target);
//
//            mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
//                @Override
//                public void onPolylineClick(Polyline polyline) {
//                    if (polyline.getColor() == ActivtyColorSpecifier.WALKING.getColorSpecified()) {
//                        Toast.makeText(MapsActivity.this, "Activity detected on ::WALKING", Toast.LENGTH_SHORT).show();
//                    } else if (polyline.getColor() == ActivtyColorSpecifier.RUNNING.getColorSpecified()) {
//                        Toast.makeText(MapsActivity.this, "Activity detected on ::RUNNING", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(MapsActivity.this, "Activity detected on ::OTHERS", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(mMap.getCameraPosition().target));
//        }
    }

    protected void createMarkerForGlobal(GoogleMap map, double latitude, double longitude) {
        map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView()))
                .anchor(0.5f, 0.5f));

    }

    private Bitmap getMarkerBitmapFromView() {

        Bitmap returnedBitmap = null;
        try {
            View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.location_point, null);
            customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
            customMarkerView.buildDrawingCache();
            returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(returnedBitmap);
            customMarkerView.draw(canvas);
            customMarkerView.destroyDrawingCache();
            return returnedBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return returnedBitmap;
        }

    }
}
