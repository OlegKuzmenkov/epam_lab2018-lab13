package com.oleg_kuzmenkov.android.geolocationapplication;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationService extends Service {
    private final String LOG_TAG = "Message";
    private final String BROADCAST_ACTION = "location";
    private final String KEY = "key";
    private final int NOTIFICATION_ID = 101;

    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "OnCreateService");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "OnStartCommandService");

        createLocationCallback();
        createLocationRequest();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            } else {
                stopSelf();
            }
        } else{
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
        // Create PendingIntent
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setColor(Color.GREEN);
        builder.setContentTitle("Geolocation service is running");
        builder.setContentText("Go to GeolocationApplication");
        builder.setContentIntent(resultPendingIntent);
        Notification notification = builder.build();

        //NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //notificationManager.notify(1, notification);
        startForeground(NOTIFICATION_ID, notification);
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "OnDestroyService");
        super.onDestroy();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void createLocationCallback() {
        Log.d(LOG_TAG, "createLocationCallback");
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(LOG_TAG, "OnLocationResult from Service");
                Intent intent = new Intent(BROADCAST_ACTION);
                intent.putExtra(KEY,locationResult);
                sendBroadcast(intent);
            }
        };
    }

    private void createLocationRequest() {
        Log.d(LOG_TAG, "createLocationRequest");
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
}