package com.example.warrantyvault;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import java.security.MessageDigest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User picks one of their saved products -> the app finds the nearest service centers
 * of that product's brand (Google Places API "Text Search (New)") and shows them on the map.
 */
public class ServiceCentresActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String PLACES_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final double SEARCH_RADIUS_METERS = 30000.0;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView tvCentreName, tvCentreAddress, tvStatus;
    private Button btnGetDirections;
    private Spinner spinnerProducts;
    private ProgressBar progressSearch;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<String> productLabels = new ArrayList<>();
    private final List<String> productBrands = new ArrayList<>();   // search keyword per spinner row (index - 1)
    private int pendingProductIndex = -1;                           // waiting for location permission
    private Marker selectedMarker;
    private LatLng selectedLatLng;

    /** One service centre result. */
    private static class Centre {
        String name, address, phone;
        Boolean openNow;
        double lat, lng;
        float distanceMeters;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_centres);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.mapRoot));
        BottomNavHelper.setup(this, BottomNavHelper.SERVICE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvCentreName = findViewById(R.id.tvCentreName);
        tvCentreAddress = findViewById(R.id.tvCentreAddress);
        tvStatus = findViewById(R.id.tvStatus);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        spinnerProducts = findViewById(R.id.spinnerProducts);
        progressSearch = findViewById(R.id.progressSearch);

        UiUtils.applyPressScaleAnimation(btnGetDirections);
        btnGetDirections.setOnClickListener(v -> openDirections());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadProductsIntoSpinner();
    }

    // ------------------------------------------------------------------
    // Product selector
    // ------------------------------------------------------------------
    private void loadProductsIntoSpinner() {
        SharedPreferences prefs = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);

        executorService.execute(() -> {
            List<String> labels = new ArrayList<>();
            List<String> brands = new ArrayList<>();
            labels.add("Select a product…");

            if (userId != -1) {
                DatabaseHelper db = new DatabaseHelper(this);
                Cursor c = null;
                try {
                    c = db.getProductsByUser(userId);
                    if (c != null && c.moveToFirst()) {
                        int nameCol = c.getColumnIndex(DatabaseHelper.COLUMN_PRODUCT_NAME);
                        int brandCol = c.getColumnIndex(DatabaseHelper.COLUMN_PRODUCT_BRAND);
                        do {
                            String name = nameCol != -1 ? c.getString(nameCol) : "";
                            String brand = brandCol != -1 ? c.getString(brandCol) : "";
                            if (name == null) name = "";
                            if (brand == null) brand = "";
                            labels.add(brand.isEmpty() ? name : name + " (" + brand + ")");
                            brands.add(brand.isEmpty() ? name : brand);
                        } while (c.moveToNext());
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) c.close();
                }
            }

            mainHandler.post(() -> {
                productLabels.clear();
                productLabels.addAll(labels);
                productBrands.clear();
                productBrands.addAll(brands);

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, productLabels);
                spinnerProducts.setAdapter(adapter);

                if (productBrands.isEmpty()) {
                    tvStatus.setText("No products yet. Add a product first.");
                }

                spinnerProducts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position > 0) {
                            onProductSelected(position - 1);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            });
        });
    }

    private void onProductSelected(int brandIndex) {
        if (mMap == null) return;
        if (!hasLocationPermission()) {
            pendingProductIndex = brandIndex;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        findCentresFor(productBrands.get(brandIndex));
    }

    // ------------------------------------------------------------------
    // Map + location
    // ------------------------------------------------------------------
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(22.9734, 78.6569), 4.5f)); // India

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Centre) {
                showCentre((Centre) tag, marker);
            }
            return false;
        });

        if (hasLocationPermission()) {
            enableMyLocationLayer();
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void enableMyLocationLayer() {
        if (mMap != null && hasLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (hasLocationPermission()) {
                enableMyLocationLayer();
                if (pendingProductIndex >= 0 && pendingProductIndex < productBrands.size()) {
                    findCentresFor(productBrands.get(pendingProductIndex));
                }
            } else {
                tvStatus.setText("Location permission is needed to find nearby centers");
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
            pendingProductIndex = -1;
        }
    }

    // ------------------------------------------------------------------
    // Search flow: current location -> Places API -> markers
    // ------------------------------------------------------------------
    @SuppressWarnings("MissingPermission")
    private void findCentresFor(String brand) {
        setSearching(true, "Getting your location…");

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        searchPlaces(brand, location);
                    } else {
                        // fall back to last known location
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, last -> {
                            if (last != null) {
                                searchPlaces(brand, last);
                            } else {
                                setSearching(false, "Couldn't get your location. Turn on GPS and try again.");
                            }
                        });
                    }
                })
                .addOnFailureListener(this, e -> setSearching(false, "Couldn't get your location: " + e.getMessage()));
    }

    private void searchPlaces(String brand, Location userLoc) {
        setSearching(true, "Searching " + brand + " service centers…");

        final String apiKey = getApiKey();
        if (apiKey.isEmpty() || apiKey.contains("YOUR_")) {
            setSearching(false, "Google Maps API key missing. Add MAPS_API_KEY in local.properties.");
            return;
        }

        executorService.execute(() -> {
            List<Centre> centres = new ArrayList<>();
            String error = null;
            HttpURLConnection conn = null;
            try {
                JSONObject center = new JSONObject()
                        .put("latitude", userLoc.getLatitude())
                        .put("longitude", userLoc.getLongitude());
                JSONObject circle = new JSONObject()
                        .put("center", center)
                        .put("radius", SEARCH_RADIUS_METERS);
                JSONObject body = new JSONObject()
                        .put("textQuery", brand + " authorized service centre")
                        .put("locationBias", new JSONObject().put("circle", circle))
                        .put("rankPreference", "DISTANCE")
                        .put("maxResultCount", 10);

                conn = (HttpURLConnection) new URL(PLACES_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-Goog-Api-Key", apiKey);
                conn.setRequestProperty("X-Goog-FieldMask",
                        "places.displayName,places.formattedAddress,places.location,"
                                + "places.nationalPhoneNumber,places.regularOpeningHours.openNow");
                // The Android-restricted key needs the app identity headers
                conn.setRequestProperty("X-Android-Package", getPackageName());
                String sha1 = getSigningSha1();
                if (!sha1.isEmpty()) {
                    conn.setRequestProperty("X-Android-Cert", sha1);
                }

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String response = readAll(is);

                if (code >= 200 && code < 300) {
                    JSONArray places = new JSONObject(response).optJSONArray("places");
                    if (places != null) {
                        for (int i = 0; i < places.length(); i++) {
                            JSONObject p = places.getJSONObject(i);
                            JSONObject loc = p.optJSONObject("location");
                            if (loc == null) continue;

                            Centre c = new Centre();
                            JSONObject dn = p.optJSONObject("displayName");
                            c.name = dn != null ? dn.optString("text", brand + " Service Center") : brand + " Service Center";
                            c.address = p.optString("formattedAddress", "");
                            c.phone = p.optString("nationalPhoneNumber", "");
                            JSONObject hours = p.optJSONObject("regularOpeningHours");
                            if (hours != null && hours.has("openNow")) c.openNow = hours.optBoolean("openNow");
                            c.lat = loc.getDouble("latitude");
                            c.lng = loc.getDouble("longitude");

                            float[] res = new float[1];
                            Location.distanceBetween(userLoc.getLatitude(), userLoc.getLongitude(), c.lat, c.lng, res);
                            c.distanceMeters = res[0];
                            centres.add(c);
                        }
                    }
                    Collections.sort(centres, (a, b) -> Float.compare(a.distanceMeters, b.distanceMeters));
                } else {
                    error = "Places API error " + code + ". Check that Places API (New) is enabled for your key.";
                }
            } catch (Exception e) {
                error = "Search failed: " + e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }

            final String finalError = error;
            mainHandler.post(() -> {
                if (finalError != null) {
                    setSearching(false, finalError);
                    return;
                }
                showCentresOnMap(brand, userLoc, centres);
            });
        });
    }

    private void showCentresOnMap(String brand, Location userLoc, List<Centre> centres) {
        mMap.clear();
        selectedMarker = null;
        selectedLatLng = null;

        if (centres.isEmpty()) {
            setSearching(false, "No " + brand + " service centers found near you");
            tvCentreName.setText("No centers found");
            tvCentreAddress.setText("Try another product or move closer to a city.");
            LatLng me = new LatLng(userLoc.getLatitude(), userLoc.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 11f));
            return;
        }

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        bounds.include(new LatLng(userLoc.getLatitude(), userLoc.getLongitude()));

        Marker nearest = null;
        for (int i = 0; i < centres.size(); i++) {
            Centre c = centres.get(i);
            LatLng pos = new LatLng(c.lat, c.lng);
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(c.name)
                    .snippet(formatDistance(c.distanceMeters))
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            i == 0 ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED)));
            if (m != null) {
                m.setTag(c);
                if (i == 0) nearest = m;
            }
            bounds.include(pos);
        }

        setSearching(false, centres.size() + " " + brand + " service centers found (nearest in green)");

        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 180));
        } catch (Exception e) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(userLoc.getLatitude(), userLoc.getLongitude()), 12f));
        }

        if (nearest != null) {
            showCentre(centres.get(0), nearest);
            nearest.showInfoWindow();
        }
    }

    // ------------------------------------------------------------------
    // Detail card + directions
    // ------------------------------------------------------------------
    private void showCentre(Centre c, Marker marker) {
        selectedMarker = marker;
        selectedLatLng = new LatLng(c.lat, c.lng);

        tvCentreName.setText(c.name);

        StringBuilder sb = new StringBuilder();
        sb.append(formatDistance(c.distanceMeters));
        if (c.openNow != null) sb.append(" • ").append(c.openNow ? "Open now" : "Closed now");
        if (!c.address.isEmpty()) sb.append("\n").append(c.address);
        if (!c.phone.isEmpty()) sb.append("\n").append(c.phone);
        tvCentreAddress.setText(sb.toString());

        btnGetDirections.setText("Get Directions");
    }

    private void openDirections() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Select a product, then a service center first", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri navUri = Uri.parse(String.format(Locale.US, "google.navigation:q=%f,%f",
                selectedLatLng.latitude, selectedLatLng.longitude));
        Intent intent = new Intent(Intent.ACTION_VIEW, navUri);
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Uri geo = Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f",
                    selectedLatLng.latitude, selectedLatLng.longitude,
                    selectedLatLng.latitude, selectedLatLng.longitude));
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, geo));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "No maps app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private void setSearching(boolean searching, String status) {
        progressSearch.setVisibility(searching ? View.VISIBLE : View.GONE);
        tvStatus.setText(status);
    }

    private String formatDistance(float meters) {
        if (meters < 1000) return Math.round(meters) + " m away";
        return String.format(Locale.getDefault(), "%.1f km away", meters / 1000f);
    }

    private String getApiKey() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                String key = ai.metaData.getString("com.google.android.geo.API_KEY");
                return key != null ? key : "";
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return "";
    }

    /** SHA-1 of the app signing certificate (hex, no colons) - needed for Android-restricted API keys. */
    @SuppressWarnings("deprecation")
    private String getSigningSha1() {
        try {
            Signature[] sigs;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                sigs = pi.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
                sigs = pi.signatures;
            }
            if (sigs == null || sigs.length == 0) return "";
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(sigs[0].toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String readAll(InputStream is) throws java.io.IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
