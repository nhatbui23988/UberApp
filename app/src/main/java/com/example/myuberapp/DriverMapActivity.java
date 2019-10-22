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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback,
        View.OnClickListener,
        RoutingListener,
        UberConstant {

    private LocationRequest locationRequest;
    private Location lastLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private GeoFire.CompletionListener geoFireListener;
    private DatabaseReference assignCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationListener;
    //View
    private GoogleMap mMap;
    private Marker pickUpMarker;
    private SupportMapFragment mapFragment;
    private Button btnLogOut, btnSetting, btnStatus, btnHistory;
    private TextView tvCustomerName, tvCustomerPhone, tvCustomerDestination, tvCustomerDistance, tvCustomerPrice;
    private ImageView imageCustomerProfile;
    private LinearLayout linearCustomerInfo;
    private Switch workingSwitch;
//
    private String customerID = "", destination = "", driverID = "";
    private boolean isLogOut = false;
    private int status = 0;
    private LatLng destinationLaLng;
    private LatLng pickupLaLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        polylines = new ArrayList<>();
        Intent intent = new Intent(DriverMapActivity.this, OnAppKilled.class);
        startService(intent);
        setUp();
    }

    private void setUp() {
        setUpView();
        setUpDriverConnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationCallback == null)
            setUpDriverConnect();
        if (mMap != null){
            mMap.animateCamera(CameraUpdateFactory.zoomBy(12));
        }
    }

    private void setUpDriverConnect() {
        driverID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();
//        setLocation cần add geoFireListener, có thể dùng new geoFireListener
        geoFireListener = new GeoFire.CompletionListener() {

            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        };

        mLocationCallback = new LocationCallback() {
            @Override
            //gọi event cập nhật vị trí
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    lastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    //di chuyển camera map đến tọa độ định vị được
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    //animation zoom
//                  lấy id customer gửi request nếu có
                    getAssignedCustomer();
                    updateDriverState(location);
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                Log.e("nhat", "" + locationAvailability.toString());
            }
        };
    }

    private void updateDriverState(Location location) {
        //Lấy ID của tài xế
        //tạo node driverAvailable
        DatabaseReference availableRef = FirebaseDatabase
                .getInstance()
                .getReference(NODE_DRIVER_AVAILABLE);
        //tạo node driverWorking
        DatabaseReference workingRef = FirebaseDatabase
                .getInstance()
                .getReference(NODE_DRIVER_WORKING);
        GeoFire geoFireAvailable = new GeoFire(availableRef);
        GeoFire geoFireWorking = new GeoFire(workingRef);
        //set tọa độ của tài xế lên FireBase
        if (driverID != null) {
//                        nếu không có khách -> customerID == null
//                                                  -> rảnh -> available
//                        nếu có khách -> customerID != null
//                                                  -> bận chạy -> working
            switch (customerID) {
                case "": { //->không có khách
//                                rảnh -> remove ID bên working
                    geoFireWorking.removeLocation(driverID, geoFireListener);
//                                ->Thêm ID bên driver available (Rảnh)
                    geoFireAvailable.setLocation(driverID
                            , new GeoLocation(location.getLatitude(), location.getLongitude())
                            , geoFireListener);
                    break;
                }
                default: { //!= null -> có khách
//                                -> bận chạy -> remove ID bên available
                    geoFireAvailable.removeLocation(driverID, geoFireListener);
//                                ->Thêm ID bên driver working (đã bận, đã nhận khách )
                    geoFireWorking.setLocation(driverID
                            , new GeoLocation(location.getLatitude(), location.getLongitude())
                            , geoFireListener);
                    break;
                }
            }
        }
    }

    private void setUpView() {
        workingSwitch = findViewById(R.id.switch_working);
        btnLogOut = findViewById(R.id.btn_log_out);
        btnHistory = findViewById(R.id.btn_history);
        btnSetting = findViewById(R.id.btn_setting);
        btnStatus = findViewById(R.id.btn_status_request);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerPhone = findViewById(R.id.tv_customer_phone);
        tvCustomerDestination = findViewById(R.id.tv_customer_destination);
        tvCustomerDistance = findViewById(R.id.tv_customer_distance);
        tvCustomerPrice = findViewById(R.id.tv_customer_price);
        imageCustomerProfile = findViewById(R.id.image_customer_profile);
        linearCustomerInfo = findViewById(R.id.linear_customer_info);
//
        btnLogOut.setOnClickListener(this);
        btnSetting.setOnClickListener(this);
        btnStatus.setOnClickListener(this);
        btnHistory.setOnClickListener(this);

    }


    //  Lấy thông tin customer hiện thị lên
    private void getAssignedCustomerInfo() {
        linearCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference customerRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_CUSTOMERS)
                .child(customerID);
