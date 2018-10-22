package com.example.willi.appmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private final static int LOCATION_PERMISSION = 0;
    private static final double RADIUS_OF_EARTH_KM = 6371;
    private static final double ARRIBADERLAT = 4.792509;
    private static final double ARRIBADERLONG = -73.909356;
    private static final double ABAJOIZQLAT = 4.548875;
    private static final double ABAJOIZQLONG = -74.271749;

    private static int voy;

    private Marker oldmark;
    private Marker newmark;
    private Marker lastmark;

    private EditText mAddress;
    private TextView distance;


    //UNA SOLA VEZ
    private FusedLocationProviderClient mFusedLocationClient;
    //ACTUALIZACION PERIODICA
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private List<String> listalocations;
    private JSONObject jso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mAddress = findViewById(R.id.mAddress);
        distance = findViewById(R.id.distancia);
        voy = 0;

        mLocationRequest = createLocationRequest();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        requestPermission(this, Manifest.permission.ACCESS_FINE_LOCATION,
                "Se necesita acceder a la ubicacion", LOCATION_PERMISSION);

        LocationSettingsRequest.Builder builder = new
                LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates(); //Todas las condiciones para recibir localizaciones
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                        try {// Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MapActivity.this,
                                    LOCATION_PERMISSION);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. No way to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });

        mLocationCallback = new LocationCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                Log.i("LOCATION", "Location update in the callback: " + location);
                if (location != null && mMap != null) {
                    if (voy == 0) {
                        LatLng user = new LatLng(location.getLatitude(), location.getLongitude());
                        oldmark = mMap.addMarker(new MarkerOptions().position(user).title("Usted").snippet("                       ").icon(BitmapDescriptorFactory.fromResource(R.drawable.userpin32)));
                        newmark = oldmark;
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(19));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(user));
                        voy++;

                    } else {
                        LatLng user = new LatLng(location.getLatitude(), location.getLongitude());
                        oldmark.remove();
                        newmark = mMap.addMarker(new MarkerOptions().position(user).title("Usted").snippet("                       ").icon(BitmapDescriptorFactory.fromResource(R.drawable.userpin32)));
                        oldmark = newmark;

                    }
                    if (lastmark!=null)
                    {
                        String s = getString(R.string.distancia);
                        distance.setText(s + " " + String.valueOf(distancepoint(newmark.getPosition().latitude,
                                newmark.getPosition().longitude, lastmark.getPosition().latitude, lastmark.getPosition().longitude)) + " Km");


                        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="+newmark.getPosition().latitude+","+newmark.getPosition().longitude+"&destination="+lastmark.getPosition().latitude+","+lastmark.getPosition().longitude+ "&key=" + "AIzaSyCD3_5vFwy-QFyFsomsi-WMxUUUcW5TtQQ";
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Log.i("LOCATION", response);
                                    jso = new JSONObject(response);
                                    trazarRuta(jso);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
                        queue.add(stringRequest);
                    }
                }
            }
        };
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = getString(R.string.Ir);
                if (mAddress.getText().toString().equals(s)) {
                    mAddress.setText("");
                }
            }
        });

        mAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            Geocoder mGeocoder = new Geocoder(getBaseContext());

            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                //mAddress.setText("");

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String addressString = mAddress.getText().toString();
                    if (!addressString.isEmpty()) {
                        try {
                            List<Address> addresses = mGeocoder.getFromLocationName(addressString, 2, ABAJOIZQLAT, ABAJOIZQLONG, ARRIBADERLAT, ARRIBADERLONG);

                            if (addresses != null && !addresses.isEmpty()) {

                                Address addressResult = addresses.get(0);
                                LatLng position = new LatLng(addressResult.getLatitude(), addressResult.getLongitude());
                                if (mMap != null) {
                                    MarkerOptions myMarkerOptions = new MarkerOptions();
                                    myMarkerOptions.position(position);
                                    myMarkerOptions.title("Dirección Encontrada");
                                    myMarkerOptions.snippet("                       ");
                                    myMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.navpin32));
                                    lastmark = mMap.addMarker(myMarkerOptions);
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14));
                                    String s = getString(R.string.distancia);
                                    distance.setText(s + " " + String.valueOf(distancepoint(newmark.getPosition().latitude,
                                            newmark.getPosition().longitude, position.latitude, position.longitude)) + " Km");



                                }
                            } else {
                                Toast.makeText(MapActivity.this, "Dirección no encontrada", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(MapActivity.this, "La dirección esta vacía", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });
    }

    private void trazarRuta(JSONObject jso) {

        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {
            jRoutes = jso.getJSONArray("routes");
            for (int i=0; i<jRoutes.length();i++){

                jLegs = ((JSONObject)(jRoutes.get(i))).getJSONArray("legs");

                for (int j=0; j<jLegs.length();j++){

                    jSteps = ((JSONObject)jLegs.get(j)).getJSONArray("steps");

                    for (int k = 0; k<jSteps.length();k++){


                        String polyline = ""+((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                        Log.i("end",""+polyline);
                        List<LatLng> list = PolyUtil.decode(polyline);
                        mMap.addPolyline(new PolylineOptions().addAll(list).color(Color.BLUE).width(10));



                    }



                }



            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public double distancepoint(double lat1, double long1, double lat2, double long2) {
        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(long1 - long2);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double result = RADIUS_OF_EARTH_KM * c;
        return Math.round(result * 100.0) / 100.0;
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); //tasa de refresco en milisegundos
        mLocationRequest.setFastestInterval(5000); //máxima tasa de refresco
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private void requestPermission(Activity context, String permission, String explanation, int requestId) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                Toast.makeText(context, explanation, Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(context, new String[]{permission}, requestId);
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOCATION_PERMISSION: {
                if (resultCode == RESULT_OK) {
                    startLocationUpdates(); //Se encendió la localización!!!
                } else {
                    Toast.makeText(this,
                            "Sin acceso a localización, hardware deshabilitado!",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }






    /*public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSION: {
                //LocationView();
                break;
            }
        }
    }*/


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

        Date currentTime = Calendar.getInstance().getTime();

        if (currentTime.getHours() >= 18 || currentTime.getHours() <= 6) {
            mMap.setMapStyle(MapStyleOptions
                    .loadRawResourceStyle(this, R.raw.noche));
        } else {
            mMap.setMapStyle(MapStyleOptions
                    .loadRawResourceStyle(this, R.raw.dia));
        }


        // Add a marker in Sydney and move the camera
        //LatLng bogota2 = new LatLng(4.397908 , -74.076066);
        //mMap.addMarker(new MarkerOptions().position(bogota).title("Marcador en Plaza de Bolívar").snippet("test").alpha(1f).icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder)));
        //mMap.addMarker(new MarkerOptions().position(bogota2).title("Marcador en Plaza de Bolívar").snippet("test").alpha(1f).icon(BitmapDescriptorFactory.fromResource(R.drawable.userpin)));
        //mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(bogota));

        mMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng Tequendama = new LatLng(4.613079, -74.070752);
        LatLng Ibis = new LatLng(4.614641, -74.069025);
        LatLng Marriott = new LatLng(4.659429, -74.108398);
        LatLng hilton = new LatLng(4.655761, -74.055403);
        LatLng Atton = new LatLng(4.685638, -74.056165);
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));

        mMap.addMarker(new MarkerOptions().position(Tequendama).title("Tequendama").snippet("300000    4.6  ").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder32)));
        mMap.addMarker(new MarkerOptions().position(Ibis).title("Ibis").snippet("300000    4.6  ").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder32)));
        mMap.addMarker(new MarkerOptions().position(Marriott).title("Marriott").snippet("300000    4.6  ").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder32)));
        mMap.addMarker(new MarkerOptions().position(Atton).title("Atton").snippet("300000    4.6  ").icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder32)));


    }
}
