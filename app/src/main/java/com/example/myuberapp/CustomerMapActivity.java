package com.example.myuberapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final int MAX_RADIUS = 5;
//
    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LatLng pickupLocation, destinationLatLng;
    private GeoFire.CompletionListener geoFireListener;
    private GeoQuery geoQuery;
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverValueEventListener;
    //    view
    private Button btnLogOut, btnRequest, btnSetting;
    private SupportMapFragment mapFragment;
    private Marker pickUpMarker;
    private TextView tvDriverName, tvDriverPhone, tvDriverCar;
    private ImageView imageDriverProfile;
    private LinearLayout linearDriverInfo;
    private RadioGroup radioGroupDriverService;
    //
    private String requestService;
    private boolean isRequest = false;
    private boolean isDriverFound = false;
    private int radius = 1;
    private String destination;
    private String driverFoundID = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        connectView();
//        geoFireListener cần thêm vào setLocation để không bị error

    }

    private void connectView() {
//
        btnLogOut = findViewById(R.id.btn_log_out);
        btnRequest = findViewById(R.id.btn_request);
        btnSetting = findViewById(R.id.btn_setting);
        tvDriverCar = findViewById(R.id.tv_driver_car);
        tvDriverName = findViewById(R.id.tv_driver_name);
        tvDriverPhone = findViewById(R.id.tv_driver_phone);
        linearDriverInfo = findViewById(R.id.linear_driver_info);
        imageDriverProfile = findViewById(R.id.image_driver_profile);
        radioGroupDriverService = findViewById(R.id.radio_group_services);
//
        btnLogOut.setOnClickListener(this);
        btnRequest.setOnClickListener(this);
        btnSetting.setOnClickListener(this);
//
        destinationLatLng = new LatLng(0,0);

//
        geoFireListener = new GeoFire.CompletionListener(){
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        };
//        callback khi gọi requestLocationUpdate
        mLocationCallback = new LocationCallback(){
            @Override
            //gọi event cập nhật vị trí
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    //lấy tọa độ nhận được
                    lastLocation = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    //di chuyển camera map đến tọa độ định vị được
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    //animation zoom
                    mMap.animateCamera(CameraUpdateFactory.zoomBy(11));
                    //Lấy ID của tài xế
                }
            }
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                Log.e("nhat",""+locationAvailability.toString());
            }
        };
// Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

// Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

// Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
                Log.e("nhat", "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.e("nhat", "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_request:{
                if (isRequest){
                    endRequestRide();
                }
                else {
                    startSendCustomerRequest();
                }
                break;
            }
            case R.id.btn_log_out:{
                logOutCustomer();
                break;
            }
            case R.id.btn_setting:{
                goToSettingScreen();
                break;
            }

        }
    }

    private void goToSettingScreen() {
        Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingActivity.class);
        startActivity(intent);
    }
    private void logOutCustomer() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void startSendCustomerRequest() {
//        check radio button service
        if (!checkRequestService()){
            return;
        }
        isRequest = true;
        String userID = FirebaseAuth.getInstance().getUid();
        //tạo node customer request
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(reference);
        //set tọa độ của khách lên FireBase
        pickupLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        if (userID != null) {
            Log.e("nhat", "userID:" + userID);
            geoFire.setLocation(userID
                    , new GeoLocation(pickupLocation.latitude, pickupLocation.longitude)
                    , geoFireListener);
        }
//        Marker resize
        int height = 150;
        int width = 150;
        BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.location_pin  ,null);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
//        dat marker pickup len
        pickUpMarker = mMap.addMarker(new MarkerOptions()
                .position(pickupLocation)
                .title("Pickup here")
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
        );
        btnRequest.setText("Getting your driver");
//
        getClosestDriver();
    }

    private boolean checkRequestService() {
        int selectedId = radioGroupDriverService.getCheckedRadioButtonId();

        final RadioButton radioButton = findViewById(selectedId);

        if (TextUtils.isEmpty(radioButton.getText())){
            return false;
        }else {
            requestService = radioButton.getText().toString();
            return true;
        }
    }

    private void endRequestRide() {
        //Nếu đang gửi request tìm Driver, bấm lần nữa để hủy
        //dừng query tìm kiếm driver, cũng như tìm location của driver
        isRequest = false;
//      remove dò driver gần nhất
        geoQuery.removeAllListeners();
//      remove  xác định vị trí driver
        driverLocationRef.removeEventListener(driverValueEventListener);
//      remove  lắng nghe sự kiện từ chối của driver
        driverHasEndedRef.removeEventListener(driverHasEndedRefListener);
        btnRequest.setText("Request");

        //set các giá trị về default
        if (driverFoundID != null){
            DatabaseReference driverRef = FirebaseDatabase
                    .getInstance()
                    .getReference()
                    .child("Users")
                    .child("Drivers")
                    .child(driverFoundID);
//            xóa bên trong node Driver, set lại là true, bỏ customerRequest
            driverRef.setValue(true);
//            ID driver tìm đc về null
            driverFoundID = null;

//            hide View Driver Info
            linearDriverInfo.setVisibility(View.GONE);
            tvDriverPhone.setText("");
            tvDriverName.setText("");
            tvDriverCar.setText("");
            imageDriverProfile.setImageResource(R.drawable.ic_user_default);
        }
        isDriverFound = false;
        radius = 1;

        //remove location đã gửi request đi
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(reference);
        geoFire.removeLocation(userID, geoFireListener);
        //        Remove Marker
        if (pickUpMarker != null){
            pickUpMarker.remove();
        }
    }