//        Event nếu data trên Firebase đc cập nhật thì cập nhật lên UI
        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//              nếu user tồn tại, và đã có thông tin user thì get thông tin đưa lên EditText
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get(NODE_NAME) != null) {
                        tvCustomerName.setText(map.get(NODE_NAME).toString());
                    }
                    if (map.get(NODE_PHONE) != null) {
                        tvCustomerPhone.setText(map.get(NODE_PHONE).toString());
                    }
                    if (map.get(NODE_PROFILE_IMAGE_URL) != null) {
                        Glide.with(getApplicationContext())
                                .load(map.get(NODE_PROFILE_IMAGE_URL).toString())
                                .error(R.drawable.ic_user_default)
                                .into(imageCustomerProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void getAssignedCustomer() {
//        Tìm đến ID Driver hiện tại
        DatabaseReference assignCustomerRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_DRIVERS)
                .child(driverID)
                .child(NODE_CUSTOMER_REQUEST)
                .child(NODE_CUSTOMER_RIDE_ID);
//        Truy cập vào node bên trong của Driver
//        nếu có khách bắt xe của mình -> lấy ID khách đó và tìm đến vị trí của khách
        assignCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
//                    đã có khách
//                    status = 1;
//                    lấy customer ID
                    customerID = dataSnapshot.getValue().toString();
//                    lấy thông tin customer trên node Customers
                    getAssignedCustomerInfo();
//                    lấy thông tin vị trí trên customerRequest
                    getAssignedCustomerDestination();
//                    tìm location của customer trên customerRequest
                    getAssignedCustomerPickupLocation();
                } else {
//                    không có khách thì ẩn Layout Info và set value to default
                    endRequest();
                }
            }

            //
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDestination() {
        DatabaseReference assignCustomerRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_DRIVERS)
                .child(driverID)
                .child(NODE_CUSTOMER_REQUEST);
//        Truy cập vào node bên trong của Driver -> customerRequest -> destination -> lấy tên địa điểm để đón
        assignCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
//                    đã có khách
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get(NODE_DESTINATION) != null){
                        destination = map.get(NODE_DESTINATION).toString();
                        tvCustomerDestination.setText(destination);
                    }else {
                        destination = "";
                        tvCustomerDestination.setText("");
                    }
                    if (map.get(NODE_DISTANCE) != null){
                        tvCustomerDistance.setText(map.get(NODE_DISTANCE).toString()+ "km");
                    }
                    if (map.get(NODE_PRICE) != null){
                        tvCustomerPrice.setText(map.get(NODE_PRICE).toString()+" VND");
                    }
                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if (map.get(NODE_DESTINATION_LAT) != null && map.get(NODE_DESTINATION_LNG) != null){
                        destinationLat = Double.parseDouble(map.get(NODE_DESTINATION_LAT).toString());
                        destinationLng = Double.parseDouble(map.get(NODE_DESTINATION_LNG).toString());
                        destinationLaLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerPickupLocation() {
//        tìm đến vị trí khách vừa đặt xe
        assignCustomerPickupLocationRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_DRIVERS)
                .child(driverID)
                .child(NODE_CUSTOMER_REQUEST);
        assignedCustomerPickupLocationListener = assignCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !TextUtils.isEmpty(customerID)) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(NODE_PICKUP_LAT) != null) {
                        locationLat = Double.parseDouble(map.get(NODE_PICKUP_LAT).toString());
                    }
                    if (map.get(NODE_PICKUP_LNG) != null) {
                        locationLng = Double.parseDouble(map.get(NODE_PICKUP_LNG).toString());
                    }
                    int height = 100;
                    int width = 100;
                    BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.location_pin  ,null);
                    Bitmap b = bitmapdraw.getBitmap();
                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
                    pickupLaLng = new LatLng(locationLat, locationLng);
                    pickUpMarker = mMap.addMarker(new MarkerOptions()
                            .position(pickupLaLng)
                            .title("Pickup Location")
                            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                    );
