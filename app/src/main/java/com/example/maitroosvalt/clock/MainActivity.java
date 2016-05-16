package com.example.maitroosvalt.clock;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private final static String TAG = "MainActivity";
    private NotificationManager mNotificationManager;
    private BroadcastReceiver mBroadcastReceiver;

    private GoogleMap mGoogleMap;
    private Menu mOptionsMenu;

    private LocationManager locationManager;

    private String provider;

    private ArrayList<LatLng> sinceLastWP = new ArrayList<LatLng>();
    private ArrayList<LatLng> sinceLastReset = new ArrayList<LatLng>();
    private double wpDistance;
    private double stepDistance;
    private double cResetDistance;
    private double totalDistance;


    private int markerCount = 0;
    private Location locationPrevious;

    private Polyline mPolyline;

    private PolylineOptions mPolylineOptions;
    private LatLng lastWP;

    private TextView textViewWPCount;
    private TextView textViewSpeed;
    private TextView textViewCResetDistance;
    private TextView textViewCResetLine;
    private TextView textViewWPDistance;
    private TextView textViewWPLine;
    private TextView textViewTotalDistance;
    private TextView textViewTotalLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();

        // get the location provider (GPS/CEL-towers, WIFI)
        provider = locationManager.getBestProvider(criteria, false);

        //Log.d(TAG, provider);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");

        }

        locationPrevious = locationManager.getLastKnownLocation(provider);

        if (locationPrevious != null) {
            // do something with initial position?
        }


        textViewWPCount = (TextView) findViewById(R.id.textview_wpcount);
        textViewSpeed = (TextView) findViewById(R.id.textview_speed);

        textViewSpeed = (TextView) findViewById(R.id.textview_speed);
        textViewCResetDistance = (TextView)findViewById(R.id.textview_creset_distance);
        textViewCResetLine = (TextView)findViewById(R.id.textview_creset_line);
        textViewWPDistance = (TextView)findViewById(R.id.textview_wp_distance);
        textViewWPLine = (TextView)findViewById(R.id.textview_wp_line);
        textViewTotalDistance = (TextView)findViewById(R.id.textview_total_distance);
        textViewTotalLine = (TextView)findViewById(R.id.textview_total_line);


        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "notification-broadcast-addwaypoint":
                        createWayPoint();
                        return;
                    default:
                        return;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();

        /*intentFilter.addAction("notification-broadcast-addwaypoint");
        intentFilter.addAction("notification-broadcast-resettripmeter");*/
        intentFilter.addAction("notification-broadcast");
        intentFilter.addAction("notification-broadcast-addwaypoint");
        registerReceiver(mBroadcastReceiver, intentFilter);

        buttonNotificationCustomLayout();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_mylocation:
                item.setChecked(!item.isChecked());
                updateMyLocation();
                return true;
            case R.id.menu_trackposition:
                item.setChecked(!item.isChecked());
                updateTrackPosition();
                return true;
            case R.id.menu_keepmapcentered:
                item.setChecked(!item.isChecked());
                return true;
            case R.id.menu_map_type_hybrid:
            case R.id.menu_map_type_none:
            case R.id.menu_map_type_normal:
            case R.id.menu_map_type_satellite:
            case R.id.menu_map_type_terrain:
                item.setChecked(true);
                updateMapType();
                return true;

            case R.id.menu_map_zoom_10:
            case R.id.menu_map_zoom_15:
            case R.id.menu_map_zoom_20:
            case R.id.menu_map_zoom_in:
            case R.id.menu_map_zoom_out:
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomLevel(item.getItemId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }


    private void updateMapZoomLevel(int itemId) {
        if (!checkReady()) {
            return;
        }

        switch (itemId) {
            case R.id.menu_map_zoom_10:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
                break;
            case R.id.menu_map_zoom_15:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                break;
            case R.id.menu_map_zoom_20:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                break;
            case R.id.menu_map_zoom_in:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomIn());
                break;
            case R.id.menu_map_zoom_out:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomOut());
                break;
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomFitTrack();
                break;
        }
    }

    private void updateMapZoomFitTrack() {
        if (mPolyline == null) {
            return;
        }

        List<LatLng> points = mPolyline.getPoints();

        if (points.size() <= 1) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

    }

    private void updateTrackPosition() {
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            mPolylineOptions = new PolylineOptions().width(5).color(Color.RED);
            mPolyline = mGoogleMap.addPolyline(mPolylineOptions);
        }

       /* if (lastWP != null) {
         wpPolyLine
        }*/


    }

    private void updateMapType() {
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_map_type_normal).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_hybrid).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_none).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_satellite).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_terrain).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }

    }

    private boolean checkReady() {
        if (mGoogleMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateMyLocation() {
        if (mOptionsMenu.findItem(R.id.menu_mylocation).isChecked()) {
            mGoogleMap.setMyLocationEnabled(true);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(false);
    }

    public void buttonAddWayPointClicked(View view){
        createWayPoint();
    }

    public void buttonCResetClicked(View view){
        cResetDistance = 0;
        updateCresetLine((double) 0);
        textViewCResetDistance.setText(String.valueOf(cResetDistance));
        sinceLastReset.clear();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        //mGoogleMap.setMyLocationEnabled(false);

        //LatLng latLngITK = new LatLng(59.3954789, 24.6621282);
        //mGoogleMap.addMarker(new MarkerOptions().position(latLngITK).title("ITK"));
        //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngITK, 17));

        // set zoom level to 15 - street
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(17));

        // if there was initial location received, move map to it
        if (locationPrevious != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())));
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());

        if (mGoogleMap==null) return;

        if (mOptionsMenu.findItem(R.id.menu_keepmapcentered).isChecked() || locationPrevious == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            List<LatLng> points = mPolyline.getPoints();
            points.add(newPoint);
            mPolyline.setPoints(points);
            sinceLastReset.add(newPoint);
          //  double totalLineDistance = SphericalUtil.computeLength(points);
            double totalLineDistance = latlngListDistance(points);

            TextView TextViewTotalLineDistance = (TextView) findViewById(R.id.textview_total_line);
            TextViewTotalLineDistance.setText(String.valueOf(Math.round(totalLineDistance)));


            if (lastWP != null) {
                sinceLastWP.add(newPoint);
                wpDistance +=stepDistance;
                double totalLineDistanceWP = latlngListDistance(sinceLastWP);
                textViewWPLine.setText(String.valueOf(Math.round(totalLineDistanceWP)));
            }

            stepDistance=locationPrevious.distanceTo(location);
            totalDistance+=stepDistance;
            cResetDistance +=stepDistance;
            updateTextViews();


            updateCresetLine(latlngListDistance(sinceLastReset));
        }




        locationPrevious = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    protected void onResume(){
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.requestLocationUpdates(provider, 500, 1, this);
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.removeUpdates(this);
        }
    }

    public void createWayPoint() {
        if (locationPrevious==null){
            return;
        }

        wpDistance = 0;
        markerCount++;

        sinceLastWP.clear();

        LatLng point = new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude());

        lastWP = point;

        mGoogleMap.addMarker(new MarkerOptions().position(point).title(Integer.toString(markerCount)));
        textViewWPCount.setText(Integer.toString(markerCount));
    }

    public float directDistanceBetweenPoints(LatLng firstPoint, LatLng newPoint) {
        Location start = new Location(LocationManager.GPS_PROVIDER);
        start.setLatitude(newPoint.latitude);
        start.setLongitude(newPoint.longitude);

        Location end = new Location(LocationManager.GPS_PROVIDER);
        end.setLatitude(firstPoint.latitude);
        end.setLongitude(firstPoint.longitude);

        float distance = start.distanceTo(end);

        return distance;
    }

    public void updateTextViews() {
        textViewCResetDistance.setText(String.valueOf(Math.round(cResetDistance)));
        textViewWPDistance.setText(String.valueOf(Math.round(wpDistance)));
        textViewTotalDistance.setText(String.valueOf(Math.round(totalDistance)));
        setSpeed();
    }

    public double latlngListDistance(List<LatLng> locations) {
        double totalDistance = 0;

        for (int i = 0; i + 1 < locations.size(); i++) {
            totalDistance += directDistanceBetweenPoints(locations.get(i), locations.get(i + 1));
        }

        return totalDistance;
    }

    public void setSpeed() {
        textViewSpeed.setText(String.valueOf(Math.round(stepDistance/(500 * 0.001))));
    }

    public void updateCresetLine(double value) {
        textViewCResetLine.setText(String.valueOf(Math.round(value)));
   }


public void buttonNotificationCustomLayout() {

        // get the view layout
        RemoteViews remoteView = new RemoteViews(
                getPackageName(), R.layout.notify);

        // define intents
        PendingIntent pIntentAddWaypoint = PendingIntent.getBroadcast(
                this,
                0,
                new Intent("notification-broadcast-addwaypoint"),
                0
        );

        // attach events
        remoteView.setOnClickPendingIntent(R.id.buttonAddWayPoint, pIntentAddWaypoint);

        // build notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContent(remoteView)
                        .setSmallIcon(R.drawable.ic_location_on_white_24dp);

        // notify
        mNotificationManager.notify(4, mBuilder.build());


    }
}