//
    private DatabaseReference driverHasEndedRef;
    private ValueEventListener driverHasEndedRefListener;
    private void getHasEndedRequest(){
        driverHasEndedRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID)
                .child("customerRequest");
        driverHasEndedRefListener = driverHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                } else {
                    endRequestRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
//
    private void getClosestDriver() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driverAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
//        remove listener cũ
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
//            driver has been found
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
//                đã tìm đc driver gần bạn
//                nếu trước đó chưa tìm đc driver nào thì chọn driver này
                if (!isDriverFound && isRequest){
                    DatabaseReference customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    customerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                if (isDriverFound) {
                                    return;
                                }
                                if (driverMap.get("service").equals(requestService)) {
//                                    SUCCESS
//                    chọn driver này-> chuyển isDriverFound = true
                                    isDriverFound = true;
//                    lấy ID driver
                                    driverFoundID = dataSnapshot.getKey();
//          Tìm đến node driver có ID đc truyền vào
                                    DatabaseReference driverRef = FirebaseDatabase
                                            .getInstance()
                                            .getReference()
                                            .child("Users")
                                            .child("Drivers")
                                            .child(driverFoundID)
                                            .child("customerRequest");
//                    thêm ID của mình (khách hàng) vào bên trong node của driver đó
                                    String customerID = FirebaseAuth
                                            .getInstance()
                                            .getCurrentUser()
                                            .getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerRideID", customerID);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLatLng", destinationLatLng.longitude);
                                    Log.e("nhat", "onKeyEntered customerID ID: " + customerID);
                                    Log.e("nhat", "onKeyEntered destination: " + destination);
//                    làm mới node
                                    driverRef.updateChildren(map);

//                  hiển thị vị trí driver vừa tìm đc
                                    getDriverLocation();
//                  Hiển thị thông tin driver
                                    getDriverInfo();
//                  Kiểm tra nếu Driver từ chối request thì sẽ xóa request
                                    getHasEndedRequest();
                                    btnRequest.setText("Found your Driver");


                                }

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    Log.e("nhat","onKeyEntered driver ID: "+key);
//
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!isDriverFound){
//                    if (radius < MAX_RADIUS){
//                        radius++;
//                        getClosestDriver();
//                    }
                    radius++;
                    Log.e("nhat",""+radius);
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
//
    private void getDriverInfo() {
        linearDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference driverInfoRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID);

        driverInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        tvDriverName.setText(map.get("name").toString());
                        Log.e("nhat","driver name: "+map.get("name").toString());
                    }
                    if (map.get("phone") != null){
                        tvDriverPhone.setText(map.get("phone").toString());
                        Log.e("nhat","driver phone: "+map.get("phone").toString());
                    }
                    if (map.get("car") != null){
                        tvDriverCar.setText(map.get("car").toString());
                        Log.e("nhat","driver car: "+map.get("car").toString());
                    }
                    if (map.get("profileImageUrl") != null){
                        String url = map.get("profileImageUrl").toString();
                        Glide.with(CustomerMapActivity.this)
                                .load(url)
                                .error(R.drawable.ic_user_default)
                                .into(imageDriverProfile);
                        Log.e("nhat","driver car: "+map.get("car").toString());
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
//
    private void getDriverLocation() {
//        tìm đến thông tin location của Driver
        driverLocationRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("driverWorking")
                .child(driverFoundID)
                .child("l");

        driverValueEventListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0, locationLng = 0;
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null){
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(driverLatLng.latitude);
                    loc1.setLongitude(driverLatLng.longitude);
//
                    Location loc2 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    float distance = loc1.distanceTo(loc2);
                    if (distance<100){
//                        thoong bao tai xe sap toi roi
                        btnRequest.setText("Your Driver arrived");
                    }
                    else{
                        btnRequest.setText("Distance your Driver: "+String.valueOf(distance));
                    }
//                    resize
                    int height = 50;
                    int width = 50;
                    BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.car,null);
                    Bitmap b = bitmapdraw.getBitmap();
                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
//                    đặt marker lên map để biết vị trí Driver
                    mDriverMarker = mMap.addMarker(new MarkerOptions()
                            .position(driverLatLng)
                            .title("Your Driver Location")
                            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        //từ phiên bản Android Version 6.0 trở lên sẽ cần người dùng ban quyền
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(this)
                    .setTitle("Grant permission")
                    .setMessage("Please provide permission")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create()
                    .show();
        }
        else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
        }
    }


    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(!checkPermissions()){
//            startLocationPermissionRequest();
//        }
//        checkLocationPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        removeLocation();
    }
//  func remove cần đặt lại chỗ khác
    private void removeLocation() {
//        String userID = FirebaseAuth.getInstance().getUid();
//        if (userID != null){
//            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("customerRequest");
//            GeoFire geoFire = new GeoFire(reference);
//            geoFire.removeLocation(userID, geoFireListener);
//        }

    }

    final int LOCATION_REQUEST_CODE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:{
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "Please grant permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


}