//                    getRouteToMaker(pickupLaLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
// Active tim` duong
    private void getRouteToMaker(LatLng pickUpLaLng) {
        Routing routing = new Routing.Builder()
                .key("AIzaSyBARj5JyjCV7hBN-9yC3dfzVbccEOSxdOM")
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), pickUpLaLng)
                .build();
        routing.execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomBy(12));
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //từ phiên bản Android Version 6.0 trở lên sẽ cần người dùng ban quyền
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        workingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    driverConnect();
                }
                else {
                    driverDisconnect();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (!isLogOut){
            driverDisconnect();
        }
    }

    private void driverConnect(){
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
        if (mMap != null)
            mMap.setMyLocationEnabled(true);
    }

    private void driverDisconnect() {
        if (mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userID = FirebaseAuth.getInstance().getUid();
        if (userID != null){
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("driverAvailable");
            GeoFire geoFire = new GeoFire(reference);
            geoFire.removeLocation(userID, geoFireListener);
        }
    }

    final int LOCATION_REQUEST_CODE = 1;

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

    //check permission
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle("Grant permission")
                    .setMessage("Please provide permission")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create()
                    .show();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please grant permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_log_out: {
                logOutDriverAccount();
                break;
            }
            case R.id.btn_setting: {
                goToSettingScreen();
                break;
            }
            case R.id.btn_status_request:{
                updateStatus();
                break;
            }
            case R.id.btn_history:{
                Intent intent = new Intent(DriverMapActivity.this, HistoryActivity.class);
                intent.putExtra(KEY_DRIVER_OR_CUSTOMER,NODE_DRIVERS);
                startActivity(intent);
                break;
            }
        }
    }

    private void updateStatus() {
        switch (status){
//        Nhận khách
            case 0: {
                status = 1;
//                removePolylines();
//                if (destinationLaLng != null){
//                    if (destinationLaLng.latitude != 0.0 && destinationLaLng.longitude != 0.0) {
//                        getRouteToMaker(destinationLaLng);
//                    }
//                }

                btnStatus.setText("Drive completed");
                break;
            }
//            kết thúc chuyến đi
            case 1:{
                status = 0;
                recordRide();
                endRequest();
                break;
            }
        }
    }

    private void recordRide() {
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_DRIVERS)
                .child(driverID)
                .child(NODE_HISTORY);
        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_CUSTOMERS)
                .child(customerID)
                .child(NODE_HISTORY);
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_HISTORY);
        String requestID = historyRef.push().getKey();
        if (requestID != null){
            driverRef.child(requestID).setValue(true);
            customerRef.child(requestID).setValue(true);

// lưu thông tin chuyến đi vào lịch sử
            HashMap map = new HashMap();
            map.put(NODE_DRIVER, driverID);
            map.put(NODE_CUSTOMER, customerID);
            map.put(NODE_RATING, 0);
            map.put(NODE_COMMENT, "");
            map.put(NODE_TIMESTAMP, System.currentTimeMillis());
            map.put(NODE_DESTINATION, DEFAULT_DESTINATION);
            if (!TextUtils.isEmpty(tvCustomerDistance.getText().toString()))
                map.put(NODE_DISTANCE, tvCustomerDistance.getText().toString());
            if (!TextUtils.isEmpty(tvCustomerPrice.getText().toString()))
                map.put(NODE_PRICE, tvCustomerPrice.getText().toString());
            map.put(PUT_NODE_FROM_LAT, pickupLaLng.latitude);
            map.put(PUT_NODE_FROM_LNG, pickupLaLng.longitude);
            map.put(PUT_NODE_TO_LAT, destinationLaLng.latitude);
            map.put(PUT_NODE_TO_LNG, destinationLaLng.longitude);
            historyRef.child(requestID).updateChildren(map);
        }
    }

    //Từ chối đón khách
    private void endRequest() {
        btnStatus.setText("No Customer");
//        removePolylines();
//      remove node customerRequest ben trong driver
        String driverID = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(NODE_USERS)
                .child(NODE_DRIVERS)
                .child(driverID)
                .child(NODE_CUSTOMER_REQUEST);
        driverRef.removeValue();
//      remove node customerRequest co ID = customerID
        DatabaseReference customerRequestRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("customerRequest");
        customerRequestRef.removeValue();
        customerID = "";
//        remove marker
        if (pickUpMarker != null){
            pickUpMarker.remove();
        }
//        remove listenr
        if (assignedCustomerPickupLocationListener != null) {
            assignCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationListener);
        }
//        set view to default
        linearCustomerInfo.setVisibility(View.GONE);
        tvCustomerDestination.setText("");
        tvCustomerPhone.setText("");
        tvCustomerName.setText("");
        imageCustomerProfile.setImageResource(R.drawable.ic_user_default);
    }

    private void logOutDriverAccount() {
        isLogOut = true;
        driverDisconnect();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToSettingScreen() {
        Intent intent = new Intent(DriverMapActivity.this, DriverSettingActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Something went wrong, Please try again: ", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
//    Tim` duong
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLaLng);
        builder.include(destinationLaLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cameraUpdate);
        mMap.addMarker(new MarkerOptions().position(pickupLaLng)
        .title("Pickup Location")
//        .icon(BitmapDescriptorFactory.fromBitmap())
                );
        mMap.addMarker(new MarkerOptions().position(destinationLaLng)
                        .title("Destination Location")
//        .icon(BitmapDescriptorFactory.fromBitmap())
        );

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void removePolylines(){
        for (Polyline lines: polylines) {
            lines.remove();
        }
        polylines.clear();
    }
}
