package edu.orangecoastcollege.cs273.caffeinefinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class CaffeineListActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final int FINE_LOCATION_REQUEST_CODE = 101;
    private DBHelper db;
    private List<CaffeineLocation> mAllLocationsList;
    private ListView locationsListView;
    private LocationListAdapter locationListAdapter;
    private GoogleMap mMap;
    /// Member variable to access the Google Play services (LOCATION)
    private GoogleApiClient mGoogleApiClient;
    // Member variable to store location requests (how often to update)
    private LocationRequest mLocationRequest;
    // Member variable to store current location
    private Location myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caffeine_list);

        deleteDatabase(DBHelper.DATABASE_NAME);
        db = new DBHelper(this);
        db.importLocationsFromCSV("locations.csv");

        mAllLocationsList = db.getAllCaffeineLocations();
        locationsListView = (ListView) findViewById(R.id.locationsListView);
        locationListAdapter = new LocationListAdapter(this, R.layout.location_list_item, mAllLocationsList);
        locationsListView.setAdapter(locationListAdapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.caffeineMapFragment);
        mapFragment.getMapAsync(this);

        // check to see apiClient is on
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // Define the interval for updates;
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(1000);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return; // delete all
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE);
        }
        myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient); // ALT ENTER to build permission
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this); // THIS becuase we implemented location listenter
        handleNewLocation(myLocation);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("Caffeine Finder", "Suspended connection from Google Play Services.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("Caffeine Finder", "Failed connection to Google Play Services.");

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location newLocation){
        // Clear the map first
        mMap.clear();
        // Let's change the vale of myLocation to newLocation
        myLocation = newLocation;
        // Add special marker (blue) for "my" location
        //MBCC Building Lat/Lng (MBCC 135)  33.671028, -117.911305
        LatLng myCoordinate = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(myCoordinate)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker)));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(myCoordinate).zoom(14.0f).build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(cameraUpdate);

        // Add normal markers for all caffeine locations
        for (CaffeineLocation caffeineLocation : mAllLocationsList) {
            LatLng coordinate = new LatLng(caffeineLocation.getLatitude(), caffeineLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(coordinate).title(caffeineLocation.getName()));
        }
    }
    public void findClosestCaffeine(View view){
        double minDistance = Double.MAX_VALUE;
        CaffeineLocation closestCaffeineLocation = null;
        for (CaffeineLocation caffeineLocation : mAllLocationsList){
            Location location = new Location("");
            location.setLatitude(caffeineLocation.getLatitude());
            location.setLongitude(caffeineLocation.getLongitude());
            double distance = myLocation.distanceTo(location);
            if (distance < minDistance) {
                minDistance = distance;
                closestCaffeineLocation = caffeineLocation;
            }            
        }
        // Launch new intent to the details activity
        //Intent detailsIntent = new Intent(this, CaffeineDetailsActivity.class);
        //detailsIntent.putExtra("ClosestCaffeineLocation", closestCaffeineLocation);
        //startActivity(detailsIntent);

    }

}
