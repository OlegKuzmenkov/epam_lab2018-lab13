package com.oleg_kuzmenkov.android.geolocationapplication;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String LOG_TAG = "Message";
    private final String BROADCAST_ACTION = "location";
    private final String KEY = "key";

    private GoogleMap mGoogleMap;;
    private Marker mCurrLocationMarker;
    private MapFragment mMapFragment;
    private Button mStartServiceButton;
    private Button mStopServiceButton;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create dynamic MapFragment
        mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, mMapFragment);
        fragmentTransaction.commit();
        mMapFragment.getMapAsync(this);

        mStartServiceButton = findViewById(R.id.start_service_button);
        mStartServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission() == true) {
                    // start service
                    Intent intent = new Intent(getApplicationContext(), LocationService.class);
                    startService(intent);
                } else{
                    startRequestForPermission();
                }

            }
        });

        mStopServiceButton = findViewById(R.id.stop_service_button);
        mStopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LocationService.class);
                // stop service
                stopService(intent);
            }
        });

        //create and register BroadcastReceiver
        createBroadcastReceiver();
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(mBroadcastReceiver, intFilt);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        Log.d(LOG_TAG, "OnMapReady");
        // mGoogleMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "OnDestroyActivity");
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onRequestPermissionsResult ( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode) {
            case 1:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(LOG_TAG, "Permission Granted");
                    // start service
                    Intent intent = new Intent(getApplicationContext(), LocationService.class);
                    startService(intent);
                } else {
                    Toast.makeText(this, "Call Permission Not Granted", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                break;
        }
    }

    /**
     * Check permission
     */
    private boolean checkPermission(){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //location Permission already granted
                return true;
            } else {
                return false;
            }
        } else {
            //location Permission already granted
            return true;
        }
    }

    /**
     * Request permission
     */
    private void startRequestForPermission () {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOG_TAG, "OnReceive from BroadcastReceiver");
                LocationResult currentLocation = intent.getParcelableExtra(KEY);
                setLocationResult(currentLocation);
            }
        };
    }

    /**
     * Set result location on the GoogleMap
     */
    private void setLocationResult(LocationResult locationResult){
        Log.d(LOG_TAG, "OnLocationResult");
        List<Location> locationList;
        LatLng prevLatLng = null;

        if(locationResult != null) {
            locationList = locationResult.getLocations();
        } else{
            return;
        }

        if (locationList.size() > 0) {
            //the last location in the list is the newest
            Location currentLocation = locationList.get(locationList.size() - 1);
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            if (mCurrLocationMarker != null) {
                prevLatLng = new LatLng(mCurrLocationMarker.getPosition().latitude,mCurrLocationMarker.getPosition().longitude);
                mCurrLocationMarker.remove();
            }
            //place current location marker
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currentLatLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

            //place current location circle
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(currentLatLng);
            circleOptions.radius(0.5d);
            circleOptions.fillColor(Color.BLUE);
            mGoogleMap.addCircle(circleOptions);

            //connect previous and current position by line
            if(prevLatLng != null) {
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.add(currentLatLng, prevLatLng);
                polylineOptions.width(25);
                polylineOptions.color(Color.BLUE);
                mGoogleMap.addPolyline(polylineOptions);
            }

            //move map camera
            if (prevLatLng == null) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13));
            } else {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
            }
        }
    }
}
