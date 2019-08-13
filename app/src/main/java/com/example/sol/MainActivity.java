package com.example.sol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private TextView tvSunSet;
    private TextView tvSunRise;
    private TextView tvMoonPhase;
    private ImageView ivMoonPhase;
    private TextView tvLocation;
    private static final int REQUEST_LOCATION = 1;
    double longitude;
    double latitude;

    protected LocationManager locationManager;
    protected boolean gps_enabled,network_enabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSunSet = (TextView) findViewById(R.id.tvSunset);
        tvSunSet.setText("");
        tvSunRise = (TextView) findViewById(R.id.tvSunRise);
        tvSunRise.setText("");
        tvMoonPhase = (TextView) findViewById(R.id.tvMoonPhase);
        tvMoonPhase.setText("");
        tvLocation = (TextView) findViewById(R.id.tvLocation);
        tvLocation.setText("");
        ivMoonPhase = (ImageView) findViewById(R.id.ivMoonPhase);
        ivMoonPhase.setVisibility(View.INVISIBLE);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, false);
//        Location location = locationManager.getLastKnownLocation(bestProvider);

        Location location = locationManager.getLastKnownLocation(bestProvider);//(locationManager.NETWORK_PROVIDER);
//        locationManager.requestLocationUpdates(bestProvider,0,0,this);

        LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
        latitude = userLocation.latitude;
        longitude = userLocation.longitude;

        if (location == null) {
            Toast.makeText(MainActivity.this, "NO GPS", Toast.LENGTH_LONG).show();
            tvMoonPhase.setText("NO GPS");
        }

        if(location != null) {
            onLocationChanged(location);

            OkHttpClient client = new OkHttpClient();

            android.text.format.DateFormat df = new android.text.format.DateFormat();//"YYYY-MM-DDThh:mm:ss");
            String now = df.format("yyyy-MM-ddTkk:mm:ss", new java.util.Date()).toString();

            String url = String.format("https://api.darksky.net/forecast/" +
                    "6a16aa916f1fe73085452ff8cfd5f8bb/" +
                    "%f,%f,%s" +
                    "?exclude=minutely,hourly,currently,alerts,flags", latitude, longitude, now);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final String myResponse = response.body().string();

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject reader = new JSONObject(myResponse);
                                    JSONObject daily = reader.getJSONObject("daily");
                                    JSONArray data = daily.getJSONArray("data");
                                    JSONObject dailyData = data.getJSONObject(0);
                                    long lngSet = dailyData.getLong("sunsetTime") * 1000;
                                    long lngRise = dailyData.getLong("sunriseTime") * 1000;
                                    double fMoonPhase = dailyData.getDouble("moonPhase");
                                    String moonPhase;


                                    if (fMoonPhase == 0) {
                                        moonPhase = "New Moon";
                                        ivMoonPhase.setImageResource(R.drawable.new_moon);
                                    } else if (fMoonPhase < .24) {
                                        moonPhase = "Waxing Crescent";
                                        ivMoonPhase.setImageResource(R.drawable.waxing_crescent);
                                    } else if (fMoonPhase <= 0.26) {
                                        moonPhase = "First Quarter";
                                        ivMoonPhase.setImageResource(R.drawable.first_quarter);
                                    } else if (fMoonPhase <= 0.48) {
                                        moonPhase = "Waxing Gibbous";
                                        ivMoonPhase.setImageResource(R.drawable.waxing_gibbous);
                                    } else if (fMoonPhase <= 0.52) {
                                        moonPhase = "Full Moon";
                                        ivMoonPhase.setImageResource(R.drawable.full_moon);
                                    } else if (fMoonPhase <= 0.74) {
                                        moonPhase = "Waning Gibbous";
                                        ivMoonPhase.setImageResource(R.drawable.waning_gibbous);
                                    } else if (fMoonPhase <= 0.76) {
                                        moonPhase = "Last Quarter";
                                        ivMoonPhase.setImageResource(R.drawable.third_quarter);
                                    } else {
                                        moonPhase = "Waning Crescent";
                                        ivMoonPhase.setImageResource(R.drawable.waning_crescent);
                                    }


                                    ivMoonPhase.setVisibility(View.VISIBLE);
                                    tvMoonPhase.setText(moonPhase);

                                    Date sSet = new Date(lngSet);
                                    Date sRise = new Date(lngRise);
                                    String strSet = new SimpleDateFormat("hh:mma").format(sSet);
                                    String strRise = new SimpleDateFormat("hh:mma").format(sRise);
//                                String result = String.format("SunRise: %s\nSunSet: %s", strRise, strSet);
                                    tvSunSet.setText(strSet);
                                    tvSunRise.setText(strRise);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
//                            tvOut.setText(myResponse);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            longitude = location.getLongitude();
            latitude = location.getLatitude();

//        String output = String.format("Longitude: %f\nLatitude: %f",longitude, latitude);
//        tvOut.setText(output);
        }
        catch (Exception e){
            Log.e("Error: ", e.getMessage());
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


}
